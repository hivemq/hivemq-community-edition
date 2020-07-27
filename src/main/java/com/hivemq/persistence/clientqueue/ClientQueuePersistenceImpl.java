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
package com.hivemq.persistence.clientqueue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.AbstractPersistence;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.ProducerQueues;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.payload.PayloadPersistenceException;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.Channel;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;

/**
 * @author Lukas Brandl
 */
@LazySingleton
public class ClientQueuePersistenceImpl extends AbstractPersistence implements ClientQueuePersistence {

    public static final int SHARED_IN_FLIGHT_MARKER = 1;

    @NotNull
    private final ClientQueueLocalPersistence localPersistence;
    @NotNull
    private final ProducerQueues singleWriter;
    @NotNull
    private final MqttConfigurationService mqttConfigurationService;
    @NotNull
    private final ClientSessionLocalPersistence clientSessionLocalPersistence;
    @NotNull
    private final MessageDroppedService messageDroppedService;
    @NotNull
    private final LocalTopicTree topicTree;
    @NotNull
    private final ChannelPersistence channelPersistence;
    @NotNull
    private final PublishPollService publishPollService;

    @Inject
    public ClientQueuePersistenceImpl(
            @NotNull final ClientQueueLocalPersistence localPersistence,
            @NotNull final SingleWriterService singleWriterService,
            @NotNull final MqttConfigurationService mqttConfigurationService,
            @NotNull final ClientSessionLocalPersistence clientSessionLocalPersistence,
            @NotNull final MessageDroppedService messageDroppedService,
            @NotNull final LocalTopicTree topicTree,
            @NotNull final ChannelPersistence channelPersistence,
            @NotNull final PublishPollService publishPollService) {
        this.localPersistence = localPersistence;
        this.singleWriter = singleWriterService.getQueuedMessagesQueue();
        this.mqttConfigurationService = mqttConfigurationService;
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        this.messageDroppedService = messageDroppedService;
        this.topicTree = topicTree;
        this.channelPersistence = channelPersistence;
        this.publishPollService = publishPollService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<Void> add(
            @NotNull final String queueId, final boolean shared, @NotNull final PUBLISH publish,
            final boolean retained, final long queueLimit) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
            checkNotNull(publish, "Publish must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }

        return singleWriter.submit(queueId, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.add(queueId, shared, publish, queueLimit, mqttConfigurationService.getQueuedMessagesStrategy(),
                    retained, bucketIndex);
            final int queueSize = localPersistence.size(queueId, shared, bucketIndex);
            if (queueSize == 1) {
                if (shared) {
                    sharedPublishAvailable(queueId);
                } else {
                    publishAvailable(queueId);
                }
            }
            return null;
        });
    }

