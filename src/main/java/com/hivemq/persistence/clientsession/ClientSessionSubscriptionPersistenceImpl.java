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
package com.hivemq.persistence.clientsession;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.iteration.ChunkCursor;
import com.hivemq.extensions.iteration.Chunker;
import com.hivemq.extensions.iteration.MultipleChunkResult;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.topic.SubscriptionFlags;
import com.hivemq.mqtt.topic.TopicFilter;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.AbstractPersistence;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.ProducerQueues;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
@LazySingleton
public class ClientSessionSubscriptionPersistenceImpl extends AbstractPersistence implements ClientSessionSubscriptionPersistence {

    private final Logger log = LoggerFactory.getLogger(ClientSessionSubscriptionPersistenceImpl.class);

    private final @NotNull ClientSessionSubscriptionLocalPersistence localPersistence;
    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull SharedSubscriptionService sharedSubscriptionService;
    private final @NotNull ChannelPersistence channelPersistence;
    private final @NotNull ProducerQueues singleWriter;
    private final @NotNull EventLog eventLog;
    private final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence;
    private final @NotNull PublishPollService publishPollService;
    private final @NotNull Chunker chunker;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    @Inject
    ClientSessionSubscriptionPersistenceImpl(final @NotNull ClientSessionSubscriptionLocalPersistence localPersistence,
                                             final @NotNull LocalTopicTree topicTree,
                                             final @NotNull SharedSubscriptionService sharedSubscriptionService,
                                             final @NotNull SingleWriterService singleWriterService,
                                             final @NotNull ChannelPersistence channelPersistence,
                                             final @NotNull EventLog eventLog,
                                             final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence,
                                             final @NotNull PublishPollService publishPollService,
                                             final @NotNull Chunker chunker,
                                             final @NotNull MqttServerDisconnector mqttServerDisconnector) {

        this.localPersistence = localPersistence;
        this.topicTree = topicTree;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.channelPersistence = channelPersistence;
        this.singleWriter = singleWriterService.getSubscriptionQueue();
        this.eventLog = eventLog;
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        this.publishPollService = publishPollService;
        this.chunker = chunker;
        this.mqttServerDisconnector = mqttServerDisconnector;
    }

    @NotNull
    @Override
    public ImmutableSet<Topic> getSubscriptions(@NotNull final String client) {
        checkNotNull(client, "Client id must not be null");
        return localPersistence.getSubscriptions(client);
    }

    @NotNull
    @Override
    public ListenableFuture<SubscriptionResult> addSubscription(@NotNull final String client, @NotNull final Topic topic) {
        try {
            checkNotNull(client, "Client id must not be null");
            checkNotNull(topic, "Topic must not be null");

            final long timestamp = System.currentTimeMillis();

            final ClientSession session = clientSessionLocalPersistence.getSession(client);

            //It must not be possible to add subscriptions for an expired or not existing session
            if (session == null) {
                return Futures.immediateFuture(null);
            }

            final boolean subscriberExisted;
            //parse topic for shared flag
            final SharedSubscription sharedSubscription = sharedSubscriptionService.checkForSharedSubscription(topic.getTopic());
            final ListenableFuture<Void> persistFuture;
            if (sharedSubscription == null) {
                //not a shared subscription
                subscriberExisted = topicTree.addTopic(client, topic, SubscriptionFlags.getDefaultFlags(false, topic.isRetainAsPublished(), topic.isNoLocal()), null);
                persistFuture = singleWriter.submit(client, (bucketIndex, queueBuckets, queueIndex) -> {
                    localPersistence.addSubscription(client, topic, timestamp, bucketIndex);
                    return null;
                });
            } else {
                if (sharedSubscription.getTopicFilter().isEmpty()) {
                    disconnectSharedSubscriberWithEmptyTopic(client);
                    return Futures.immediateFuture(null);
                }
                // QoS 2 is not supported for shared subscriptions
                if (topic.getQoS() == QoS.EXACTLY_ONCE) {
                    topic.setQoS(QoS.AT_LEAST_ONCE);
                }

                final Topic sharedTopic = new Topic(sharedSubscription.getTopicFilter(), topic.getQoS(), topic.isNoLocal(),
                        topic.isRetainAsPublished(), topic.getRetainHandling(), topic.getSubscriptionIdentifier());

                subscriberExisted = topicTree.addTopic(client, sharedTopic, SubscriptionFlags.getDefaultFlags(true,
                        topic.isRetainAsPublished(), topic.isNoLocal()), sharedSubscription.getShareName());

                final Subscription subscription = new Subscription(sharedTopic, SubscriptionFlags.getDefaultFlags(true, topic.isRetainAsPublished(), topic.isNoLocal()), sharedSubscription.getShareName());

                persistFuture = singleWriter.submit(client, (bucketIndex, queueBuckets, queueIndex) -> {
                    localPersistence.addSubscription(client, topic, timestamp, bucketIndex);
                    invalidateSharedSubscriptionCacheAndPoll(client, ImmutableSet.of(subscription));
                    return null;
                });
            }

            //set future result when local persistence future and topic tree future return;
            return Futures.whenAllComplete(persistFuture)
                    .call(() -> new SubscriptionResult(topic, subscriberExisted, sharedSubscription == null ? null : sharedSubscription.getShareName()),
                    MoreExecutors.directExecutor());

        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }
    }

