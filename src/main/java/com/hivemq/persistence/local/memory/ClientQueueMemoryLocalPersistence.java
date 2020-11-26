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
package com.hivemq.persistence.local.memory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.annotations.ExecuteInSingleWriter;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.ObjectMemoryEstimation;
import com.hivemq.util.PublishUtil;
import com.hivemq.util.Strings;
import com.hivemq.util.ThreadPreConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.QOS_0_MEMORY_HARD_LIMIT_DIVISOR;
import static com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl.Key;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class ClientQueueMemoryLocalPersistence implements ClientQueueLocalPersistence {

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(ClientQueueMemoryLocalPersistence.class);

    private static final int NO_PACKET_ID = 0;

    private final @NotNull MessageDroppedService messageDroppedService;

    private final @NotNull PublishPayloadPersistence payloadPersistence;

    final private @NotNull Map<Key, LinkedList<MessageWithID>> @NotNull [] qos12MessageBuckets;
    private final @NotNull Map<Key, LinkedList<PublishWithRetained>> @NotNull [] qos0MessageBuckets;
    private final @NotNull Map<Key, AtomicInteger> @NotNull [] queueSizeBuckets;
    private final @NotNull Map<Key, AtomicInteger> @NotNull [] retainedQueueSizeBuckets;

    //must be concurrent
    private final @NotNull Map<String, AtomicInteger> clientQos0MemoryMap;

    private final int qos0ClientMemoryLimit;
    private final long qos0MemoryLimit;
    private final int retainedMessageMax;

    private final @NotNull AtomicLong qos0MessagesMemory;
    private final @NotNull AtomicLong totalMemorySize;

    @Inject
    ClientQueueMemoryLocalPersistence(
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull MetricRegistry metricRegistry) {

        retainedMessageMax = InternalConfigurations.RETAINED_MESSAGE_QUEUE_SIZE.get();
        qos0ClientMemoryLimit = InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT.get();

        final int bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
        this.messageDroppedService = messageDroppedService;
        this.payloadPersistence = payloadPersistence;
        this.qos0MemoryLimit = getQos0MemoryLimit();

        //must be concurrent as it is not protected by bucket access
        this.clientQos0MemoryMap = new ConcurrentHashMap<>();
        this.totalMemorySize = new AtomicLong();
        this.qos0MessagesMemory = new AtomicLong();

        //noinspection unchecked
        this.qos12MessageBuckets = new HashMap[bucketCount];
        //noinspection unchecked
        this.qos0MessageBuckets = new HashMap[bucketCount];
        //noinspection unchecked
        this.queueSizeBuckets = new HashMap[bucketCount];
        //noinspection unchecked
        this.retainedQueueSizeBuckets = new HashMap[bucketCount];

        for (int i = 0; i < bucketCount; i++) {
            qos12MessageBuckets[i] = new HashMap<>();
            qos0MessageBuckets[i] = new HashMap<>();
            queueSizeBuckets[i] = new HashMap<>();
            retainedQueueSizeBuckets[i] = new HashMap<>();
        }

        metricRegistry.register(
                HiveMQMetrics.QUEUED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name(),
                (Gauge<Long>) totalMemorySize::get);

    }

    private long getQos0MemoryLimit() {
        final long maxHeap = Runtime.getRuntime().maxMemory();
        final long maxHardLimit;

        final int hardLimitDivisor = QOS_0_MEMORY_HARD_LIMIT_DIVISOR.get();

        if (hardLimitDivisor < 1) {
            //fallback to default if config failed
            maxHardLimit = maxHeap / 4;
        } else {
            maxHardLimit = maxHeap / hardLimitDivisor;
        }
        log.debug("{} allocated for qos 0 inflight messages", Strings.convertBytes(maxHardLimit));
        return maxHardLimit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void add(
            @NotNull final String queueId, final boolean shared, @NotNull final PUBLISH publish, final long max,
            @NotNull final QueuedMessagesStrategy strategy, final boolean retained, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(publish, "Publish must not be null");
        checkNotNull(strategy, "Strategy must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        add(queueId, shared, List.of(publish), max, strategy, retained, bucketIndex);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void add(
            @NotNull final String queueId, final boolean shared, @NotNull final List<PUBLISH> publishes, final long max,
            @NotNull final QueuedMessagesStrategy strategy, final boolean retained, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(publishes, "Publishes must not be null");
        checkNotNull(strategy, "Strategy must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);

        final Map<Key, LinkedList<MessageWithID>> bucket = qos12MessageBuckets[bucketIndex];

        final AtomicInteger queueSize = getOrPutQueueSize(key, bucketIndex);
        final AtomicInteger retainedQueueSize = getOrPutRetainedQueueSize(key, bucketIndex);
        final int qos0Size = qos0Size(key, bucketIndex);

        for (final PUBLISH publish : publishes) {
            final PublishWithRetained publishWithRetained = new PublishWithRetained(publish, retained);
            if (publish.getQoS() == QoS.AT_MOST_ONCE) {
                addQos0Publish(key, publishWithRetained, bucketIndex);
            } else {
                final int qos1And2QueueSize = queueSize.get() - qos0Size - retainedQueueSize.get();
                if (qos1And2QueueSize >= max && !retained) {
                    if (strategy == QueuedMessagesStrategy.DISCARD) {
                        logAndDecrementPayloadReference(publish, shared, queueId);
                        continue;
                    } else {
                        final boolean discarded = discardOldest(bucket, key, false);
                        if (!discarded) {
                            //discard this message if no old could be discarded
                            logAndDecrementPayloadReference(publish, shared, queueId);
                            continue;
                        }
                    }
                } else if (retainedQueueSize.get() >= retainedMessageMax && retained) {
                    if (strategy == QueuedMessagesStrategy.DISCARD) {
                        logAndDecrementPayloadReference(publish, shared, queueId);
                        continue;
                    } else {
                        final boolean discarded = discardOldest(bucket, key, true);
                        if (!discarded) {
                            //discard this message if no old could be discarded
                            logAndDecrementPayloadReference(publish, shared, queueId);
                            continue;
                        }
                    }
                } else {
                    queueSize.incrementAndGet();
                    if (retained) {
                        retainedQueueSize.incrementAndGet();
                    }
                }

                publishWithRetained.setPacketIdentifier(NO_PACKET_ID);
                getOrPutMessageQueue(key, bucket).add(publishWithRetained);
                increaseMessagesMemory(publishWithRetained.getEstimatedSize());
            }
        }

    }

    private void addQos0Publish(@NotNull final Key key, @NotNull final PublishWithRetained publishWithRetained, final int bucketIndex) {
        final long currentQos0MessagesMemory = qos0MessagesMemory.get();
        if (currentQos0MessagesMemory >= qos0MemoryLimit) {
            if (key.isShared()) {
                messageDroppedService.qos0MemoryExceededShared(
                        key.getQueueId(), publishWithRetained.getTopic(), 0, currentQos0MessagesMemory, qos0MemoryLimit);
            } else {
                messageDroppedService.qos0MemoryExceeded(
                        key.getQueueId(), publishWithRetained.getTopic(), 0, currentQos0MessagesMemory, qos0MemoryLimit);
            }
            payloadPersistence.decrementReferenceCounter(publishWithRetained.getPublishId());
            return;
        }

        if (!key.isShared()) {
            final AtomicInteger clientQos0Memory = clientQos0MemoryMap.get(key.getQueueId());
            if (clientQos0Memory != null && clientQos0Memory.get() >= qos0ClientMemoryLimit) {
                messageDroppedService.qos0MemoryExceeded(key.getQueueId(), publishWithRetained.getTopic(), 0, clientQos0Memory.get(), qos0ClientMemoryLimit);
                payloadPersistence.decrementReferenceCounter(publishWithRetained.getPublishId());
                return;
            }
        }

        final Map<Key, LinkedList<PublishWithRetained>> bucketMessages = qos0MessageBuckets[bucketIndex];
        getOrPutQos0MessageQueue(key, bucketMessages).add(publishWithRetained);
        getOrPutQueueSize(key, bucketIndex).incrementAndGet();
        if (publishWithRetained.retained) {
            getOrPutRetainedQueueSize(key, bucketIndex).incrementAndGet();
        }
        increaseQos0MessagesMemory(publishWithRetained.getEstimatedSize());
        increaseClientQos0MessagesMemory(key, publishWithRetained.getEstimatedSize());
        increaseMessagesMemory(publishWithRetained.getEstimatedSize());
    }


    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @ExecuteInSingleWriter
    public ImmutableList<PUBLISH> readNew(
            @NotNull final String queueId, final boolean shared, @NotNull final ImmutableIntArray packetIds,
            final long bytesLimit, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(packetIds, "Packet IDs must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);

        final AtomicInteger queueSize = getOrPutQueueSize(key, bucketIndex);
        if (queueSize.get() == 0) {
            return ImmutableList.of();
        }

        final Map<Key, LinkedList<PublishWithRetained>> qos0MessageBucket = qos0MessageBuckets[bucketIndex];
        final LinkedList<PublishWithRetained> qos0Messages = getOrPutQos0MessageQueue(key, qos0MessageBucket);

        // In case there are only qos 0 messages
        if (queueSize.get() == qos0Messages.size()) {
            return getQos0Publishes(packetIds, bytesLimit, bucketIndex, key, qos0Messages);
        }

        final Map<Key, LinkedList<MessageWithID>> bucket = qos12MessageBuckets[bucketIndex];
        final LinkedList<MessageWithID> messageQueue = getOrPutMessageQueue(key, bucket);

        final int countLimit = packetIds.length();
        int messageCount = 0;
        int packetIdIndex = 0;
        int bytes = 0;
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();

        final Iterator<MessageWithID> iterator = messageQueue.iterator();
        while (iterator.hasNext()) {
            final MessageWithID messageWithID = iterator.next();
            if (!(messageWithID instanceof PublishWithRetained)) {
                continue;
            }
            final PublishWithRetained publishWithRetained = (PublishWithRetained) messageWithID;
            if (publishWithRetained.getPacketIdentifier() != NO_PACKET_ID) {
                //already inflight
                continue;
            }

            if (PublishUtil.checkExpiry(publishWithRetained.getTimestamp(), publishWithRetained.getMessageExpiryInterval())) {
                iterator.remove();
                payloadPersistence.decrementReferenceCounter(publishWithRetained.getPublishId());
                getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                if (publishWithRetained.retained) {
                    getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                }
                increaseMessagesMemory(-publishWithRetained.getEstimatedSize());
                //do not return here, because we could have a QoS 0 message left
            } else {

                final int packetId = packetIds.get(packetIdIndex);
                publishWithRetained.setPacketIdentifier(packetId);
                publishes.add(publishWithRetained);
                packetIdIndex++;
                messageCount++;
                bytes += publishWithRetained.getEstimatedSizeInMemory();
                if ((messageCount == countLimit) || (bytes > bytesLimit)) {
                    break;
                }
            }

            // poll a qos 0 message
            if (!qos0Messages.isEmpty()) {
                final PUBLISH qos0Publish = pollQos0Message(key, qos0Messages, bucketIndex);
                if (!PublishUtil.checkExpiry(qos0Publish.getTimestamp(), qos0Publish.getMessageExpiryInterval())) {
                    publishes.add(qos0Publish);
                    messageCount++;
                    bytes += qos0Publish.getEstimatedSizeInMemory();
                }
            }
            if ((messageCount == countLimit) || (bytes > bytesLimit)) {
                break;
            }
        }
        return publishes.build();

    }

    @NotNull
    private ImmutableList<PUBLISH> getQos0Publishes(
            final @NotNull ImmutableIntArray packetIds,
            final long bytesLimit,
            final int bucketIndex,
            final @NotNull Key key,
            final @NotNull LinkedList<PublishWithRetained> qos0Messages) {
        final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
        int qos0MessagesFound = 0;
        int qos0Bytes = 0;
        while (qos0MessagesFound < packetIds.length() && bytesLimit > qos0Bytes) {
            final PUBLISH qos0Publish = pollQos0Message(key, qos0Messages, bucketIndex);
            if (!PublishUtil.checkExpiry(qos0Publish.getTimestamp(), qos0Publish.getMessageExpiryInterval())) {
                publishes.add(qos0Publish);
                qos0MessagesFound++;
                qos0Bytes += qos0Publish.getEstimatedSizeInMemory();
            }
            if (qos0Messages.isEmpty()) {
                break;
            }
        }

        return publishes.build();
    }

    @NotNull
    private PUBLISH pollQos0Message(@NotNull final Key key, final @NotNull LinkedList<PublishWithRetained> qos0Messages, final int bucketIndex) {
        final PublishWithRetained publishWithRetained = qos0Messages.remove(0);
        getOrPutQueueSize(key, bucketIndex).decrementAndGet();
        if (publishWithRetained.retained) {
            getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
        }
        increaseQos0MessagesMemory(-publishWithRetained.getEstimatedSize());
        increaseClientQos0MessagesMemory(key, -publishWithRetained.getEstimatedSize());
        increaseMessagesMemory(-publishWithRetained.getEstimatedSize());
        payloadPersistence.decrementReferenceCounter(publishWithRetained.getPublishId());
        return publishWithRetained;
    }

    @NotNull
    @Override
    @ExecuteInSingleWriter
    public ImmutableList<MessageWithID> readInflight(
            @NotNull final String client, final boolean shared, final int batchSize,
            final long bytesLimit, final int bucketIndex) {
        checkNotNull(client, "client id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(client, shared);

        final @NotNull Map<Key, LinkedList<MessageWithID>> bucket = qos12MessageBuckets[bucketIndex];
        final LinkedList<MessageWithID> messageQueue = getOrPutMessageQueue(key, bucket);

        int messageCount = 0;
        int bytes = 0;
        final ImmutableList.Builder<MessageWithID> messages = ImmutableList.builder();

        for (final MessageWithID messageWithID : messageQueue) {
            // Stop at first non inflight message
            // This works because in-flight messages are always first in the queue
            if (messageWithID.getPacketIdentifier() == NO_PACKET_ID) {
                break;
            }
            messages.add(messageWithID);
            messageCount++;

            if (messageWithID instanceof PublishWithRetained) {
                final PublishWithRetained publishWithRetained = (PublishWithRetained) messageWithID;
                bytes += publishWithRetained.getEstimatedSizeInMemory();
                publishWithRetained.setDuplicateDelivery(true);
            }

            if ((messageCount == batchSize) || (bytes > bytesLimit)) {
                break;
            }
        }
        return messages.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    @ExecuteInSingleWriter
    public String replace(@NotNull final String client, @NotNull final PUBREL pubrel, final int bucketIndex) {
        checkNotNull(client, "client id must not be null");
        checkNotNull(pubrel, "pubrel must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(client, false);

        final Map<Key, LinkedList<MessageWithID>> bucket = qos12MessageBuckets[bucketIndex];
        final LinkedList<MessageWithID> messageQueue = getOrPutMessageQueue(key, bucket);

        boolean packetIdFound = false;
        String replacedId = null;
        boolean retained = false;

        int messageIndexInQueue = -1;

        for (final MessageWithID messageWithID : messageQueue) {
            messageIndexInQueue++;
            final int packetId = messageWithID.getPacketIdentifier();
            if (packetId == NO_PACKET_ID) {
                break;
            }
            if (packetId == pubrel.getPacketIdentifier()) {
                packetIdFound = true;
                if (messageWithID instanceof PublishWithRetained) {
                    final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                    retained = publish.retained;
                    payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                    increaseMessagesMemory(-publish.getEstimatedSize());
                    pubrel.setExpiryInterval(publish.getMessageExpiryInterval());
                    pubrel.setPublishTimestamp(publish.getTimestamp());
                    replacedId = publish.getUniqueId();
                } else if (messageWithID instanceof PubrelWithRetained) {
                    final PubrelWithRetained pubrelWithRetained = (PubrelWithRetained) messageWithID;
                    pubrel.setExpiryInterval(pubrelWithRetained.getExpiryInterval());
                    pubrel.setPublishTimestamp(pubrelWithRetained.getPublishTimestamp());
                    retained = pubrelWithRetained.retained;
                }
                break;
            }
        }
        final PubrelWithRetained pubrelWithRetained = new PubrelWithRetained(pubrel, retained);
        if (packetIdFound) {
            messageQueue.set(messageIndexInQueue, pubrelWithRetained);
        } else {
            getOrPutQueueSize(key, bucketIndex).incrementAndGet();
            // Ensure unknown PUBRELs are always first in queue
            messageQueue.addFirst(pubrelWithRetained);
        }
        increaseMessagesMemory(pubrelWithRetained.getEstimatedSize());
        return replacedId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public String remove(@NotNull final String client, final int packetId, final int bucketIndex) {
        return remove(client, packetId, null, bucketIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    @ExecuteInSingleWriter
    public String remove(
            @NotNull final String client, final int packetId, @Nullable final String uniqueId, final int bucketIndex) {
        checkNotNull(client, "client id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(client, false);
        final Map<Key, LinkedList<MessageWithID>> bucket = qos12MessageBuckets[bucketIndex];
        final LinkedList<MessageWithID> messageQueue = getOrPutMessageQueue(key, bucket);
        final Iterator<MessageWithID> iterator = messageQueue.iterator();
        while (iterator.hasNext()) {
            final MessageWithID messageWithID = iterator.next();
            if (messageWithID.getPacketIdentifier() == packetId) {
                String removedId = null;
                if (messageWithID instanceof PublishWithRetained) {
                    final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                    if (uniqueId != null && !uniqueId.equals(publish.getUniqueId())) {
                        break;
                    }
                    payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                    removedId = publish.getUniqueId();
                }
                getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                if (isRetained(messageWithID)) {
                    getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                }
                increaseMessagesMemory(-getMessageSize(messageWithID));
                iterator.remove();
                return removedId;
            }
        }
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public int size(@NotNull final String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX); // QueueSizes are not thread save
        final Key key = new Key(queueId, shared);
        final AtomicInteger queueSize = queueSizeBuckets[bucketIndex].get(key);
        return (queueSize == null) ? 0 : queueSize.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public int qos0Size(@NotNull final String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX); // QueueSizes are not thread save
        final Key key = new Key(queueId, shared);
        return qos0Size(key, bucketIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void clear(@NotNull final String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);

        final Map<Key, LinkedList<MessageWithID>> bucket = qos12MessageBuckets[bucketIndex];
        final LinkedList<MessageWithID> messageQueue = getOrPutMessageQueue(key, bucket);
        for (final MessageWithID messageWithID : messageQueue) {
            if (messageWithID instanceof PublishWithRetained) {
                payloadPersistence.decrementReferenceCounter(((PublishWithRetained) messageWithID).getPublishId());
            }
            increaseMessagesMemory(-getMessageSize(messageWithID));
        }
        bucket.remove(key);

        final Map<Key, LinkedList<PublishWithRetained>> bucketMessages = qos0MessageBuckets[bucketIndex];
        final LinkedList<PublishWithRetained> qos0Messages = getOrPutQos0MessageQueue(key, bucketMessages);
        for (final PublishWithRetained qos0Message : qos0Messages) {
            increaseQos0MessagesMemory(-qos0Message.getEstimatedSize());
            increaseClientQos0MessagesMemory(key, -qos0Message.getEstimatedSize());
            increaseMessagesMemory(-qos0Message.getEstimatedSize());
            payloadPersistence.decrementReferenceCounter(qos0Message.getPublishId());
        }
        bucketMessages.remove(key);
        queueSizeBuckets[bucketIndex].remove(key);
        retainedQueueSizeBuckets[bucketIndex].remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void removeAllQos0Messages(@NotNull final String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);
        final Map<Key, LinkedList<PublishWithRetained>> bucketMessages = qos0MessageBuckets[bucketIndex];
        final LinkedList<PublishWithRetained> publishesWithRetained = getOrPutQos0MessageQueue(key, bucketMessages);
        for (final PublishWithRetained publishWithRetained : publishesWithRetained) {
            payloadPersistence.decrementReferenceCounter(publishWithRetained.getPublishId());
            getOrPutQueueSize(key, bucketIndex).decrementAndGet();
            if (publishWithRetained.retained) {
                getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
            }
            increaseQos0MessagesMemory(-publishWithRetained.getEstimatedSize());
            increaseClientQos0MessagesMemory(key, -publishWithRetained.getEstimatedSize());
            increaseMessagesMemory(-publishWithRetained.getEstimatedSize());
        }
        bucketMessages.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @ExecuteInSingleWriter
    public ImmutableSet<String> cleanUp(final int bucketIndex) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final ImmutableSet.Builder<String> sharedQueues = ImmutableSet.builder();
        final Map<Key, AtomicInteger> bucketClients = queueSizeBuckets[bucketIndex];

        for (final Key bucketKey : bucketClients.keySet()) {
            if (bucketKey.isShared()) {
                sharedQueues.add(bucketKey.getQueueId());
            }
            cleanExpiredMessages(bucketKey, bucketIndex);
        }

        return sharedQueues.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void removeShared(
            @NotNull final String sharedSubscription, @NotNull final String uniqueId, final int bucketIndex) {
        checkNotNull(sharedSubscription, "Shared subscription must not be null");
        checkNotNull(uniqueId, "Unique id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(sharedSubscription, true);
        final Map<Key, LinkedList<MessageWithID>> bucket = qos12MessageBuckets[bucketIndex];
        final LinkedList<MessageWithID> messageQueue = getOrPutMessageQueue(key, bucket);
        final Iterator<MessageWithID> iterator = messageQueue.iterator();
        while (iterator.hasNext()) {
            final MessageWithID messageWithID = iterator.next();
            if (messageWithID instanceof PublishWithRetained) {
                final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                if (!uniqueId.equals(publish.getUniqueId())) {
                    continue;
                }
                payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                if (publish.retained) {
                    getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                }
                increaseMessagesMemory(-publish.getEstimatedSize());
                iterator.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ExecuteInSingleWriter
    public void removeInFlightMarker(
            @NotNull final String sharedSubscription, @NotNull final String uniqueId, final int bucketIndex) {
        checkNotNull(sharedSubscription, "Shared subscription must not be null");
        checkNotNull(uniqueId, "Unique id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(sharedSubscription, true);
        final Map<Key, LinkedList<MessageWithID>> bucket = qos12MessageBuckets[bucketIndex];
        final LinkedList<MessageWithID> messageQueue = getOrPutMessageQueue(key, bucket);
        for (final MessageWithID messageWithID : messageQueue) {
            if (messageWithID instanceof PublishWithRetained) {
                final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                if (!uniqueId.equals(publish.getUniqueId())) {
                    continue;
                }
                publish.setPacketIdentifier(NO_PACKET_ID);
                break;
            }
        }

    }

    public @NotNull Map<String, AtomicInteger> getClientQos0MemoryMap() {
        return clientQos0MemoryMap;
    }

    @Override
    @ExecuteInSingleWriter
    public void closeDB(final int bucketIndex) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        qos0MessageBuckets[bucketIndex].clear();
        qos12MessageBuckets[bucketIndex].clear();
        queueSizeBuckets[bucketIndex].clear();
        retainedQueueSizeBuckets[bucketIndex].clear();
        clientQos0MemoryMap.clear();
        totalMemorySize.set(0L);
        qos0MessagesMemory.set(0L);
    }

    private int getMessageSize(final @NotNull MessageWithID messageWithID) {
        if (messageWithID instanceof PublishWithRetained) {
            return ((PublishWithRetained) messageWithID).getEstimatedSize();
        }
        if (messageWithID instanceof PubrelWithRetained) {
            return ((PubrelWithRetained) messageWithID).getEstimatedSize();
        }
        return 0;
    }

    private boolean isRetained(final @NotNull MessageWithID messageWithID) {
        if (messageWithID instanceof PublishWithRetained) {
            return ((PublishWithRetained) messageWithID).retained;
        }
        if (messageWithID instanceof PubrelWithRetained) {
            return ((PubrelWithRetained) messageWithID).retained;
        }
        return false;
    }

    private void logMessageDropped(
            @NotNull final PUBLISH publish, final boolean shared, @NotNull final String queueId) {
        if (shared) {
            messageDroppedService.queueFullShared(queueId, publish.getTopic(), publish.getQoS().getQosNumber());
        } else {
            messageDroppedService.queueFull(queueId, publish.getTopic(), publish.getQoS().getQosNumber());
        }
    }

    /**
     * @param size the amount of bytes the currently used qos 0 memory will be increased by. May be negative.
     */
    private void increaseQos0MessagesMemory(final int size) {
        if (size < 0) {
            qos0MessagesMemory.addAndGet(size - ObjectMemoryEstimation.linkedListNodeOverhead());
        } else {
            qos0MessagesMemory.addAndGet(size + ObjectMemoryEstimation.linkedListNodeOverhead());
        }
    }

    /**
     * @param size the amount of bytes the currently used memory will be increased by. May be negative.
     */
    private void increaseMessagesMemory(final int size) {
        if (size < 0) {
            totalMemorySize.addAndGet(size - ObjectMemoryEstimation.linkedListNodeOverhead());
        } else {
            totalMemorySize.addAndGet(size + ObjectMemoryEstimation.linkedListNodeOverhead());
        }
    }

    /**
     * @param size the amount of bytes the currently used qos 0 memory will be increased by. May be negative.
     */
    @VisibleForTesting
    void increaseClientQos0MessagesMemory(final Key key, final int size) {
        if (key.isShared()) {
            return;
        }

        clientQos0MemoryMap.compute(key.getQueueId(), (clientId, clientQos0Memory) -> {
            if (clientQos0Memory == null) {
                if (size < 0) {
                    //strange case that should never happen as there must be a increase before a decrease..
                    return null;
                } else {
                    return new AtomicInteger(size + ObjectMemoryEstimation.linkedListNodeOverhead());
                }
            }
            if (size < 0) {
                clientQos0Memory.addAndGet(size - ObjectMemoryEstimation.linkedListNodeOverhead());
            } else {
                clientQos0Memory.addAndGet(size + ObjectMemoryEstimation.linkedListNodeOverhead());
            }
            if (clientQos0Memory.get() <= 0) {
                return null; //this removes the AtomicInteger
            }
            return clientQos0Memory;
        });
    }

    /**
     * @return true if a message was discarded, else false
     */
    private boolean discardOldest(
            @NotNull final Map<Key, LinkedList<MessageWithID>> bucket,
            @NotNull final Key key,
            final boolean retainedOnly) {

        final LinkedList<MessageWithID> publishes = getOrPutMessageQueue(key, bucket);

        final Iterator<MessageWithID> iterator = publishes.iterator();
        while (iterator.hasNext()) {
            final MessageWithID messageWithID = iterator.next();
            if (!(messageWithID instanceof PublishWithRetained)) {
                continue;
            }
            final PublishWithRetained publish = (PublishWithRetained) messageWithID;
            // we must no discard inflight messages
            if (publish.getPacketIdentifier() != NO_PACKET_ID) {
                continue;
            }
            // Messages that are queued as retained messages are not discarded,
            // otherwise a client could only receive a limited amount of retained messages per subscription.
            if (retainedOnly && !publish.retained ||
                    !retainedOnly && publish.retained) {
                continue;
            }
            logAndDecrementPayloadReference(publish, key.isShared(), key.getQueueId());
            iterator.remove();
            return true;
        }
        return false;

    }

    private void logAndDecrementPayloadReference(
            final @NotNull PUBLISH publish, final boolean shared, final @NotNull String queueId) {
        logMessageDropped(publish, shared, queueId);
        payloadPersistence.decrementReferenceCounter(publish.getPublishId());
    }


    private void cleanExpiredMessages(@NotNull final Key key, final int bucketIndex) {

        final Map<Key, LinkedList<PublishWithRetained>> bucketMessages = qos0MessageBuckets[bucketIndex];
        final LinkedList<PublishWithRetained> qos0Messages = getOrPutQos0MessageQueue(key, bucketMessages);
        final Iterator<PublishWithRetained> iterator = qos0Messages.iterator();
        while (iterator.hasNext()) {
            final PublishWithRetained publishWithRetained = iterator.next();
            if (PublishUtil.checkExpiry(publishWithRetained.getTimestamp(), publishWithRetained.getMessageExpiryInterval())) {
                getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                increaseQos0MessagesMemory(-publishWithRetained.getEstimatedSize());
                increaseClientQos0MessagesMemory(key, -publishWithRetained.getEstimatedSize());
                increaseMessagesMemory(-publishWithRetained.getEstimatedSize());
                payloadPersistence.decrementReferenceCounter(publishWithRetained.getPublishId());
                if (publishWithRetained.retained) {
                    getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                }
                iterator.remove();
            }
        }

        final Map<Key, LinkedList<MessageWithID>> bucket = qos12MessageBuckets[bucketIndex];
        final LinkedList<MessageWithID> messageQueue = getOrPutMessageQueue(key, bucket);

        final Iterator<MessageWithID> qos12iterator = messageQueue.iterator();
        while (qos12iterator.hasNext()) {
            final MessageWithID messageWithID = qos12iterator.next();
            if (messageWithID instanceof PubrelWithRetained) {
                final PubrelWithRetained pubrel = (PubrelWithRetained) messageWithID;
                if (!InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS) {
                    continue;
                }
                if (pubrel.getExpiryInterval() == null || pubrel.getPublishTimestamp() == null) {
                    continue;
                }
                if (!PublishUtil.checkExpiry(pubrel.getPublishTimestamp(), pubrel.getExpiryInterval())) {
                    continue;
                }
                getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                if (pubrel.retained) {
                    getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                }
                increaseMessagesMemory(-pubrel.getEstimatedSize());
                qos12iterator.remove();

            } else if (messageWithID instanceof PublishWithRetained) {
                final PublishWithRetained publish = (PublishWithRetained) messageWithID;
                final boolean expireInflight = InternalConfigurations.EXPIRE_INFLIGHT_MESSAGES;
                final boolean isInflight = publish.getQoS() == QoS.EXACTLY_ONCE && publish.getPacketIdentifier() > 0;
                final boolean drop = PublishUtil.checkExpiry(publish) && (!isInflight || expireInflight);
                if (drop) {
                    payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                    getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                    if (publish.retained) {
                        getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                    }
                    increaseMessagesMemory(-publish.getEstimatedSize());
                    qos12iterator.remove();
                }
            }

        }
    }

    @NotNull
    private AtomicInteger getOrPutQueueSize(@NotNull final Key key, final int bucketIndex) {
        final Map<Key, AtomicInteger> queueSizeBucket = queueSizeBuckets[bucketIndex];
        return getOrPutQueueSizeFromBucket(key, queueSizeBucket);
    }

    private @NotNull AtomicInteger getOrPutRetainedQueueSize(@NotNull final Key key, final int bucketIndex) {
        final Map<Key, AtomicInteger> queueSizeBucket = retainedQueueSizeBuckets[bucketIndex];
        return getOrPutQueueSizeFromBucket(key, queueSizeBucket);
    }

    private @NotNull AtomicInteger getOrPutQueueSizeFromBucket(
            final @NotNull Key key, final @NotNull Map<Key, AtomicInteger> queueSizeBucket) {
        return queueSizeBucket.compute(key, (ignore, queueSize) -> {
            if (queueSize == null) {
                return new AtomicInteger();
            }
            return queueSize;
        });
    }

    @NotNull
    private LinkedList<PublishWithRetained> getOrPutQos0MessageQueue(@NotNull final Key key, final @NotNull Map<Key, LinkedList<PublishWithRetained>> bucket) {
        return bucket.compute(key, (ignore, publishes) -> {
            if (publishes == null) {
                return new LinkedList<>();
            }
            return publishes;
        });
    }

    @NotNull
    private LinkedList<MessageWithID> getOrPutMessageQueue(@NotNull final Key key, final @NotNull Map<Key, LinkedList<MessageWithID>> bucket) {
        return bucket.compute(key, (ignore, publishes) -> {
            if (publishes == null) {
                return new LinkedList<>();
            }
            return publishes;
        });
    }

    private int qos0Size(@NotNull final Key key, final int bucketIndex) {
        final Map<Key, LinkedList<PublishWithRetained>> bucketMessages = qos0MessageBuckets[bucketIndex];
        final LinkedList<PublishWithRetained> publishes = bucketMessages.get(key);
        if (publishes != null) {
            return publishes.size();
        }
        return 0;
    }

    @VisibleForTesting
    static class PublishWithRetained extends PUBLISH {

        private final boolean retained;

        PublishWithRetained(@NotNull final PUBLISH publish, final boolean retained) {
            super(publish, publish.getPersistence());
            this.retained = retained;
        }

        int getEstimatedSize() {
            return getEstimatedSizeInMemory()  // publish
                    + ObjectMemoryEstimation.objectShellSize() // the object itself
                    + ObjectMemoryEstimation.booleanSize(); // retain flag
        }
    }

    private static class PubrelWithRetained extends PUBREL {

        private final boolean retained;

        private PubrelWithRetained(@NotNull final PUBREL pubrel, final boolean retained) {
            super(pubrel.getPacketIdentifier(),
                    pubrel.getReasonCode(),
                    pubrel.getReasonString(),
                    pubrel.getUserProperties(),
                    pubrel.getPublishTimestamp(),
                    pubrel.getExpiryInterval());
            this.retained = retained;
        }

        private int getEstimatedSize() {
            return getEstimatedSizeInMemory()  // publish
                    + ObjectMemoryEstimation.objectShellSize() // the object itself
                    + ObjectMemoryEstimation.booleanSize(); // retain flag
        }
    }
}
