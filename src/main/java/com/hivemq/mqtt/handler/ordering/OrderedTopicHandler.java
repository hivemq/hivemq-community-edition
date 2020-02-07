/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.mqtt.handler.ordering;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.event.PubrelDroppedEvent;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.publish.PubrelWithFuture;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
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
 */
public class OrderedTopicHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderedTopicHandler.class);
    private static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

    static {
        //remove the stacktrace from the static exception
        CLOSED_CHANNEL_EXCEPTION.setStackTrace(new StackTraceElement[0]);
    }


    private final Map<Integer, SettableFuture<PublishStatus>> messageIdToFutureMap = new ConcurrentHashMap<>();

    @VisibleForTesting
    final Queue<QueuedMessage> queue = new ArrayDeque<>();

    private final AtomicBoolean closedAlready = new AtomicBoolean(false);
    private final Set<Integer> unacknowledgedMessages = ConcurrentHashMap.newKeySet();

    @Override
    public void channelRead(final ChannelHandlerContext ctx, @NotNull final Object msg) throws Exception {

        if (msg instanceof PUBACK || msg instanceof PUBCOMP) {
            messageFlowComplete(ctx, ((MessageWithID) msg).getPacketIdentifier());
        }
        if (msg instanceof PUBREC) {
            final PUBREC pubrec = (PUBREC) msg;
            if (pubrec.getReasonCode() != Mqtt5PubRecReasonCode.SUCCESS &&
                    pubrec.getReasonCode() != Mqtt5PubRecReasonCode.NO_MATCHING_SUBSCRIBERS) {
                messageFlowComplete(ctx, ((MessageWithID) msg).getPacketIdentifier());
            }
        }

        super.channelRead(ctx, msg);
    }

    private void messageFlowComplete(@NotNull final ChannelHandlerContext ctx, final int packetId){
        final SettableFuture<PublishStatus> publishStatusFuture = messageIdToFutureMap.get(packetId);

        if (publishStatusFuture != null) {
            messageIdToFutureMap.remove(packetId);
            publishStatusFuture.set(PublishStatus.DELIVERED);
        }

        final boolean removed = unacknowledgedMessages.remove(packetId);
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
            unacknowledgedMessages.add(poll.publish.getPacketIdentifier());
            ctx.writeAndFlush(poll.getPublish(), poll.getPromise());
        } while (unacknowledgedMessages.size() < maxInflightWindow);
    }

    @Override
    public void userEventTriggered(@NotNull final ChannelHandlerContext ctx, @NotNull final Object evt) throws Exception {
        if (evt instanceof PublishDroppedEvent) {
            final PublishDroppedEvent publishDroppedEvent = (PublishDroppedEvent) evt;
            // Already logged, just proceeded with with the next message
            messageFlowComplete(ctx, publishDroppedEvent.getMessage().getPacketIdentifier());
            return;
        } else if (evt instanceof PubrelDroppedEvent) {
            final PubrelDroppedEvent pubrelDroppedEvent = (PubrelDroppedEvent) evt;
            messageFlowComplete(ctx, pubrelDroppedEvent.getMessage().getPacketIdentifier());
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, @NotNull final Object msg, @NotNull final ChannelPromise promise) throws Exception {

        if (msg instanceof PubrelWithFuture) {
            final PubrelWithFuture pubrelWithFuture = (PubrelWithFuture) msg;
            messageIdToFutureMap.put(pubrelWithFuture.getPacketIdentifier(), pubrelWithFuture.getFuture());
            super.write(ctx, pubrelWithFuture, promise);
            return;
        }

        if (!(msg instanceof PUBLISH)) {
            super.write(ctx, msg, promise);
            return;
        }

        final Channel channel = ctx.channel();
        SettableFuture<PublishStatus> future = null;
        if (msg instanceof PublishWithFuture) {
            final PublishWithFuture publishWithFuture = (PublishWithFuture) msg;
            future = publishWithFuture.getFuture();
        }

        final PUBLISH publish = (PUBLISH) msg;
        final String clientId = channel.attr(ChannelAttributes.CLIENT_ID).get();
        final int qosNumber = publish.getQoS().getQosNumber();
        if (log.isTraceEnabled()) {
            log.trace("Client {}: Sending PUBLISH QoS {} Message with packet id {}", clientId, publish.getQoS().getQosNumber(), publish.getPacketIdentifier());
        }

        if (qosNumber < 1) {
            handleQosZero(ctx, publish, promise, future);
            return;
        }

        if (future != null) {
            messageIdToFutureMap.put(publish.getPacketIdentifier(), future);
        }

        //do not store in OrderedTopicHandler if channelInactive has been called already
        if (closedAlready.get()) {
            promise.setFailure(CLOSED_CHANNEL_EXCEPTION);
            return;
        }


        final int maxInflightWindow = ChannelUtils.maxInflightWindow(ctx.channel());
        if (unacknowledgedMessages.size() >= maxInflightWindow) {
            queueMessage(promise, publish, clientId);
        } else {
            unacknowledgedMessages.add(publish.getPacketIdentifier());
            super.write(ctx, publish, promise);
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {

        closedAlready.set(true);

        for (final QueuedMessage queuedMessage : queue) {
            if (queuedMessage != null) {
                if (!queuedMessage.getPromise().isDone()) {
                    queuedMessage.getPromise().setFailure(CLOSED_CHANNEL_EXCEPTION);
                }
            }
        }

        // In case the client is disconnected, we return all the publish status futures
        // This is particularly important for shared subscriptions, because the publish wont be resent otherwise
        for (final Map.Entry<Integer, SettableFuture<PublishStatus>> entry : messageIdToFutureMap.entrySet()) {
            final SettableFuture<PublishStatus> publishStatusFuture = entry.getValue();
            publishStatusFuture.set(PublishStatus.NOT_CONNECTED);
        }

        super.channelInactive(ctx);
    }

    private void queueMessage(@NotNull final ChannelPromise promise, @NotNull final PUBLISH publish, @NotNull final String clientId) {

        if (log.isTraceEnabled()) {
            final String topic = publish.getTopic();
            final int messageId = publish.getPacketIdentifier();
            log.trace("Buffered publish message with qos {} packetIdentifier {} and topic {} for client {}, because the receive maximum is exceeded",
                    publish.getQoS().name(), messageId, topic, clientId);
        }

        queue.add(new QueuedMessage(publish, promise));
    }

    private void handleQosZero(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBLISH publish, @NotNull final ChannelPromise promise, @Nullable final SettableFuture<PublishStatus> future) throws Exception {
        if (future != null) {
            future.set(PublishStatus.DELIVERED);
        }
        super.write(ctx, publish, promise);
    }

    @NotNull
    public Set<Integer> unacknowledgedMessages() {
        return unacknowledgedMessages;
    }

    @Immutable
    @VisibleForTesting
    static class QueuedMessage {

        @NotNull
        private final PUBLISH publish;
        @NotNull
        private final ChannelPromise promise;

        QueuedMessage(@NotNull final PUBLISH publish, @NotNull final ChannelPromise promise) {

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
