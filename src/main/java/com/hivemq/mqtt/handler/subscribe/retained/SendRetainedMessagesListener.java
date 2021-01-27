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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.subscribe.IncomingSubscribeService;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling.DO_NOT_SEND;
import static com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST;

/**
 * A {@link io.netty.channel.ChannelFutureListener} which sends out all retained messages for the client. This listener
 * is probably most useful for the {@link IncomingSubscribeService}
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
public class SendRetainedMessagesListener implements ChannelFutureListener {

    private final @NotNull RetainedMessagePersistence retainedMessagePersistence;
    private final @NotNull RetainedMessagesSender retainedMessagesSender;
    private final @NotNull List<SubscriptionResult> subscriptions;
    private final @NotNull Set<Topic> ignoredTopics;

    public SendRetainedMessagesListener(
            @NotNull final List<SubscriptionResult> subscriptions,
            @NotNull final Set<Topic> ignoredTopics,
            @NotNull final RetainedMessagePersistence retainedMessagePersistence,
            @NotNull final RetainedMessagesSender retainedMessagesSender) {

        checkNotNull(subscriptions, "Subscriptions must not be null");
        checkNotNull(ignoredTopics, "ignoredTopics must not be null");

        this.subscriptions = subscriptions;
        this.ignoredTopics = ignoredTopics;
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.retainedMessagesSender = retainedMessagesSender;
    }

    @Override
    public void operationComplete(@Nullable final ChannelFuture future) throws Exception {
        if (future != null && future.isSuccess()) {

            final Channel channel = future.channel();
            if (channel == null || !channel.isActive()) {
                return;
            }

            final List<Topic> topicsWithWildcards = sendExactMatches(channel);

            //When there's no exact match, we try to match the wildcard filters
            if (!topicsWithWildcards.isEmpty()) {
                sendMatchingWildcardSubscriptions(topicsWithWildcards, channel);
            }
        }
    }

    /**
     * Sends out retained messages where the topic exactly matches. This is only possible if there is no wildcard in a
     * topic
     *
     * @param channel the {@link io.netty.channel.Channel} to send the messages to.
     * @return a List of all topics which contained wildcards and thus were not processed by this method
     */
    @NotNull
    private List<Topic> sendExactMatches(@NotNull final Channel channel) {
        final List<Topic> topicsWithWildcards = new ArrayList<>(subscriptions.size());
        for (final SubscriptionResult subscription : subscriptions) {

            if (subscription == null) {
                continue;
            }

            final Topic subscriptionTopic = subscription.getTopic();

            if (subscriptionTopic.getRetainHandling() == DO_NOT_SEND) {
                continue;
            }

            if (subscriptionTopic.getRetainHandling() == SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST &&
                    subscription.subscriptionAlreadyExisted()) {
                //do not send retained messages if the subscription already existed before
                continue;
            }

            if (subscription.getShareName() != null) {
                //do not send retained messages for shared subscriptions
                continue;
            }

            if (!ignoredTopics.contains(subscriptionTopic)) {
                final String topic = subscriptionTopic.getTopic();
                if (topic.contains("#") || topic.contains("+")) {
                    topicsWithWildcards.add(subscriptionTopic);
                } else {

                    final ListenableFuture<Void> writeFuture =
                            retainedMessagesSender.writeRetainedMessages(channel, subscriptionTopic);
                    Futures.addCallback(
                            writeFuture,
                            new SendRetainedMessageResultListener(channel, subscriptionTopic, retainedMessagesSender),
                            channel.eventLoop());

                }
            }
        }
        return topicsWithWildcards;
    }

    /**
     * Sends out matching wildcard subscriptions for the given topics to the given Channel
     * <p>
     * This method is rather expensive but needed for wildcard matching. If you can, try to reduce the input as much as
     * possible, especially if there aren't any wildcards so we don't have to process the rather expensive wildcard
     * matching
     *
     * @param topicsWithWildcards a List of String which represents the topics with wildcards
     * @param channel             the {@link io.netty.channel.Channel} to write the matching retained messages to
     */
    private void sendMatchingWildcardSubscriptions(
            @NotNull final List<Topic> topicsWithWildcards, @NotNull final Channel channel) {

        for (final Topic subscribedTopic : topicsWithWildcards) {

            final ListenableFuture<Set<String>> future =
                    retainedMessagePersistence.getWithWildcards(subscribedTopic.getTopic());
            Futures.addCallback(
                    future,
                    new RetainedMessagesHandleWildcardsCallback(subscribedTopic, channel, retainedMessagesSender),
                    channel.eventLoop());
        }
    }

    static class RetainedMessagesHandleWildcardsCallback implements FutureCallback<Set<String>> {

        static final int CONCURRENT_MESSAGES = 25;
        private final @NotNull Topic subscription;
        private final @NotNull Channel channel;

        private final @NotNull RetainedMessagesSender retainedMessagesSender;

        RetainedMessagesHandleWildcardsCallback(
                final @NotNull Topic subscription,
                final @NotNull Channel channel,
                final @NotNull RetainedMessagesSender retainedMessagesSender) {
            this.subscription = subscription;
            this.channel = channel;
            this.retainedMessagesSender = retainedMessagesSender;
        }

        @Override
        public void onSuccess(final @Nullable Set<String> retainedMessageTopics) {
            if (retainedMessageTopics == null || retainedMessageTopics.size() == 0) {
                //Do nothing, we don't have retained messages
                return;
            }

            //Attention, this set is immutable, so we need a fresh mutable collection
            final Queue<String> topics = new ConcurrentLinkedQueue<>(retainedMessageTopics);

            final Integer clientReceiveMaximum = channel.attr(ChannelAttributes.CLIENT_RECEIVE_MAXIMUM).get();

            int concurrentMessages = clientReceiveMaximum == null ? CONCURRENT_MESSAGES :
                    Math.min(clientReceiveMaximum, CONCURRENT_MESSAGES);
            concurrentMessages = Math.min(concurrentMessages, retainedMessageTopics.size());

            final Topic[] topicBatch = new Topic[concurrentMessages];
            for (int i = 0; i < concurrentMessages; i++) {
                final String nextTopic = topics.poll();
                topicBatch[i] = new Topic(nextTopic, subscription.getQoS(), subscription.isNoLocal(),
                        subscription.isRetainAsPublished(), subscription.getRetainHandling(),
                        subscription.getSubscriptionIdentifier());
            }


            final ListenableFuture<Void> sentFuture = retainedMessagesSender.writeRetainedMessages(channel, topicBatch);

            Futures.addCallback(
                    sentFuture, new SendRetainedMessageListenerAndScheduleNext(subscription, topics, channel,
                            retainedMessagesSender, concurrentMessages), channel.eventLoop());
        }

        @Override
        public void onFailure(final @NotNull Throwable throwable) {
            Exceptions.rethrowError("Unable to send retained messages on topic " + subscription.getTopic() +
                    " to client " + ChannelUtils.getClientId(channel) + ".", throwable);
            channel.disconnect();
        }
    }
}
