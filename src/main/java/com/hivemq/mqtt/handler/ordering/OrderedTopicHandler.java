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
import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.PublishDroppedEvent;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.publish.PubrelWithFuture;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.util.ChannelAttributes;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hivemq.configuration.service.InternalConfigurations.INFLIGHT_QUEUE_BUCKET_COUNT;

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

    private final Map<Integer, Integer> messageIdToBucketMap = new ConcurrentHashMap<>();
    private final Map<Integer, SettableFuture<PublishStatus>> messageIdToFutureMap = new ConcurrentHashMap<>();

    private Queue<QueuedMessage>[] queues;

    private boolean[] active;

    private final AtomicBoolean closedAlready = new AtomicBoolean(false);

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {

        final Integer clientReceiveMaximum = ctx.channel().attr(ChannelAttributes.CLIENT_RECEIVE_MAXIMUM).get();

        if (clientReceiveMaximum != null && clientReceiveMaximum < INFLIGHT_QUEUE_BUCKET_COUNT) {
            //noinspection unchecked
            queues = new ArrayDeque[clientReceiveMaximum];
            active = new boolean[clientReceiveMaximum];
        } else {
            //noinspection unchecked
            queues = new ArrayDeque[INFLIGHT_QUEUE_BUCKET_COUNT];
            active = new boolean[INFLIGHT_QUEUE_BUCKET_COUNT];
        }

        super.handlerAdded(ctx);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, @NotNull final Object msg) throws Exception {

        if (msg instanceof PUBACK || msg instanceof PUBCOMP) {

            final int packetId = ((MessageWithID) msg).getPacketIdentifier();
            messageFlowComplete(ctx, packetId);
        }

        super.channelRead(ctx, msg);
    }

    private void messageFlowComplete(final ChannelHandlerContext ctx, final int packetId){
        final SettableFuture<PublishStatus> publishStatusFuture = messageIdToFutureMap.get(packetId);

        final Integer bucket = messageIdToBucketMap.remove(packetId);
        if (publishStatusFuture != null) {
            messageIdToFutureMap.remove(packetId);
            publishStatusFuture.set(PublishStatus.DELIVERED);
        }

        if (bucket == null) {
            return;
        }

        //The message for that ID was not queued
        if (queues[bucket] == null || queues[bucket].isEmpty()) {

            active[bucket] = false;
            return;
        }

        final Queue<QueuedMessage> queue = queues[bucket];

        final QueuedMessage poll = queue.poll();
        ctx.writeAndFlush(poll.getPublish(), poll.getPromise());
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (!(evt instanceof PublishDroppedEvent)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        final PublishDroppedEvent publishDroppedEvent = (PublishDroppedEvent) evt;
        // Already logged, just proceeded with with the next message
        messageFlowComplete(ctx, publishDroppedEvent.getMessage().getPacketIdentifier());
    }

    @Override
    public void write(final ChannelHandlerContext ctx, @NotNull final Object msg, @NotNull final ChannelPromise promise) throws Exception {

        if (msg instanceof PubrelWithFuture) {
            final PubrelWithFuture pubrelWithFuture = (PubrelWithFuture) msg;
            messageIdToFutureMap.put(pubrelWithFuture.getPacketIdentifier(), pubrelWithFuture.getFuture());
            super.write(ctx, pubrelWithFuture.getPubrel(), promise);
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

        final String topic = publish.getTopic();
        final int messageId = publish.getPacketIdentifier();

        final int bucket = BucketUtils.getBucket(topic, INFLIGHT_QUEUE_BUCKET_COUNT);

        //QoS messages which get resent must pass the queueing logic
        if (publish.isDuplicateDelivery() && messageIdToBucketMap.containsKey(messageId)) {
            super.write(ctx, msg, promise);
            return;
        }

        if (!active[bucket]) {
            handleInactiveBucket(ctx, publish, promise, messageId, bucket);
        } else {
            handleActiveBucket(promise, publish, clientId, bucket);
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {

        closedAlready.set(true);

        for (final Queue<QueuedMessage> queue : queues) {
            if (queue != null) {
                for (final QueuedMessage queuedMessage : queue) {
                    if (queuedMessage != null) {
                        if (!queuedMessage.getPromise().isDone()) {
                            queuedMessage.getPromise().setFailure(CLOSED_CHANNEL_EXCEPTION);
                        }
                    }
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

    @NotNull
    private Queue<QueuedMessage> getOrCreateQueue(final int bucket) {
        Queue<QueuedMessage> queue = queues[bucket];
        if (queue == null) {
            queues[bucket] = new ArrayDeque<>();
            queue = queues[bucket];
        }
        return queue;
    }

    private void handleActiveBucket(@NotNull final ChannelPromise promise, final PUBLISH publish, @NotNull final String clientId, final int bucket) {

        final String topic = publish.getTopic();
        final int messageId = publish.getPacketIdentifier();

        if (log.isTraceEnabled()) {
            log.trace("Enqueued publish message with qos {} packetIdentifier {} and topic {} for client {}",
                    publish.getQoS().name(), messageId, topic, clientId);

        }

        final Queue<QueuedMessage> queue = getOrCreateQueue(bucket);
        queue.add(new QueuedMessage(publish, promise));
        messageIdToBucketMap.put(messageId, bucket);
    }

    private void handleInactiveBucket(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBLISH publish, @NotNull final ChannelPromise promise, final int messageId, final int bucket) throws Exception {
        active[bucket] = true;
        messageIdToBucketMap.put(messageId, bucket);
        super.write(ctx, publish, promise);
    }

    private void handleQosZero(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBLISH publish, @NotNull final ChannelPromise promise, @Nullable final SettableFuture<PublishStatus> future) throws Exception {
        if (future != null) {
            future.set(PublishStatus.DELIVERED);
        }
        super.write(ctx, publish, promise);
    }

    @VisibleForTesting
    @NotNull
    Queue<QueuedMessage>[] getQueues() {
        return queues;
    }

    public int inFlightSize() {
        return messageIdToBucketMap.size();
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
