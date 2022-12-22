/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.mqtt.handler.publish;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.packetid.ShortPacketIdEncodingUtils;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.publish.PubrelWithFuture;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
public class OrderedTopicService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OrderedTopicService.class);
    private static final @NotNull ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

    static {
        //remove the stacktrace from the static exception
        CLOSED_CHANNEL_EXCEPTION.setStackTrace(new StackTraceElement[0]);
    }

    // We lazy-initialize these member variables to save memory for outgoing (broker to client) QoS 0 message flows.
    // They aren't required to be concurrent, because OrderedTopicService is part of the PublishFlowHandler, which
    // implements a non-shared ChannelInboundHandler for which netty guarantees us that no concurrent invocations of
    // handler methods happen.
    @VisibleForTesting
    @Nullable Queue<QueuedMessage> queue;
    private @Nullable AtomicBoolean closedAlready;
    private @Nullable ShortHashSet unacknowledgedMessages;
    private @Nullable ShortObjectHashMap<SettableFuture<PublishStatus>> messageIdToFutureMap;

    private void lazyInitialize() {
        if (queue == null) {
            assert closedAlready == null;
            assert unacknowledgedMessages == null;
            assert messageIdToFutureMap == null;
            queue = new ArrayDeque<>();
            closedAlready = new AtomicBoolean(false);
            unacknowledgedMessages = new ShortHashSet();
            messageIdToFutureMap = new ShortObjectHashMap<>();
        }
    }

    public void messageFlowComplete(final @NotNull ChannelHandlerContext ctx, final int packetId) {
        lazyInitialize();
        Preconditions.checkNotNull(queue);
        Preconditions.checkNotNull(unacknowledgedMessages);
        Preconditions.checkNotNull(messageIdToFutureMap);

        final SettableFuture<PublishStatus> publishStatusFuture = messageIdToFutureMap.remove(
                ShortPacketIdEncodingUtils.encode(packetId));

        if (publishStatusFuture != null) {
            publishStatusFuture.set(PublishStatus.DELIVERED);
        }

        final boolean removed = unacknowledgedMessages.remove(ShortPacketIdEncodingUtils.encode(packetId));
        if (!removed) {
            return;
        }

        if (queue.isEmpty()) {
            return;
        }

        final int maxInflightWindow = ChannelUtils.maxInflightWindow(ctx.channel());

        do {
            final QueuedMessage poll = queue.poll();
            if (poll == null) {
                return;
            }
            unacknowledgedMessages.add(ShortPacketIdEncodingUtils.encode(poll.publish.getPacketIdentifier()));
            ctx.writeAndFlush(poll.getPublish(), poll.getPromise());
        } while (unacknowledgedMessages.size() < maxInflightWindow);
    }

    public boolean handlePublish(
            final @NotNull Channel channel,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise) {
        lazyInitialize();
        Preconditions.checkNotNull(closedAlready);
        Preconditions.checkNotNull(unacknowledgedMessages);
        Preconditions.checkNotNull(messageIdToFutureMap);

        if (msg instanceof PubrelWithFuture) {
            final PubrelWithFuture pubrelWithFuture = (PubrelWithFuture) msg;
            messageIdToFutureMap.put(
                    ShortPacketIdEncodingUtils.encode(pubrelWithFuture.getPacketIdentifier()),
                    pubrelWithFuture.getFuture());
            return false;
        }

        if (!(msg instanceof PUBLISH)) {
            return false;
        }

        SettableFuture<PublishStatus> future = null;
        if (msg instanceof PublishWithFuture) {
            final PublishWithFuture publishWithFuture = (PublishWithFuture) msg;
            future = publishWithFuture.getFuture();
        }

        final PUBLISH publish = (PUBLISH) msg;
        final String clientId = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getClientId();
        final int qosNumber = publish.getQoS().getQosNumber();
        if (log.isTraceEnabled()) {
            log.trace("Client {}: Sending PUBLISH QoS {} Message with packet id {}", clientId, publish.getQoS().getQosNumber(), publish.getPacketIdentifier());
        }

        if (qosNumber < 1) {
            if (future != null) {
                future.set(PublishStatus.DELIVERED);
            }
            return false;
        }

        if (future != null) {
            messageIdToFutureMap.put(ShortPacketIdEncodingUtils.encode(publish.getPacketIdentifier()), future);
        }

        //do not store in OrderedTopicService if channelInactive has been called already
        if (closedAlready.get()) {
            promise.setFailure(CLOSED_CHANNEL_EXCEPTION);
            return true;
        }


        final int maxInflightWindow = ChannelUtils.maxInflightWindow(channel);
        if (unacknowledgedMessages.size() >= maxInflightWindow) {
            queueMessage(promise, publish, clientId);
            return true;
        } else {
            unacknowledgedMessages.add(ShortPacketIdEncodingUtils.encode(publish.getPacketIdentifier()));
            return false;
        }
    }

    public void handleInactive() {
        if (queue != null) {
            Preconditions.checkNotNull(closedAlready);
            Preconditions.checkNotNull(messageIdToFutureMap);

            closedAlready.set(true);

            for (final QueuedMessage queuedMessage : queue) {
                if (queuedMessage != null) {
                    if (!queuedMessage.getPromise().isDone()) {
                        queuedMessage.getPromise().setFailure(CLOSED_CHANNEL_EXCEPTION);
                    }
                }
            }

            // In case the client is disconnected, we return all the publish status futures
            // This is particularly important for shared subscriptions, because the publish will not be resent otherwise
            messageIdToFutureMap.forEachValue(publishStatusFuture -> publishStatusFuture.set(PublishStatus.NOT_CONNECTED));
        }
    }

    private void queueMessage(final @NotNull ChannelPromise promise, final @NotNull PUBLISH publish, final @NotNull String clientId) {
        Preconditions.checkNotNull(queue);

        if (log.isTraceEnabled()) {
            final String topic = publish.getTopic();
            final int messageId = publish.getPacketIdentifier();
            log.trace("Buffered publish message with qos {} packetIdentifier {} and topic {} for client {}, because the receive maximum is exceeded",
                    publish.getQoS().name(), messageId, topic, clientId);
        }

        queue.add(new QueuedMessage(publish, promise));
    }

    @VisibleForTesting
    public @NotNull ShortHashSet unacknowledgedMessages() {
        Preconditions.checkNotNull(unacknowledgedMessages);
        return unacknowledgedMessages;
    }

    @Immutable
    @VisibleForTesting
    static class QueuedMessage {

        @NotNull
        private final PUBLISH publish;
        @NotNull
        private final ChannelPromise promise;

        QueuedMessage(final @NotNull PUBLISH publish, final @NotNull ChannelPromise promise) {

            this.publish = publish;
            this.promise = promise;
        }

        @NotNull
        public PUBLISH getPublish() {
            return publish;
        }

        @NotNull
        public ChannelPromise getPromise() {
            return promise;
        }
    }
}