    @Override
    @NotNull
    public ListenableFuture<Void> add(
            @NotNull final String queueId, final boolean shared, @NotNull final List<PUBLISH> publishes,
            final boolean retained, final long queueLimit) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
            checkNotNull(publishes, "Publishes must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }

        return singleWriter.submit(queueId, (bucketIndex, queueBuckets, queueIndex) -> {
            final boolean queueWasEmpty = localPersistence.size(queueId, shared, bucketIndex) == 0;
            localPersistence.add(queueId, shared, publishes, queueLimit, mqttConfigurationService.getQueuedMessagesStrategy(),
                    retained, bucketIndex);
            if (queueWasEmpty) {
                if (shared) {
                    sharedPublishAvailable(queueId);
                } else {
                    publishAvailable(queueId);
                }
            }
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publishAvailable(@NotNull final String client) {
        final ClientSession session = clientSessionLocalPersistence.getSession(client);
        if (session == null || !session.isConnected()) {
            return;
        }

        final Channel channel = channelPersistence.get(client);
        if (channel == null || !channel.isActive()) {
            return;
        }

        if (ChannelUtils.messagesInFlight(channel)) {
            return;
        }
        channel.eventLoop().submit(() -> publishPollService.pollNewMessages(client, channel));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sharedPublishAvailable(@NotNull final String sharedSubscription) {
        publishPollService.pollSharedPublishes(sharedSubscription);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<ImmutableList<PUBLISH>> readNew(
            @NotNull final String queueId, final boolean shared, @NotNull final ImmutableIntArray packetIds,
            final long byteLimit) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
            checkNotNull(packetIds, "Message ID's must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }
        return singleWriter.submit(
                queueId, (bucketIndex, queueBuckets, queueIndex) -> checkPayloadReference(
                        localPersistence.readNew(queueId, shared, packetIds, byteLimit, bucketIndex), queueId, shared));
    }

    @NotNull
    private <T extends MessageWithID> ImmutableList<T> checkPayloadReference(
            @NotNull final ImmutableList<T> publishes,
            @NotNull final String queueId,
            final boolean shared) {
        List<T> reducedList = null;
        for (final T message : publishes) {
            if (message instanceof PUBLISH) {
                final PUBLISH publish = (PUBLISH) message;
                try {
                    publish.dereferencePayload();
                } catch (final PayloadPersistenceException e) {
                    messageDroppedService.failed(queueId, publish.getTopic(), publish.getQoS().getQosNumber());
                    if (shared) {
                        removeShared(queueId, publish.getUniqueId());
                    } else {
                        remove(queueId, publish.getPacketIdentifier());
                    }
                    if (reducedList == null) {
                        reducedList = new ArrayList<>(publishes);
                    }
                    reducedList.remove(message);
                }
            }
        }
        if (reducedList == null) {
            return publishes;
        }
        return ImmutableList.copyOf(reducedList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<ImmutableList<PUBLISH>> readShared(
            @NotNull final String sharedSubscription, final int messageLimit, final long byteLimit) {
        checkNotNull(sharedSubscription, "Shared subscription must not be null");
        // We reuse the non shared read new logic but without providing real message ID's.
        final ImmutableIntArray.Builder builder = ImmutableIntArray.builder(messageLimit);
        for (int i = 0; i < messageLimit; i++) {
            builder.add(
                    SHARED_IN_FLIGHT_MARKER); // We don't need a real message id here, messages are just marked as in-flight
        }
        return readNew(sharedSubscription, true, builder.build(), byteLimit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<ImmutableList<MessageWithID>> readInflight(
            @NotNull final String client, final long byteLimit, final int messageLimit) {
        checkNotNull(client, "Client ID must not be null");
        return singleWriter.submit(client, (bucketIndex, queueBuckets, queueIndex) -> {
            final ImmutableList<MessageWithID> messages =
                    localPersistence.readInflight(client, false, messageLimit, byteLimit, bucketIndex);
            return checkPayloadReference(messages, client, false);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<Void> remove(@NotNull final String client, final int packetId) {
        checkNotNull(client, "Client ID must not be null");
        return singleWriter.submit(client, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.remove(client, packetId, bucketIndex);
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<Void> putPubrel(@NotNull final String client, final int packetId) {
        checkNotNull(client, "Client must not be null");
        return singleWriter.submit(client, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.replace(client, new PUBREL(packetId), bucketIndex);
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<Void> clear(@NotNull final String queueId, final boolean shared) {
        checkNotNull(queueId, "Queue ID must not be");
        return singleWriter.submit(queueId, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.clear(queueId, shared, bucketIndex);
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<Void> closeDB() {
        return closeDB(localPersistence, singleWriter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<Void> cleanUp(final int bucketIndex) {
        return singleWriter.submit(bucketIndex, (bucketIndex1, queueBuckets, queueIndex) -> {
            final ImmutableSet<String> sharedQueues = localPersistence.cleanUp(bucketIndex1);
            for (final String sharedQueue : sharedQueues) {
                final SharedSubscription sharedSubscription =
                        SharedSubscriptionServiceImpl.splitTopicAndGroup(sharedQueue);
                final ImmutableSet<SubscriberWithQoS> sharedSubscriber =
                        topicTree.getSharedSubscriber(
                                sharedSubscription.getShareName(),
                                sharedSubscription.getTopicFilter());
                if (sharedSubscriber.isEmpty()) {
                    localPersistence.clear(sharedQueue, true, bucketIndex);
                }
            }
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ListenableFuture<Integer> size(@NotNull final String queueId, final boolean shared) {
        return singleWriter.submit(
                queueId,
                (bucketIndex, queueBuckets, queueIndex) -> localPersistence.size(queueId, shared, bucketIndex));
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public ListenableFuture<Void> removeShared(
            @NotNull final String sharedSubscription, @NotNull final String uniqueId) {
        return singleWriter.submit(sharedSubscription, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.removeShared(sharedSubscription, uniqueId, bucketIndex);
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public ListenableFuture<Void> removeInFlightMarker(
            @NotNull final String sharedSubscription, @NotNull final String uniqueId) {
        return singleWriter.submit(sharedSubscription, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.removeInFlightMarker(sharedSubscription, uniqueId, bucketIndex);
            // We notify the clients that there are new messages to poll.
            sharedPublishAvailable(sharedSubscription);
            return null;

        });
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public ListenableFuture<Void> removeAllQos0Messages(@NotNull final String queueId, final boolean shared) {
        return singleWriter.submit(queueId, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.removeAllQos0Messages(queueId, shared, bucketIndex);
            return null;
        });
    }

    public static class Key implements Comparable<Key> {

        @NotNull
        private final String queueId;
        private final boolean shared;

        public Key(@NotNull final String queueId, final boolean shared) {
            this.queueId = queueId;
            this.shared = shared;
        }

        @NotNull
        public String getQueueId() {
            return queueId;
        }

        public boolean isShared() {
            return shared;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Key key = (Key) o;
            return shared == key.shared &&
                    Objects.equals(queueId, key.queueId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(queueId, shared);
        }

        @Override
        public int compareTo(@NotNull final Key other) {
            int compare = this.queueId.compareTo(other.queueId);
            if (compare == 0) {
                compare = Boolean.compare(this.shared, other.shared);
            }
            return compare;
        }
    }
}
