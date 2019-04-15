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

package com.hivemq.mqtt.handler.subscribe.retained;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.ChannelAttributes;
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
    private final @NotNull String lastTopic;
    private final @NotNull Queue<String> topics;
    private final @NotNull Channel channel;
    private final @NotNull RetainedMessagesSender retainedMessagesSender;

    SendRetainedMessageListenerAndScheduleNext(
            final @NotNull Topic subscription,
            final @NotNull String lastTopic,
            final @NotNull Queue<String> topics,
            final @NotNull Channel channel,
            final @NotNull RetainedMessagesSender retainedMessagesSender) {

        checkNotNull(subscription, "Subscription must not be null");
        checkNotNull(lastTopic, "Last Topic must not be null");
        checkNotNull(topics, "Topics must not be null");
        checkNotNull(channel, "Channel must not be null");
        checkNotNull(retainedMessagesSender, "RetainedMessagesSender must not be null");

        this.subscription = subscription;
        this.lastTopic = lastTopic;
        this.topics = topics;
        this.channel = channel;
        this.retainedMessagesSender = retainedMessagesSender;
    }

    @Override
    public void onSuccess(final Void result) {
        if (channel.isActive()) {
            final String nextTopic = topics.poll();
            if (nextTopic != null) {
                send(nextTopic);
            }
        }
    }

    private void send(final @NotNull String topic) {
        final ListenableFuture<Void> sentFuture = retainedMessagesSender.writeRetainedMessage(
                new Topic(topic, subscription.getQoS(), subscription.isNoLocal(),
                        subscription.isRetainAsPublished(), subscription.getRetainHandling(),
                        subscription.getSubscriptionIdentifier()),
                channel);

        Futures.addCallback(sentFuture, new SendRetainedMessageListenerAndScheduleNext(
                subscription, topic, topics, channel, retainedMessagesSender), channel.eventLoop());
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
                                channel.attr(ChannelAttributes.CLIENT_ID).get(), subscription.getTopic());
                    }
                    send(lastTopic);
                }, 1, TimeUnit.SECONDS);
            }

        } else {
            Exceptions.rethrowError("Unable to send retained message on topic " + lastTopic +
                    " to client " + channel.attr(ChannelAttributes.CLIENT_ID).get() + ".", throwable);
            channel.disconnect();
        }
    }
}