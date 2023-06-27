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
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.AbstractPersistence;
import com.hivemq.persistence.ProducerQueues;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

@LazySingleton
public class ClientQueuePersistenceImpl extends AbstractPersistence implements ClientQueuePersistence {

    public static final int SHARED_IN_FLIGHT_MARKER = 1;

    private final @NotNull ClientQueueLocalPersistence localPersistence;
    private final @NotNull ProducerQueues singleWriter;
    private final @NotNull MqttConfigurationService mqttConfigurationService;
    private final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence;
    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ConnectionPersistence connectionPersistence;
    private final @NotNull PublishPollService publishPollService;

    @Inject
    public ClientQueuePersistenceImpl(
            final @NotNull ClientQueueLocalPersistence localPersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull MqttConfigurationService mqttConfigurationService,
            final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence,
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ConnectionPersistence connectionPersistence,
            final @NotNull PublishPollService publishPollService) {
        this.localPersistence = localPersistence;
        this.mqttConfigurationService = mqttConfigurationService;
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        this.topicTree = topicTree;
        this.connectionPersistence = connectionPersistence;
        this.publishPollService = publishPollService;
        singleWriter = singleWriterService.getQueuedMessagesQueue();
    }

    @Override
    public @NotNull ListenableFuture<Void> add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull PUBLISH publish,
            final boolean retained,
            final long queueLimit) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
            checkNotNull(publish, "Publish must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }

        return singleWriter.submit(queueId, (bucketIndex) -> {
            localPersistence.add(queueId,
                    shared,
                    publish,
                    queueLimit,
                    mqttConfigurationService.getQueuedMessagesStrategy(),
                    retained,
                    bucketIndex);
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
    public @NotNull ListenableFuture<Void> add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull List<PUBLISH> publishes,
            final boolean retained,
            final long queueLimit) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
            checkNotNull(publishes, "Publishes must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }

        return singleWriter.submit(queueId, (bucketIndex) -> {
            final boolean queueWasEmpty = localPersistence.size(queueId, shared, bucketIndex) == 0;
            localPersistence.add(queueId,
                    shared,
                    publishes,
                    queueLimit,
                    mqttConfigurationService.getQueuedMessagesStrategy(),
                    retained,
                    bucketIndex);
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

    @Override
    public void publishAvailable(final @NotNull String client) {
        final ClientSession session = clientSessionLocalPersistence.getSession(client);
        if (session == null || !session.isConnected()) {
            return;
        }

        final ClientConnection clientConnection = connectionPersistence.get(client);
        if (clientConnection == null || !clientConnection.getChannel().isActive()) {
            return;
        }

        if (clientConnection.isMessagesInFlight()) {
            return;
        }
        clientConnection.getChannel()
                .eventLoop()
                .submit(() -> publishPollService.pollNewMessages(client, clientConnection.getChannel()));
    }

    @Override
    public void sharedPublishAvailable(final @NotNull String sharedSubscription) {
        publishPollService.pollSharedPublishes(sharedSubscription);
    }

    @Override
    public @NotNull ListenableFuture<ImmutableList<PUBLISH>> readNew(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull ImmutableIntArray packetIds,
            final long byteLimit) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
            checkNotNull(packetIds, "Message ID's must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }
        return singleWriter.submit(queueId,
                bucketIndex -> localPersistence.readNew(queueId, shared, packetIds, byteLimit, bucketIndex));
    }

    @Override
    public @NotNull ListenableFuture<ImmutableList<PUBLISH>> readShared(
            final @NotNull String sharedSubscription, final int messageLimit, final long byteLimit) {
        checkNotNull(sharedSubscription, "Shared subscription must not be null");
        // We reuse the non shared read new logic but without providing real message ID's.
        final ImmutableIntArray.Builder builder = ImmutableIntArray.builder(messageLimit);
        for (int i = 0; i < messageLimit; i++) {
            builder.add(SHARED_IN_FLIGHT_MARKER); // We don't need a real message id here, messages are just marked as in-flight
        }
        return readNew(sharedSubscription, true, builder.build(), byteLimit);
    }

    @Override
    public @NotNull ListenableFuture<ImmutableList<MessageWithID>> readInflight(
            final @NotNull String client, final long byteLimit, final int messageLimit) {
        checkNotNull(client, "Client ID must not be null");
        return singleWriter.submit(client,
                bucketIndex -> localPersistence.readInflight(client, false, messageLimit, byteLimit, bucketIndex));
    }

    @Override
    public @NotNull ListenableFuture<Void> remove(final @NotNull String client, final int packetId) {
        checkNotNull(client, "Client ID must not be null");
        return singleWriter.submit(client, (bucketIndex) -> {
            localPersistence.remove(client, packetId, bucketIndex);
            return null;
        });
    }

    @Override
    public @NotNull ListenableFuture<Void> putPubrel(final @NotNull String client, final int packetId) {
        checkNotNull(client, "Client must not be null");
        return singleWriter.submit(client, (bucketIndex) -> {
            localPersistence.replace(client, new PUBREL(packetId), bucketIndex);
            return null;
        });
    }

    @Override
    public @NotNull ListenableFuture<Void> clear(final @NotNull String queueId, final boolean shared) {
        checkNotNull(queueId, "Queue ID must not be");
        return singleWriter.submit(queueId, (bucketIndex) -> {
            localPersistence.clear(queueId, shared, bucketIndex);
            return null;
        });
    }

    @Override
    public @NotNull ListenableFuture<Void> closeDB() {
        return closeDB(localPersistence, singleWriter);
    }

    @Override
    public @NotNull ListenableFuture<Void> cleanUp(final int bucketIndex) {
        return singleWriter.submit(bucketIndex, (bucketIndex1) -> {
            final ImmutableSet<String> sharedQueues = localPersistence.cleanUp(bucketIndex1);
            for (final String sharedQueue : sharedQueues) {
                final SharedSubscriptionService.SharedSubscription sharedSubscription =
                        SharedSubscriptionService.splitTopicAndGroup(sharedQueue);
                final ImmutableSet<SubscriberWithQoS> sharedSubscriber = topicTree.getSharedSubscriber(
                        sharedSubscription.getShareName(),
                        sharedSubscription.getTopicFilter());
                if (sharedSubscriber.isEmpty()) {
                    localPersistence.clear(sharedQueue, true, bucketIndex);
                }
            }
            return null;
        });
    }

    @Override
    public @NotNull ListenableFuture<Integer> size(final @NotNull String queueId, final boolean shared) {
        return singleWriter.submit(queueId, (bucketIndex) -> localPersistence.size(queueId, shared, bucketIndex));
    }

    @Override
    public @NotNull ListenableFuture<Void> removeShared(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
        return singleWriter.submit(sharedSubscription, (bucketIndex) -> {
            localPersistence.removeShared(sharedSubscription, uniqueId, bucketIndex);
            return null;
        });
    }

    @Override
    public @NotNull ListenableFuture<Void> removeInFlightMarker(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
        return singleWriter.submit(sharedSubscription, (bucketIndex) -> {
            localPersistence.removeInFlightMarker(sharedSubscription, uniqueId, bucketIndex);
            // We notify the clients that there are new messages to poll.
            sharedPublishAvailable(sharedSubscription);
            return null;

        });
    }

    @Override
    public @NotNull ListenableFuture<Void> removeAllQos0Messages(final @NotNull String queueId, final boolean shared) {
        return singleWriter.submit(queueId, (bucketIndex) -> {
            localPersistence.removeAllQos0Messages(queueId, shared, bucketIndex);
            return null;
        });
    }

    public static class Key implements Comparable<Key> {

        private final @NotNull String queueId;
        private final boolean shared;

        public Key(final @NotNull String queueId, final boolean shared) {
            this.queueId = queueId;
            this.shared = shared;
        }

        public @NotNull String getQueueId() {
            return queueId;
        }

        public boolean isShared() {
            return shared;
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Key key = (Key) o;
            return shared == key.shared && Objects.equals(queueId, key.queueId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(queueId, shared);
        }

        @Override
        public int compareTo(final @NotNull Key other) {
            int compare = queueId.compareTo(other.queueId);
            if (compare == 0) {
                compare = Boolean.compare(shared, other.shared);
            }
            return compare;
        }
    }
}
