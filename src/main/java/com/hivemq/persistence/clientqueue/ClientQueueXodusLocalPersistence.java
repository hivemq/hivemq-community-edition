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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.persistence.local.xodus.TransactionCommitActions;
import com.hivemq.persistence.local.xodus.XodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.Strings;
import com.hivemq.util.ThreadPreConditions;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.QOS_0_MEMORY_HARD_LIMIT_DIVISOR;
import static com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl.Key;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

@LazySingleton
public class ClientQueueXodusLocalPersistence extends XodusLocalPersistence implements ClientQueueLocalPersistence {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientQueueXodusLocalPersistence.class);

    public static final @NotNull String PERSISTENCE_NAME = "client_queue";
    public static final @NotNull String PERSISTENCE_VERSION = "040500";
    private static final int LINKED_LIST_NODE_OVERHEAD = 24;

    private final @NotNull ClientQueuePersistenceSerializer serializer;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull ConcurrentHashMap<Integer, Map<Key, AtomicInteger>> queueSizeBuckets;
    private final @NotNull ConcurrentHashMap<Integer, Map<Key, AtomicInteger>> retainedQueueSizeBuckets;
    private final int retainedMessageMax;
    private final @NotNull PublishPayloadPersistence payloadPersistence;

    private final @NotNull ConcurrentHashMap<Integer, Map<Key, LinkedList<PublishWithRetained>>> qos0MessageBuckets;
    private final @NotNull AtomicLong qos0MessagesMemory = new AtomicLong();
    private final long qos0MemoryLimit;
    private final int qos0ClientMemoryLimit;

    private final @NotNull ConcurrentHashMap<String, AtomicInteger> clientQos0MemoryMap;

    // this caches the lower bound for a publish without packet-id,
    // the cached index is guaranteed to be lower or equal to the index
    //so it is safe to seek to this index without missing a publish without packet-id
    @VisibleForTesting
    final @NotNull Cache<String, Long> sharedSubLastPacketWithoutIdCache;

    @Inject
    ClientQueueXodusLocalPersistence(
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull EnvironmentUtil environmentUtil,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PersistenceStartup persistenceStartup,
            final @NotNull MessageDroppedService messageDroppedService) {

        super(environmentUtil,
                localPersistenceFileUtil,
                persistenceStartup,
                InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get(),
                true);
        retainedMessageMax = InternalConfigurations.RETAINED_MESSAGE_QUEUE_SIZE.get();
        qos0ClientMemoryLimit = InternalConfigurations.QOS_0_MEMORY_LIMIT_PER_CLIENT_BYTES.get();

        serializer = new ClientQueuePersistenceSerializer();
        this.messageDroppedService = messageDroppedService;
        queueSizeBuckets = new ConcurrentHashMap<>();
        retainedQueueSizeBuckets = new ConcurrentHashMap<>();
        this.payloadPersistence = payloadPersistence;
        qos0MessageBuckets = new ConcurrentHashMap<>();
        qos0MemoryLimit = getQos0MemoryLimit();
        clientQos0MemoryMap = new ConcurrentHashMap<>();
        sharedSubLastPacketWithoutIdCache = CacheBuilder.newBuilder()
                .maximumSize(InternalConfigurations.SHARED_SUBSCRIPTION_WITHOUT_PACKET_ID_CACHE_MAX_SIZE_ENTRIES.get())
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .build();
    }

    private static long getQos0MemoryLimit() {
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

    @Override
    protected @NotNull String getName() {
        return PERSISTENCE_NAME;
    }

    @Override
    protected @NotNull String getVersion() {
        return PERSISTENCE_VERSION;
    }

    @Override
    protected @NotNull StoreConfig getStoreConfig() {
        return StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING;
    }

    @Override
    protected @NotNull Logger getLogger() {
        return log;
    }

    @PostConstruct
    protected void postConstruct() {
        super.postConstruct();
    }

    @Override
    protected void init() {
        log.debug("Initializing payload reference count and queue sizes for {} persistence.", PERSISTENCE_NAME);

        checkNotNull(buckets, "Buckets must be initialized at this point");

        for (int i = 0; i < buckets.length; i++) {
            qos0MessageBuckets.put(i, new ConcurrentHashMap<>());
            queueSizeBuckets.put(i, new ConcurrentSkipListMap<>());
            retainedQueueSizeBuckets.put(i, new ConcurrentHashMap<>());
        }

        final AtomicLong nextMessageIndex = new AtomicLong(Long.MAX_VALUE / 2);

        for (final Bucket bucket : buckets) {

            bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                    Key currentKey = null;
                    int queueSize = 0;
                    int retainedSize = 0;
                    while (cursor.getNext()) {

                        final Key key = serializer.deserializeKeyId(cursor.getKey());

                        if (!key.equals(currentKey)) {

                            if (currentKey != null && queueSize != 0) {
                                queueSizeBuckets.get(BucketUtils.getBucket(currentKey.getQueueId(), getBucketCount()))
                                        .put(currentKey, new AtomicInteger(queueSize));
                                if (retainedSize != 0) {
                                    retainedQueueSizeBuckets.get(BucketUtils.getBucket(currentKey.getQueueId(),
                                            getBucketCount())).put(currentKey, new AtomicInteger(retainedSize));
                                }
                            }
                            queueSize = 0;
                            retainedSize = 0;
                        }

                        currentKey = key;

                        final MessageWithID messageWithID = serializer.deserializeValue(cursor.getValue());
                        if (messageWithID instanceof PUBLISH) {
                            final long deserializeIndex = serializer.deserializeIndex(cursor.getKey());
                            if (nextMessageIndex.get() <= deserializeIndex) {
                                nextMessageIndex.set(deserializeIndex + 1);
                            }
                            final PUBLISH publish = (PUBLISH) messageWithID;
                            payloadPersistence.incrementReferenceCounterOnBootstrap(publish.getPublishId());
                        }
                        queueSize++;
                        if (serializer.deserializeRetained(cursor.getValue())) {
                            retainedSize++;
                        }
                    }

                    //we do not put if we change bucket, therefor we must check after
                    //we must check this, because a bucket may be empty
                    if (currentKey != null) {
                        if (queueSizeBuckets.get(BucketUtils.getBucket(currentKey.getQueueId(), getBucketCount()))
                                .get(currentKey) == null) {
                            queueSizeBuckets.get(BucketUtils.getBucket(currentKey.getQueueId(), getBucketCount()))
                                    .put(currentKey, new AtomicInteger(queueSize));
                        }
                        if (retainedQueueSizeBuckets.get(BucketUtils.getBucket(currentKey.getQueueId(),
                                getBucketCount())).get(currentKey) == null) {
                            retainedQueueSizeBuckets.get(BucketUtils.getBucket(currentKey.getQueueId(),
                                    getBucketCount())).put(currentKey, new AtomicInteger(retainedSize));
                        }
                    }

                }
            });
        }

        ClientQueuePersistenceSerializer.NEXT_PUBLISH_NUMBER.set(nextMessageIndex.get());
    }

    private void decrementSharedSubscriptionIndexFirstMessageWithoutPacketId(
            final @NotNull String sharedSubId, final @NotNull Long newIndex) {
        final Long previous = sharedSubLastPacketWithoutIdCache.getIfPresent(sharedSubId);
        if (previous == null || previous > newIndex) {
            sharedSubLastPacketWithoutIdCache.put(sharedSubId, newIndex);
        }
    }

    private void incrementSharedSubscriptionIndexFirstMessageWithoutPacketId(
            final @NotNull String sharedSubId, final @NotNull Long newIndex) {
        final Long previous = sharedSubLastPacketWithoutIdCache.getIfPresent(sharedSubId);
        if (previous == null || previous < newIndex) {
            sharedSubLastPacketWithoutIdCache.put(sharedSubId, newIndex);
        }
    }

    @Override
    public void add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull PUBLISH publish,
            final long max,
            final @NotNull QueuedMessagesStrategy strategy,
            final boolean retained,
            final int bucketIndex) {

        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(publish, "Publish must not be null");
        checkNotNull(strategy, "Strategy must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);
        if (publish.getQoS() == QoS.AT_MOST_ONCE) {
            addQos0Publish(key, new PublishWithRetained(publish, retained), bucketIndex);
            return;
        }

        final Bucket bucket = buckets[bucketIndex];

        final AtomicInteger queueSize = getOrPutQueueSize(key, bucketIndex);
        final AtomicInteger retainedQueueSize = getOrPutRetainedQueueSize(key, bucketIndex);
        final int qos1And2QueueSize = queueSize.get() - qos0Size(key, bucketIndex) - retainedQueueSize.get();

        if (!retained && qos1And2QueueSize >= max) {
            if (dropForStrategy(queueId, shared, retained, publish, strategy, key, bucket)) {
                return;
            }
        } else if (retained && retainedQueueSize.get() >= retainedMessageMax) {
            if (dropForStrategy(queueId, shared, retained, publish, strategy, key, bucket)) {
                return;
            }
        } else {
            queueSize.incrementAndGet();
            if (retained) {
                retainedQueueSize.incrementAndGet();
            }
        }

        final ByteIterable keyBytes = serializer.serializeNewPublishKey(key);
        final ByteIterable valueBytes = serializer.serializePublishWithoutPacketId(publish, retained);

        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            txn.setCommitHook(() -> payloadPersistence.add(publish.getPayload(), publish.getPublishId()));
            bucket.getStore().put(txn, keyBytes, valueBytes);
        });
    }

    /**
     * @return true if the argument publish was discarded, false if another publish was discarded
     */
    private boolean dropForStrategy(
            final @NotNull String queueId,
            final boolean shared,
            final boolean retained,
            final @NotNull PUBLISH publish,
            final @NotNull MqttConfigurationService.QueuedMessagesStrategy strategy,
            final @NotNull Key key,
            final @NotNull Bucket bucket) {

        if (strategy == QueuedMessagesStrategy.DISCARD) {
            logMessageDropped(publish, shared, queueId);
            return true;
        } else {
            final boolean discarded = discardOldest(bucket, key, retained);
            if (!discarded) {
                logMessageDropped(publish, shared, queueId);
                return true;
            }
        }
        return false;
    }

    @Override
    public void add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull List<PUBLISH> publishes,
            final long max,
            final @NotNull QueuedMessagesStrategy strategy,
            final boolean retained,
            final int bucketIndex) {

        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(publishes, "Publishes must not be null");
        checkNotNull(strategy, "Strategy must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);
        final ImmutableList.Builder<PUBLISH> qos1and2Publishes = ImmutableList.builder();

        for (final PUBLISH publish : publishes) {
            if (publish.getQoS() == QoS.AT_MOST_ONCE) {
                addQos0Publish(key, new PublishWithRetained(publish, retained), bucketIndex);
            } else {
                qos1and2Publishes.add(publish);
            }
        }

        final Bucket bucket = buckets[bucketIndex];

        final AtomicInteger queueSize = getOrPutQueueSize(key, bucketIndex);
        final AtomicInteger retainedQueueSize = getOrPutRetainedQueueSize(key, bucketIndex);
        final int qos0Size = qos0Size(key, bucketIndex);

        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            final TransactionCommitActions commitActions = TransactionCommitActions.asCommitHookFor(txn);

            for (final PUBLISH publish : qos1and2Publishes.build()) {

                final int qos1And2QueueSize = queueSize.get() - qos0Size - retainedQueueSize.get();

                if (qos1And2QueueSize >= max && !retained) {
                    if (strategy == QueuedMessagesStrategy.DISCARD) {
                        logMessageDropped(publish, shared, queueId);
                        continue;
                    } else {
                        final boolean discarded = discardOldest(bucket, key, retained, txn, commitActions);
                        if (!discarded) {
                            logMessageDropped(publish, shared, queueId);
                            continue;
                        }
                    }
                } else if (retainedQueueSize.get() >= retainedMessageMax && retained) {
                    if (strategy == QueuedMessagesStrategy.DISCARD) {
                        logMessageDropped(publish, shared, queueId);
                        continue;
                    } else {
                        final boolean discarded = discardOldest(bucket, key, retained, txn, commitActions);
                        if (!discarded) {
                            //If there is no other message that could be dropped than this message will not be added
                            logMessageDropped(publish, shared, queueId);
                            continue;
                        }
                    }
                } else {
                    queueSize.incrementAndGet();
                    if (retained) {
                        retainedQueueSize.incrementAndGet();
                    }
                }
                final ByteIterable keyBytes = serializer.serializeNewPublishKey(key);
                final ByteIterable valueBytes = serializer.serializePublishWithoutPacketId(publish, retained);

                commitActions.add(() -> payloadPersistence.add(publish.getPayload(), publish.getPublishId()));

                bucket.getStore().put(txn, keyBytes, valueBytes);
            }
        });
    }

    private void addQos0Publish(
            final @NotNull Key key, final @NotNull PublishWithRetained publishWithRetained, final int bucketIndex) {

        final long currentQos0MessagesMemory = qos0MessagesMemory.get();
        final PUBLISH publish = publishWithRetained.publish;
        if (currentQos0MessagesMemory >= qos0MemoryLimit) {
            if (key.isShared()) {
                messageDroppedService.qos0MemoryExceededShared(key.getQueueId(),
                        publish.getTopic(),
                        0,
                        currentQos0MessagesMemory,
                        qos0MemoryLimit);
            } else {
                messageDroppedService.qos0MemoryExceeded(key.getQueueId(),
                        publish.getTopic(),
                        0,
                        currentQos0MessagesMemory,
                        qos0MemoryLimit);
            }
            return;
        }

        if (!key.isShared()) {
            final AtomicInteger clientQos0Memory = clientQos0MemoryMap.get(key.getQueueId());
            if (clientQos0Memory != null && clientQos0Memory.get() >= qos0ClientMemoryLimit) {
                messageDroppedService.qos0MemoryExceeded(key.getQueueId(),
                        publish.getTopic(),
                        0,
                        clientQos0Memory.get(),
                        qos0ClientMemoryLimit);
                return;
            }
        }

        getOrPutQos0Messages(key, bucketIndex).add(publishWithRetained);
        getOrPutQueueSize(key, bucketIndex).incrementAndGet();
        if (publishWithRetained.retained) {
            getOrPutRetainedQueueSize(key, bucketIndex).incrementAndGet();
        }
        increaseQos0MessagesMemory(publish.getEstimatedSizeInMemory());
        increaseClientQos0MessagesMemory(key, publish.getEstimatedSizeInMemory());

        payloadPersistence.add(publish.getPayload(), publish.getPublishId());
        publish.setPayload(null);
    }

    private void logMessageDropped(
            final @NotNull PUBLISH publish, final boolean shared, final @NotNull String queueId) {
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
            qos0MessagesMemory.addAndGet(size - LINKED_LIST_NODE_OVERHEAD);
        } else {
            qos0MessagesMemory.addAndGet(size + LINKED_LIST_NODE_OVERHEAD);
        }
    }

    /**
     * @param size the amount of bytes the currently used qos 0 memory will be increased by. May be negative.
     */
    @VisibleForTesting
    void increaseClientQos0MessagesMemory(final @NotNull Key key, final int size) {
        if (key.isShared()) {
            return;
        }

        final AtomicInteger qos0MemoryPerClient =
                clientQos0MemoryMap.compute(key.getQueueId(), (clientId, clientQos0Memory) -> {
                    if (clientQos0Memory == null) {
                        if (size < 0) {
                            //strange case that should never happen as there must be a increase before a decrease..
                            return new AtomicInteger(0);
                        } else {
                            return new AtomicInteger(size + LINKED_LIST_NODE_OVERHEAD);
                        }
                    }
                    if (size < 0) {
                        clientQos0Memory.addAndGet(size - LINKED_LIST_NODE_OVERHEAD);
                    } else {
                        clientQos0Memory.addAndGet(size + LINKED_LIST_NODE_OVERHEAD);
                    }
                    return clientQos0Memory;
                });

        if (qos0MemoryPerClient.get() <= 0) {
            clientQos0MemoryMap.remove(key.getQueueId());
        }
    }

    /**
     * @return true if a message was discarded, else false
     */
    private boolean discardOldest(final @NotNull Bucket bucket, final @NotNull Key key, final boolean retainedOnly) {
        return bucket.getEnvironment().computeInExclusiveTransaction(txn -> {
            final TransactionCommitActions commitActions = TransactionCommitActions.asCommitHookFor(txn);
            return discardOldest(bucket, key, retainedOnly, txn, commitActions);
        });
    }

    /**
     * @return true if a message was discarded, else false
     */
    private boolean discardOldest(
            final @NotNull Bucket bucket,
            final @NotNull Key key,
            final boolean retainedOnly,
            final @NotNull Transaction txn,
            final @NotNull TransactionCommitActions commitActions) {

        final AtomicBoolean discarded = new AtomicBoolean();
        try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

            // Go to the first entry without a packet id because we don't discard in-flight messages
            iterateQueue(cursor, key, true, () -> {
                final ByteIterable value = cursor.getValue();
                // Messages that are queue as retained messages are not discarded,
                // otherwise a client could only receive a limited amount of retained message per subscription.
                if (retainedOnly != serializer.deserializeRetained(value)) {
                    return true;
                }
                final PUBLISH publish = (PUBLISH) serializer.deserializeValue(value);
                commitActions.add(() -> {
                    logMessageDropped(publish, key.isShared(), key.getQueueId());
                    payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                });
                cursor.deleteCurrent();

                discarded.set(true);
                return false;
            });
        }

        return discarded.get();
    }

    private boolean setPayloadIfExistingElseDrop(
            final @NotNull PUBLISH publish,
            final @NotNull String queueId,
            final boolean shared,
            final int bucketIndex) {

        final byte[] payload = payloadPersistence.get(publish.getPublishId());
        if (payload == null) {
            messageDroppedService.failed(queueId, publish.getTopic(), publish.getQoS().getQosNumber());
            // No payload exists: remove the PUBLISH from its persistent queue. (Not necessary for QoS 0.)
            if (publish.getQoS() != QoS.AT_MOST_ONCE) {
                // Because having no payload is an unexpected error case, we're keeping it simple here: We call the
                // remove methods, which again roll a database transaction etc., even though we could already pass
                // the relevant data (cursor, key, serialized value, ...) here from the caller's transaction. This
                // works because any surrounding caller's transaction is not expected to also modify this PUBLISH,
                // hence we don't get colliding transactions.
                if (shared) {
                    removeShared(queueId, publish.getUniqueId(), bucketIndex);
                } else {
                    remove(queueId, publish.getPacketIdentifier(), publish.getUniqueId(), bucketIndex);
                }
            }
            return false;
        }
        publish.setPayload(payload);
        if (publish.getQoS() == QoS.AT_MOST_ONCE) {
            // We can decrement the persistence counter immediately because the QoS 0 PUBLISH has already been
            // removed from its (in-memory) queue, hence we won't attempt to access its payload again anyway.
            payloadPersistence.decrementReferenceCounter(publish.getPublishId());
        }
        return true;
    }

    @Override
    public @NotNull ImmutableList<PUBLISH> readNew(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull ImmutableIntArray packetIds,
            final long bytesLimit,
            final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(packetIds, "Packet IDs must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);

        final AtomicInteger queueSize = getOrPutQueueSize(key, bucketIndex);
        if (queueSize.get() == 0) {
            return ImmutableList.of();
        }

        final LinkedList<PublishWithRetained> qos0Messages = getOrPutQos0Messages(key, bucketIndex);
        if (queueSize.get() == qos0Messages.size()) {
            // In case there are only qos 0 messages
            final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();
            int qos0MessagesFound = 0;
            int qos0Bytes = 0;
            while (qos0MessagesFound < packetIds.length() && bytesLimit > qos0Bytes) {
                final PUBLISH qos0Publish = pollQos0Message(key, bucketIndex);
                if (qos0Publish.isExpired()) {
                    payloadPersistence.decrementReferenceCounter(qos0Publish.getPublishId());
                } else if (setPayloadIfExistingElseDrop(qos0Publish, queueId, shared, bucketIndex)) {
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

        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                final int countLimit = packetIds.length();
                final int[] messageCount = {0};
                final int[] packetIdIndex = {0};
                final int[] bytes = {0};
                final ImmutableList.Builder<PUBLISH> publishes = ImmutableList.builder();

                iterateQueue(cursor, key, true, () -> {
                    final ByteIterable serializedValue = cursor.getValue();
                    final PUBLISH publish = (PUBLISH) serializer.deserializeValue(serializedValue);
                    if (publish.isExpired()) {
                        cursor.deleteCurrent();
                        payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                        getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                        if (serializer.deserializeRetained(serializedValue)) {
                            getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                        }
                        //do not return here, because we could have a QoS 0 message left
                    } else {
                        if (!setPayloadIfExistingElseDrop(publish, queueId, shared, bucketIndex)) {
                            return true;
                        }

                        final int packetId = packetIds.get(packetIdIndex[0]);
                        publish.setPacketIdentifier(packetId);
                        bucket.getStore()
                                .put(txn,
                                        cursor.getKey(),
                                        serializer.serializeAndSetPacketId(serializedValue, packetId));

                        publishes.add(publish);
                        packetIdIndex[0]++;
                        messageCount[0]++;
                        bytes[0] += publish.getEstimatedSizeInMemory();
                        if ((messageCount[0] == countLimit) || (bytes[0] > bytesLimit)) {
                            return false;
                        }
                    }

                    // Add a qos 0 message
                    if (!qos0Messages.isEmpty()) {
                        final PUBLISH qos0Publish = pollQos0Message(key, bucketIndex);
                        if (qos0Publish.isExpired()) {
                            payloadPersistence.decrementReferenceCounter(qos0Publish.getPublishId());
                        } else if (setPayloadIfExistingElseDrop(qos0Publish, queueId, shared, bucketIndex)) {
                            publishes.add(qos0Publish);
                            messageCount[0]++;
                            bytes[0] += qos0Publish.getEstimatedSizeInMemory();
                        }
                    }
                    return (messageCount[0] != countLimit) && (bytes[0] <= bytesLimit);
                });
                return publishes.build();
            }
        });
    }

    private @NotNull PUBLISH pollQos0Message(final @NotNull Key key, final int bucketIndex) {
        final LinkedList<PublishWithRetained> qos0Messages = getOrPutQos0Messages(key, bucketIndex);
        final PublishWithRetained publishWithRetained = qos0Messages.poll();
        final PUBLISH qos0Publish = publishWithRetained.publish;
        getOrPutQueueSize(key, bucketIndex).decrementAndGet();
        if (publishWithRetained.retained) {
            getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
        }
        increaseQos0MessagesMemory(qos0Publish.getEstimatedSizeInMemory() * -1);
        increaseClientQos0MessagesMemory(key, qos0Publish.getEstimatedSizeInMemory() * -1);
        return qos0Publish;
    }

    @Override
    public @NotNull ImmutableList<MessageWithID> readInflight(
            final @NotNull String client,
            final boolean shared,
            final int batchSize,
            final long bytesLimit,
            final int bucketIndex) {
        checkNotNull(client, "client id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(client, shared);

        final Bucket bucket = buckets[bucketIndex];

        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                final int[] count = {0};
                final int[] bytes = {0};
                final ImmutableList.Builder<MessageWithID> messages = ImmutableList.builder();

                iterateQueue(cursor, key, false, () -> {
                    final ByteIterable serializedValue = cursor.getValue();
                    final MessageWithID message = serializer.deserializeValue(serializedValue);

                    // This works because in-flight messages are always first in the queue
                    if (message.getPacketIdentifier() == ClientQueuePersistenceSerializer.NO_PACKET_ID) {
                        return false;
                    }

                    if (message instanceof PUBLISH) {
                        final PUBLISH publish = (PUBLISH) message;
                        if (!setPayloadIfExistingElseDrop(publish, client, shared, bucketIndex)) {
                            return true;
                        }
                        bytes[0] += publish.getEstimatedSizeInMemory();
                        publish.setDuplicateDelivery(true);
                    }

                    messages.add(message);

                    count[0]++;

                    return (count[0] != batchSize) && (bytes[0] <= bytesLimit);
                });
                return messages.build();
            }
        });
    }

    @Override
    public @Nullable String replace(final @NotNull String client, final @NotNull PUBREL pubrel, final int bucketIndex) {
        checkNotNull(client, "client id must not be null");
        checkNotNull(pubrel, "pubrel must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(client, false);

        final Bucket bucket = buckets[bucketIndex];

        return bucket.getEnvironment().computeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                final boolean[] packetIdFound = new boolean[1];
                final String[] replacedId = new String[1];

                iterateQueue(cursor, key, false, () -> {
                    final MessageWithID message = serializer.deserializeValue(cursor.getValue());
                    final int packetId = message.getPacketIdentifier();
                    if (packetId == pubrel.getPacketIdentifier()) {
                        packetIdFound[0] = true;
                        final boolean retained = serializer.deserializeRetained(cursor.getValue());
                        if (message instanceof PUBLISH) {
                            final PUBLISH publish = (PUBLISH) message;
                            payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                            pubrel.setMessageExpiryInterval(publish.getMessageExpiryInterval());
                            pubrel.setPublishTimestamp(publish.getTimestamp());
                            replacedId[0] = publish.getUniqueId();
                        } else if (message instanceof PUBREL) {
                            pubrel.setMessageExpiryInterval(((PUBREL) message).getMessageExpiryInterval());
                            pubrel.setPublishTimestamp(((PUBREL) message).getPublishTimestamp());
                        }
                        final ByteIterable serializedPubRel = serializer.serializePubRel(pubrel, retained);
                        bucket.getStore().put(txn, cursor.getKey(), serializedPubRel);
                        return false;
                    }
                    return packetId != ClientQueuePersistenceSerializer.NO_PACKET_ID;
                });
                if (!packetIdFound[0]) {
                    getOrPutQueueSize(key, bucketIndex).incrementAndGet();
                    final ByteIterable serializedPubRel = serializer.serializePubRel(pubrel, false);
                    bucket.getStore().put(txn, serializer.serializeUnknownPubRelKey(key), serializedPubRel);
                }
                return replacedId[0];
            }
        });
    }

    @Override
    public String remove(final @NotNull String client, final int packetId, final int bucketIndex) {
        return remove(client, packetId, null, bucketIndex);
    }

    @Override
    public @Nullable String remove(
            final @NotNull String client, final int packetId, @Nullable final String uniqueId, final int bucketIndex) {
        checkNotNull(client, "client id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(client, false);

        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                final String[] result = {null};

                iterateQueue(cursor, key, false, () -> {
                    final MessageWithID message = serializer.deserializeValue(cursor.getValue());
                    if (message.getPacketIdentifier() == packetId) {
                        String removedId = null;
                        if (message instanceof PUBLISH) {
                            final PUBLISH publish = (PUBLISH) message;
                            if (uniqueId != null && !uniqueId.equals(publish.getUniqueId())) {
                                return false;
                            }
                            payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                            removedId = publish.getUniqueId();
                        }
                        getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                        if (serializer.deserializeRetained(cursor.getValue())) {
                            getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                        }
                        cursor.deleteCurrent();
                        result[0] = removedId;
                        return false;
                    }
                    return true;
                });
                return result[0];
            }
        });
    }

    @Override
    public int size(final @NotNull String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX); // QueueSizes are not thread save
        final Key key = new Key(queueId, shared);
        final AtomicInteger queueSize = queueSizeBuckets.get(bucketIndex).get(key);
        return (queueSize == null) ? 0 : queueSize.get();
    }

    @Override
    public void clear(final @NotNull String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                iterateQueue(cursor, key, false, () -> {
                    final MessageWithID message = serializer.deserializeValue(cursor.getValue());
                    if (message instanceof PUBLISH) {
                        payloadPersistence.decrementReferenceCounter(((PUBLISH) message).getPublishId());
                    }
                    cursor.deleteCurrent();
                    return true;
                });
            }
        });

        final LinkedList<PublishWithRetained> qos0Messages = getOrPutQos0Messages(key, bucketIndex);
        for (final PublishWithRetained qos0Message : qos0Messages) {
            increaseQos0MessagesMemory(qos0Message.publish.getEstimatedSizeInMemory() * -1);
            increaseClientQos0MessagesMemory(key, qos0Message.publish.getEstimatedSizeInMemory() * -1);
            payloadPersistence.decrementReferenceCounter(qos0Message.publish.getPublishId());
        }
        qos0MessageBuckets.get(bucketIndex).remove(key);
        queueSizeBuckets.get(bucketIndex).remove(key);
        retainedQueueSizeBuckets.get(bucketIndex).remove(key);
    }

    @Override
    public void removeAllQos0Messages(final @NotNull String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);
        final LinkedList<PublishWithRetained> publishesWithRetained = getOrPutQos0Messages(key, bucketIndex);
        final Iterator<PublishWithRetained> iterator = publishesWithRetained.iterator();
        while (iterator.hasNext()) {
            final PublishWithRetained publishWithRetained = iterator.next();
            final PUBLISH publish = publishWithRetained.publish;
            iterator.remove();
            payloadPersistence.decrementReferenceCounter(publish.getPublishId());
            getOrPutQueueSize(key, bucketIndex).decrementAndGet();
            if (publishWithRetained.retained) {
                getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
            }
            increaseQos0MessagesMemory(publish.getEstimatedSizeInMemory() * -1);
            increaseClientQos0MessagesMemory(key, publish.getEstimatedSizeInMemory() * -1);
        }
        qos0MessageBuckets.get(bucketIndex).remove(key);
    }

    @Override
    public @NotNull ImmutableSet<String> cleanUp(final int bucketIndex) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        if (stopped.get()) {
            return ImmutableSet.of();
        }

        final ImmutableSet.Builder<String> sharedQueues = ImmutableSet.builder();
        final Map<Key, AtomicInteger> bucketClients = queueSizeBuckets.get(bucketIndex);

        for (final Key bucketKey : bucketClients.keySet()) {
            if (bucketKey.isShared()) {
                sharedQueues.add(bucketKey.getQueueId());
            }
            cleanExpiredMessages(bucketKey, bucketIndex);
        }

        return sharedQueues.build();
    }

    @Override
    public void removeShared(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId, final int bucketIndex) {
        checkNotNull(sharedSubscription, "Shared subscription must not be null");
        checkNotNull(uniqueId, "Unique id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(sharedSubscription, true);

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                iterateQueue(cursor, key, false, () -> {
                    final MessageWithID message = serializer.deserializeValue(cursor.getValue());

                    if (message instanceof PUBLISH) {
                        final PUBLISH publish = (PUBLISH) message;
                        if (!uniqueId.equals(publish.getUniqueId())) {
                            return true;
                        }
                        payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                        getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                        if (serializer.deserializeRetained(cursor.getValue())) {
                            getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                        }
                        cursor.deleteCurrent();
                    }
                    return false;
                });
            }
        });
    }

    @Override
    public void removeInFlightMarker(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId, final int bucketIndex) {
        checkNotNull(sharedSubscription, "Shared subscription must not be null");
        checkNotNull(uniqueId, "Unique id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(sharedSubscription, true);

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                iterateQueue(cursor, key, false, () -> {
                    final MessageWithID message = serializer.deserializeValue(cursor.getValue());

                    if (message instanceof PUBLISH) {
                        final PUBLISH publish = (PUBLISH) message;
                        if (!uniqueId.equals(publish.getUniqueId())) {
                            return true;
                        }
                        final long index = serializer.deserializeIndex(cursor.getKey());
                        decrementSharedSubscriptionIndexFirstMessageWithoutPacketId(sharedSubscription, index);
                        bucket.getStore()
                                .put(txn, cursor.getKey(), serializer.serializePublishWithoutPacketId(publish, false));
                    }
                    return false;
                });
            }
        });
    }

    public @NotNull ConcurrentHashMap<Integer, Map<Key, AtomicInteger>> getQueueSizeBuckets() {
        return queueSizeBuckets;
    }

    public @NotNull ConcurrentHashMap<String, AtomicInteger> getClientQos0MemoryMap() {
        return clientQos0MemoryMap;
    }

    private void cleanExpiredMessages(final @NotNull Key key, final int bucketIndex) {
        final LinkedList<PublishWithRetained> qos0Messages = getOrPutQos0Messages(key, bucketIndex);
        final Iterator<PublishWithRetained> iterator = qos0Messages.iterator();
        while (iterator.hasNext()) {
            final PublishWithRetained publishWithRetained = iterator.next();
            final PUBLISH qos0Message = publishWithRetained.publish;
            if (qos0Message.isExpired()) {
                getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                increaseQos0MessagesMemory(qos0Message.getEstimatedSizeInMemory() * -1);
                increaseClientQos0MessagesMemory(key, qos0Message.getEstimatedSizeInMemory() * -1);
                payloadPersistence.decrementReferenceCounter(qos0Message.getPublishId());
                if (publishWithRetained.retained) {
                    getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                }
                iterator.remove();
            }
        }

        final Bucket bucket = buckets[bucketIndex];

        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                iterateQueue(cursor, key, false, () -> {
                    final ByteIterable serializedValue = cursor.getValue();
                    final MessageWithID message = serializer.deserializeValue(serializedValue);
                    if (message instanceof PUBREL) {
                        final PUBREL pubrel = (PUBREL) message;
                        if (!InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS_ENABLED) {
                            return true;
                        }
                        if (pubrel.getMessageExpiryInterval() == null || pubrel.getPublishTimestamp() == null) {
                            return true;
                        }
                        if (!pubrel.hasExpired()) {
                            return true;
                        }
                        getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                        if (serializer.deserializeRetained(serializedValue)) {
                            getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                        }
                        cursor.deleteCurrent();

                    } else if (message instanceof PUBLISH) {
                        final PUBLISH publish = (PUBLISH) message;
                        final boolean expireInflight = InternalConfigurations.EXPIRE_INFLIGHT_MESSAGES_ENABLED;
                        final boolean isInflight =
                                publish.getQoS() == QoS.EXACTLY_ONCE && publish.getPacketIdentifier() > 0;
                        final boolean drop = publish.isExpired() && (!isInflight || expireInflight);
                        if (drop) {
                            payloadPersistence.decrementReferenceCounter(publish.getPublishId());
                            getOrPutQueueSize(key, bucketIndex).decrementAndGet();
                            if (serializer.deserializeRetained(serializedValue)) {
                                getOrPutRetainedQueueSize(key, bucketIndex).decrementAndGet();
                            }
                            cursor.deleteCurrent();
                        }
                    }
                    return true;
                });
            }
        });
    }

    private int skipPrefix(final @NotNull ByteIterable serializedKey, final @NotNull Cursor cursor) {
        int comparison = serializer.compareClientId(serializedKey, cursor.getKey());
        while (comparison == ClientQueuePersistenceSerializer.CLIENT_ID_SAME_PREFIX) {
            comparison = compareNextClientId(serializedKey, cursor);
        }
        return comparison;
    }

    private int skipWithPacketId(
            final @NotNull ByteIterable serializedKey, final @NotNull Cursor cursor, int comparison) {
        while (comparison == ClientQueuePersistenceSerializer.CLIENT_ID_MATCH) {
            if (serializer.deserializePacketId(cursor.getValue()) == ClientQueuePersistenceSerializer.NO_PACKET_ID) {
                break;
            }
            comparison = compareNextClientId(serializedKey, cursor);
        }
        return comparison;
    }

    private int compareNextClientId(final @NotNull ByteIterable serializedClientId, final @NotNull Cursor cursor) {
        if (!cursor.getNext()) {
            return ClientQueuePersistenceSerializer.CLIENT_ID_NO_MATCH;
        }
        return serializer.compareClientId(serializedClientId, cursor.getKey());
    }

    /**
     * Move the cursor to every position of the client id order and nextEntrys the given iterationCallback.
     */
    private void iterateQueue(
            final Cursor cursor,
            final @NotNull Key key,
            final boolean skipWithId,
            final @NotNull IterationCallback iterationCallback) {

        final ByteIterable serializedKey = serializer.serializeKey(key);

        if (skipWithId) {
            final Long indexToLookTo = sharedSubLastPacketWithoutIdCache.getIfPresent(key.getQueueId());
            if (indexToLookTo != null) {
                final ByteIterable keyToSeek = serializer.serializeKey(key, indexToLookTo);
                if (cursor.getSearchKeyRange(keyToSeek) == null) {
                    return;
                }
            } else {
                if (cursor.getSearchKeyRange(serializedKey) == null) {
                    return;
                }
            }
        } else {
            if (cursor.getSearchKeyRange(serializedKey) == null) {
                return;
            }
        }

        int comparison = skipPrefix(serializedKey, cursor);
        if (skipWithId) {
            comparison = skipWithPacketId(serializedKey, cursor, comparison);
            if (key.isShared()) {
                incrementSharedSubscriptionIndexFirstMessageWithoutPacketId(key.getQueueId(),
                        serializer.deserializeIndex(cursor.getKey()));
            }
        }
        while (comparison == ClientQueuePersistenceSerializer.CLIENT_ID_MATCH) {
            if (!iterationCallback.nextEntry()) {
                return;
            }
            comparison = compareNextClientId(serializedKey, cursor);
        }
    }

    private interface IterationCallback {

        boolean nextEntry();
    }

    @VisibleForTesting
    public @NotNull ImmutableList<ClientQueueEntry> getAll(
            final @NotNull String queueId, final boolean shared, final int bucketIndex) {
        checkNotNull(queueId, "Queue id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);

        final Bucket bucket = buckets[bucketIndex];
        final ImmutableList.Builder<ClientQueueEntry> messageBuilder =
                bucket.getEnvironment().computeInExclusiveTransaction(txn -> {
                    try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                        final ImmutableList.Builder<ClientQueueEntry> entries = ImmutableList.builder();

                        iterateQueue(cursor, key, false, () -> {
                            final ByteIterable value = cursor.getValue();
                            final MessageWithID messageWithID = serializer.deserializeValue(value);
                            if (messageWithID instanceof PUBLISH) {
                                final PUBLISH publish = (PUBLISH) messageWithID;
                                publish.setPayload(payloadPersistence.get(publish.getPublishId()));
                            }
                            final boolean retained = serializer.deserializeRetained(value);
                            entries.add(new ClientQueueEntry(messageWithID, retained));
                            return true;
                        });
                        return entries;
                    }
                });
        return messageBuilder.build();
    }

    private @NotNull AtomicInteger getOrPutQueueSize(final @NotNull Key key, final int bucketIndex) {
        final Map<Key, AtomicInteger> queueSizeBucket = queueSizeBuckets.get(bucketIndex);
        return getOrPutQueueSizeFromBucket(key, queueSizeBucket);
    }

    private @NotNull AtomicInteger getOrPutRetainedQueueSize(final @NotNull Key key, final int bucketIndex) {
        final Map<Key, AtomicInteger> queueSizeBucket = retainedQueueSizeBuckets.get(bucketIndex);
        return getOrPutQueueSizeFromBucket(key, queueSizeBucket);
    }

    private @NotNull AtomicInteger getOrPutQueueSizeFromBucket(
            final @NotNull Key key, final @NotNull Map<Key, AtomicInteger> queueSizeBucket) {
        final AtomicInteger queueSize = queueSizeBucket.get(key);
        if (queueSize != null) {
            return queueSize;
        }
        final AtomicInteger newQueueSize = new AtomicInteger();
        queueSizeBucket.put(key, newQueueSize);
        return newQueueSize;
    }

    private @NotNull LinkedList<PublishWithRetained> getOrPutQos0Messages(
            final @NotNull Key key, final int bucketIndex) {

        final Map<Key, LinkedList<PublishWithRetained>> bucketMessages = qos0MessageBuckets.get(bucketIndex);
        LinkedList<PublishWithRetained> publishes = bucketMessages.get(key);
        if (publishes != null) {
            return publishes;
        }
        publishes = new LinkedList<>();
        bucketMessages.put(key, publishes);
        return publishes;
    }

    private int qos0Size(final @NotNull Key key, final int bucketIndex) {
        final Map<Key, LinkedList<PublishWithRetained>> bucketMessages = qos0MessageBuckets.get(bucketIndex);
        final LinkedList<PublishWithRetained> publishes = bucketMessages.get(key);
        if (publishes != null) {
            return publishes.size();
        }
        return 0;
    }

    private static class PublishWithRetained {

        private final @NotNull PUBLISH publish;
        private final boolean retained;

        private PublishWithRetained(final @NotNull PUBLISH publish, final boolean retained) {
            this.publish = publish;
            this.retained = retained;
        }
    }
}
