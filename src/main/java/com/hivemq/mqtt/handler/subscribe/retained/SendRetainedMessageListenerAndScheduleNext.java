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
package com.hivemq.mqtt.handler.subscribe.retained;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This FutureCallback sends a retained message and schedules the sending of the next retained message.
 *
 * @author Dominik Obermaier
 */
public class SendRetainedMessageListenerAndScheduleNext implements FutureCallback<Void> {

    private static final Logger log = LoggerFactory.getLogger(SendRetainedMessageListenerAndScheduleNext.class);

    private final @NotNull Topic subscription;
    private final @NotNull Queue<String> topics;
    private final @NotNull Channel channel;
    private final @NotNull RetainedMessagesSender retainedMessagesSender;
    private final int batchSizeMax;

    SendRetainedMessageListenerAndScheduleNext(
            final @NotNull Topic subscription,
            final @NotNull Queue<String> topics,
            final @NotNull Channel channel,
            final @NotNull RetainedMessagesSender retainedMessagesSender,
            final int batchSizeMax) {

        checkNotNull(subscription, "Subscription must not be null");
        checkNotNull(topics, "Topics must not be null");
        checkNotNull(channel, "Channel must not be null");
        checkNotNull(retainedMessagesSender, "RetainedMessagesSender must not be null");

        this.subscription = subscription;
        this.topics = topics;
        this.channel = channel;
        this.retainedMessagesSender = retainedMessagesSender;
        this.batchSizeMax = batchSizeMax;
    }

    @Override
    public void onSuccess(final Void result) {
        if (!channel.isActive()) {
            return;
        }
        send();
    }

    private void send() {
        final int remainingTopics = topics.size();
        if (remainingTopics == 0) {
            return;
        }
        final int batchSize = Math.min(remainingTopics, batchSizeMax);
        final Topic[] topicBatch = new Topic[batchSize];
        for (int i = 0; i < batchSize; i++) {
            final String nextTopic = topics.poll();
            topicBatch[i] = new Topic(nextTopic, subscription.getQoS(), subscription.isNoLocal(),
                    subscription.isRetainAsPublished(), subscription.getRetainHandling(),
                    subscription.getSubscriptionIdentifier());
        }

        final ListenableFuture<Void> sentFuture = retainedMessagesSender.writeRetainedMessages(channel, topicBatch);

        Futures.addCallback(sentFuture, new SendRetainedMessageListenerAndScheduleNext(subscription, topics, channel, retainedMessagesSender, batchSizeMax), channel.eventLoop());
    }

    @Override
    public void onFailure(final @NotNull Throwable throwable) {

        if (Exceptions.isConnectionClosedException(throwable)) {
            return;
        }

        if (throwable instanceof NoMessageIdAvailableException) {
            if (channel.isActive()) {
                //We should just try again
                channel.eventLoop().schedule(() -> {
                    if (log.isTraceEnabled()) {
                        log.trace("Retrying retained message for client '{}' on topic '{}'.",
                                channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId(), subscription.getTopic());
                    }
                    send();
                }, 1, TimeUnit.SECONDS);
            }

        } else {
            Exceptions.rethrowError("Unable to send retained message for subscription " + subscription.getTopic() +
                    " to client " + channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId() + ".", throwable);
            channel.disconnect();
        }
    }
}