    @NotNull
    @Override
    public ListenableFuture<ImmutableList<SubscriptionResult>> addSubscriptions(@NotNull final String client, @NotNull final ImmutableSet<Topic> topics) {
        try {
            checkNotNull(client, "Client id must not be null");
            checkNotNull(topics, "Topics must not be null");

            return addBatchedTopics(client, topics);
        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }

    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeSubscriptions(@NotNull final String client, @NotNull final ImmutableSet<String> topics) {
        try {
            checkNotNull(client, "Client id must not be null");
            checkNotNull(topics, "Topics must not be null");

            return removeBatchedTopics(client, topics);
        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }

    }

    @NotNull
    @Override
    public ListenableFuture<Void> remove(@NotNull final String client, @NotNull final String topic) {
        try {
            checkNotNull(client, "Client id must not be null");
            checkNotNull(topic, "Topic must not be null");

            final long timestamp = System.currentTimeMillis();

            //parse topic for shared flag
            final SharedSubscription sharedSubscription = sharedSubscriptionService.checkForSharedSubscription(topic);
            if (sharedSubscription == null) {
                //not a shared subscription
                topicTree.removeSubscriber(client, topic, null);
            } else {
                if (sharedSubscription.getTopicFilter().isEmpty()) {
                    disconnectSharedSubscriberWithEmptyTopic(client);

                    return Futures.immediateFuture(null);
                }
                topicTree.removeSubscriber(client, sharedSubscription.getTopicFilter(), sharedSubscription.getShareName());
            }

            final ListenableFuture<Void> persistFuture = singleWriter.submit(client, (bucketIndex, queueBuckets, queueIndex) -> {
                localPersistence.remove(client, topic, timestamp, bucketIndex);
                return null;
            });

            //set future result when local persistence future and topic tree future return;
            return Futures.whenAllComplete(persistFuture).call(() -> persistFuture.get(), MoreExecutors.directExecutor());
        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeAll(@NotNull final String clientId) {
        try {
            checkNotNull(clientId, "Client id must not be null");

            final Set<Topic> topics = localPersistence.getSubscriptions(clientId);
            final Set<TopicFilter> subscriptions = new HashSet<>();
            for (final Topic topic : topics) {
                final SharedSubscription sharedSubscription =
                        sharedSubscriptionService.checkForSharedSubscription(topic.getTopic());
                if (sharedSubscription == null) {
                    subscriptions.add(new TopicFilter(topic.getTopic(), null));
                } else {
                    subscriptions.add(new TopicFilter(sharedSubscription.getTopicFilter(), sharedSubscription.getShareName()));
                }
            }

            for (final TopicFilter subscription : subscriptions) {
                topicTree.removeSubscriber(clientId, subscription.getTopic(), subscription.getSharedName());
            }

            return removeAllLocally(clientId);

        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeAllLocally(@NotNull final String clientId) {
        return singleWriter.submit(clientId, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.removeAll(clientId, System.currentTimeMillis(), bucketIndex);
            return null;
        });
    }

    @NotNull
    private ListenableFuture<ImmutableList<SubscriptionResult>> addBatchedTopics(@NotNull final String clientId, @NotNull final ImmutableSet<Topic> topics) {
        final long timestamp = System.currentTimeMillis();

        final ClientSession session = clientSessionLocalPersistence.getSession(clientId);

        //It must not be possible to add subscriptions for an expired or not existing session
        if (session == null) {
            return Futures.immediateFuture(null);
        }

        final ImmutableSet.Builder<Subscription> sharedSubs = new ImmutableSet.Builder<>();
        final Set<Subscription> subscriptions = new HashSet<>();
        for (final Topic topic : topics) {

            //parse topic for shared flag
            final SharedSubscription sharedSubscription = sharedSubscriptionService.checkForSharedSubscription(topic.getTopic());
            if (sharedSubscription == null) {
                //not a shared subscription
                subscriptions.add(new Subscription(topic, SubscriptionFlags.getDefaultFlags(false, topic.isRetainAsPublished(), topic.isNoLocal()), null));
            } else {
                if (sharedSubscription.getTopicFilter().isEmpty()) {
                    disconnectSharedSubscriberWithEmptyTopic(clientId);

                    return Futures.immediateFuture(null);
                }

                // QoS 2 is not supported for shared subscriptions
                if (topic.getQoS() == QoS.EXACTLY_ONCE) {
                    topic.setQoS(QoS.AT_LEAST_ONCE);
                }

                final Subscription sharedSub = new Subscription(new Topic(sharedSubscription.getTopicFilter(), topic.getQoS(), topic.isNoLocal(),
                        topic.isRetainAsPublished(), topic.getRetainHandling(), topic.getSubscriptionIdentifier()),
                        SubscriptionFlags.getDefaultFlags(true, topic.isRetainAsPublished(), topic.isNoLocal()), sharedSubscription.getShareName());

                sharedSubs.add(sharedSub);
                subscriptions.add(sharedSub);
            }
        }
        final ImmutableList.Builder<SubscriptionResult> subscriptionResultBuilder = ImmutableList.builder();
        for (final Subscription subscription : subscriptions) {
            final boolean subscriberExisted = topicTree.addTopic(clientId, subscription.getTopic(), subscription.getFlags(), subscription.getSharedGroup());
            subscriptionResultBuilder.add(new SubscriptionResult(subscription.getTopic(), subscriberExisted, subscription.getSharedGroup()));
        }

        final ListenableFuture<Void> persistFuture = singleWriter.submit(clientId, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.addSubscriptions(clientId, topics, timestamp, bucketIndex);
            return null;
        });

        invalidateSharedSubscriptionCacheAndPoll(clientId, sharedSubs.build());

        //set future result when local persistence future and topic tree future return;
        return Futures.whenAllComplete(persistFuture).call(() -> subscriptionResultBuilder.build(), MoreExecutors.directExecutor());
    }

    /**
     * @inheritDoc
     */
    @Override
    public void invalidateSharedSubscriptionCacheAndPoll(final @NotNull String clientId, final @NotNull ImmutableSet<Subscription> sharedSubs) {

        Preconditions.checkNotNull(clientId, "Client id must never be null");
        Preconditions.checkNotNull(sharedSubs, "Subscriptions must never be null");

        final ClientSession session = clientSessionLocalPersistence.getSession(clientId);

        //not connected clients and empty subscription don't need invalidation of cache
        if ((session != null && !session.isConnected()) || sharedSubs.isEmpty()) {
            return;
        }

        final Channel channel = channelPersistence.get(clientId);
        if (channel != null && channel.isActive()) {
            for (final Subscription sharedSub : sharedSubs) {

                final Topic topic = sharedSub.getTopic();

                final String sharedSubId = sharedSub.getSharedGroup() + "/" + topic.getTopic();
                publishPollService.pollSharedPublishesForClient(clientId, sharedSubId, topic.getQoS().getQosNumber(), topic.getSubscriptionIdentifier(), channel);
                sharedSubscriptionService.invalidateSharedSubscriptionCache(clientId);
                sharedSubscriptionService.invalidateSharedSubscriberCache(sharedSubId);
                channel.attr(ChannelAttributes.NO_SHARED_SUBSCRIPTION).set(false);
                log.trace("Invalidated cache and polled for shared subscription '{}' and client '{}'", sharedSubId, clientId);
            }
        }
    }

    @NotNull
    public ListenableFuture<MultipleChunkResult<Map<String, ImmutableSet<Topic>>>> getAllLocalSubscribersChunk(@NotNull final ChunkCursor cursor) {
        return chunker.getAllLocalChunk(cursor, InternalConfigurations.PERSISTENCE_SUBSCRIPTIONS_MAX_CHUNK_SIZE,
                // Chunker.SingleWriterCall interface
                (bucket, lastKey, maxResults) -> singleWriter.submit(bucket,
                        // actual single writer call
                        (bucketIndex, ignored1, ignored2) ->
                                localPersistence.getAllSubscribersChunk(
                                        bucketIndex,
                                        lastKey,
                                        maxResults)));
    }

    @NotNull
    private ListenableFuture<Void> removeBatchedTopics(@NotNull final String clientId, @NotNull final ImmutableSet<String> topics) {

        final long timestamp = System.currentTimeMillis();

        final ImmutableSet.Builder<TopicFilter> topicsToRemoveBuilder = new ImmutableSet.Builder<>();

        for (final String topic : topics) {
            final SharedSubscription sharedSubscription = sharedSubscriptionService.checkForSharedSubscription(topic);
            if (sharedSubscription == null) {
                topicsToRemoveBuilder.add(new TopicFilter(topic, null));
            } else {
                topicsToRemoveBuilder.add(new TopicFilter(sharedSubscription.getTopicFilter(), sharedSubscription.getShareName()));
            }
        }

        final ImmutableSet<TopicFilter> topicsToRemove = topicsToRemoveBuilder.build();

        for (final TopicFilter topicFilter : topicsToRemove) {
            topicTree.removeSubscriber(clientId, topicFilter.getTopic(), topicFilter.getSharedName());
        }

        final ListenableFuture<Void> persistFuture = singleWriter.submit(clientId, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.removeSubscriptions(clientId, topics, timestamp, bucketIndex);
            return null;
        });

        return persistFuture;
    }

    private void disconnectSharedSubscriberWithEmptyTopic(final @NotNull String clientId) {
        final Channel channel = channelPersistence.get(clientId);
        if (channel != null) {
            mqttServerDisconnector.disconnect(channel,
                    "A client (IP: {}) sent a shared subscription with an empty topic. Disconnecting client.",
                    "Sent shared subscription with empty topic",
                    Mqtt5DisconnectReasonCode.TOPIC_FILTER_INVALID,
                    ReasonStrings.DISCONNECT_TOPIC_NAME_INVALID_SHARED_EMPTY);
        } else {
            //at least log if that happens
            log.debug("Client {} sent a shared subscription with empty topic.", clientId);
        }
    }

    @Override
    @NotNull
    public ImmutableSet<Topic> getSharedSubscriptions(@NotNull final String client) {

        checkNotNull(client, "Client id must not be null");
        final ImmutableSet<Topic> subscriptions = getSubscriptions(client);
        final ImmutableSet.Builder<Topic> sharedSubscriptions = ImmutableSet.builder();
        for (final Topic subscription : subscriptions) {
            final boolean isSharedSubscription = sharedSubscriptionService.checkForSharedSubscription(subscription.getTopic()) != null;
            if (isSharedSubscription) {
                sharedSubscriptions.add(subscription);
            }
        }
        return sharedSubscriptions.build();
    }

    @NotNull
    @Override
    public ListenableFuture<Void> cleanUp(final int bucketIndex) {
        return singleWriter.submit(bucketIndex, (bucketIndex1, queueBuckets, queueIndex) -> {
            localPersistence.cleanUp(bucketIndex1);
            return null;
        });
    }

    @NotNull
    @Override
    public ListenableFuture<Void> closeDB() {
        return closeDB(localPersistence, singleWriter);
    }
}
