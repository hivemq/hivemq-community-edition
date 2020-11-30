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
package com.hivemq.persistence.local.xodus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.local.rocksdb.RocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.PublishUtil;
import com.hivemq.util.ThreadPreConditions;
import jetbrains.exodus.ExodusException;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class RetainedMessageRocksDBLocalPersistence extends RocksDBLocalPersistence implements RetainedMessageLocalPersistence {

    private static final Logger log = LoggerFactory.getLogger(RetainedMessageRocksDBLocalPersistence.class);

    public static final String PERSISTENCE_VERSION = "040500_R";
    @VisibleForTesting
    public final @NotNull PublishTopicTree[] topicTrees;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull RetainedMessageXodusSerializer serializer;
    private final @NotNull AtomicLong retainMessageCounter = new AtomicLong(0);

    @Inject
    public RetainedMessageRocksDBLocalPersistence(
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull PersistenceStartup persistenceStartup) {
        super(localPersistenceFileUtil,
                persistenceStartup,
                InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get(),
                InternalConfigurations.RETAINED_MESSAGE_MEMTABLE_SIZE_PORTION,
                InternalConfigurations.RETAINED_MESSAGE_BLOCK_CACHE_SIZE_PORTION,
                InternalConfigurations.RETAINED_MESSAGE_BLOCK_SIZE,
                InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get() == PersistenceType.FILE_NATIVE);

        this.payloadPersistence = payloadPersistence;
        this.serializer = new RetainedMessageXodusSerializer();
        final int bucketCount = getBucketCount();
        this.topicTrees = new PublishTopicTree[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            topicTrees[i] = new PublishTopicTree();
        }

    }

    @NotNull
    @Override
    protected String getName() {
        return PERSISTENCE_NAME;
    }

    @NotNull
    @Override
    protected String getVersion() {
        return PERSISTENCE_VERSION;
    }

    @NotNull
    @Override
    protected Options getOptions() {
        return new Options()
                .setCreateIfMissing(true)
                .setStatistics(new Statistics());
    }

    @NotNull
    @Override
    protected Logger getLogger() {
        return log;
    }

    @PostConstruct
    protected void postConstruct() {
        super.postConstruct();
    }

    @Override
    public void init() {

        try {
            for (int i = 0; i < buckets.length; i++) {
                final RocksDB bucket = buckets[i];
                try (final RocksIterator iterator = bucket.newIterator()) {
                    iterator.seekToFirst();
                    while (iterator.isValid()) {
                        final RetainedMessage message = serializer.deserializeValue(iterator.value());
                        final Long payloadId = message.getPublishId();
                        if (payloadId != null) {
                            payloadPersistence.incrementReferenceCounterOnBootstrap(payloadId);
                        }
                        final String topic = serializer.deserializeKey(iterator.key());
                        topicTrees[i].add(topic);
                        retainMessageCounter.incrementAndGet();
                        iterator.next();
                    }
                }
            }

        } catch (final ExodusException e) {
            log.error("An error occurred while preparing the Retained Message persistence.");
            log.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
    }

    @Override
    public void bootstrapPayloads() {
        try {
            for (final RocksDB bucket : buckets) {
                try (final RocksIterator iterator = bucket.newIterator()) {
                    iterator.seekToFirst();
                    while (iterator.isValid()) {
                        final RetainedMessage message = serializer.deserializeValue(iterator.value());
                        final long payloadId = message.getPublishId();
                        payloadPersistence.incrementReferenceCounterOnBootstrap(payloadId);
                        iterator.next();
                    }
                }
            }

        } catch (final ExodusException e) {
            log.error("An error occurred while preparing the Retained Message persistence.");
            log.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
    }

    @Override
    public void clear(final int bucketIndex) {

        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        topicTrees[bucketIndex] = new PublishTopicTree();

        final RocksDB bucket = buckets[bucketIndex];
        try (final WriteBatch writeBatch = new WriteBatch();
             final WriteOptions options = new WriteOptions();
             final RocksIterator iterator = bucket.newIterator()) {
            iterator.seekToFirst();
            while (iterator.isValid()) {
                final RetainedMessage message = serializer.deserializeValue(iterator.value());
                payloadPersistence.decrementReferenceCounter(message.getPublishId());
                retainMessageCounter.decrementAndGet();
                writeBatch.delete(iterator.key());
                iterator.next();
            }
            bucket.write(options, writeBatch);
        } catch (final Exception e) {
            log.error("An error occurred while clearing the retained message persistence.");
            log.debug("Original Exception:", e);
        }
    }

    @Override
    public long size() {
        return retainMessageCounter.get();
    }

    @Override
    public void remove(@NotNull final String topic, final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        final RocksDB bucket = buckets[bucketIndex];

        try {
            final byte[] key = serializer.serializeKey(topic);
            final byte[] removed = bucket.get(key);
            if (removed == null) {
                log.trace("Removing retained message for topic {} (no message was stored previously)", topic);
                return;
            }

            final RetainedMessage message = serializer.deserializeValue(removed);

            log.trace("Removing retained message for topic {}", topic);
            bucket.delete(key);
            topicTrees[bucketIndex].remove(topic);
            payloadPersistence.decrementReferenceCounter(message.getPublishId());
            retainMessageCounter.decrementAndGet();

        } catch (final Exception e) {
            log.error("An error occurred while removing a retained message.");
            log.debug("Original Exception:", e);
        }

    }

    @Nullable
    @Override
    public RetainedMessage get(@NotNull final String topic, final int bucketIndex) {
        try {
            return tryGetLocally(topic, 0, bucketIndex);
        } catch (final Exception e) {
            log.error("An error occurred while getting a retained message.");
            log.debug("Original Exception:", e);
            return null;
        }
    }

    private RetainedMessage tryGetLocally(@NotNull final String topic, final int retry, final int bucketIndex)
            throws Exception {
        checkNotNull(topic, "Topic must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final RocksDB bucket = buckets[bucketIndex];

        final byte[] messageAsBytes = bucket.get(serializer.serializeKey(topic));
        if (messageAsBytes != null) {
            final RetainedMessage message = serializer.deserializeValue(messageAsBytes);
            final byte[] payload = payloadPersistence.getPayloadOrNull(message.getPublishId());
            if (payload == null) {
                // In case the payload was just deleted, we return the new retained message for this topic (or null if it was removed).
                if (retry < 100) {
                    return tryGetLocally(topic, retry + 1, bucketIndex);
                } else {
                    log.warn("No payload was found for the retained message on topic {}.", topic);
                    return null;
                }
            }

            if (PublishUtil.checkExpiry(message.getTimestamp(), message.getMessageExpiryInterval())) {
                return null;
            }
            message.setMessage(payload);
            return message;
        }
        return null;
    }

    @Override
    public void put(
            @NotNull final RetainedMessage retainedMessage, @NotNull final String topic, final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        checkNotNull(retainedMessage, "Retained message must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final RocksDB bucket = buckets[bucketIndex];

        try {
            final byte[] serializedTopic = serializer.serializeKey(topic);
            final byte[] valueAsBytes = bucket.get(serializedTopic);
            if (valueAsBytes != null) {
                final RetainedMessage retainedMessageFromStore = serializer.deserializeValue(valueAsBytes);
                log.trace("Replacing retained message for topic {}", topic);
                bucket.put(serializedTopic, serializer.serializeValue(retainedMessage));
                // The previous retained message is replaced, so we have to decrement the reference count.
                payloadPersistence.decrementReferenceCounter(retainedMessageFromStore.getPublishId());
            } else {
                log.trace("Creating new retained message for topic {}", topic);
                bucket.put(serializedTopic, serializer.serializeValue(retainedMessage));
                topicTrees[bucketIndex].add(topic);
                //persist needs increment.
                retainMessageCounter.incrementAndGet();
            }
        } catch (
                final Exception e) {
            log.error("An error occurred while persisting a retained message.");
            log.debug("Original Exception:", e);
        }

    }

    @NotNull
    @Override
    public Set<String> getAllTopics(
            @NotNull final String subscription, final int bucketId) {
        checkArgument(bucketId >= 0 && bucketId < getBucketCount(), "Bucket index out of range");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        return topicTrees[bucketId].get(subscription);
    }

    @Override
    public void cleanUp(final int bucketId) {
        checkArgument(bucketId >= 0 && bucketId < getBucketCount(), "Bucket index out of range");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        if (stopped.get()) {
            return;
        }

        final RocksDB bucket = buckets[bucketId];
        final PublishTopicTree topicTree = topicTrees[bucketId];

        try (final RocksIterator iterator = bucket.newIterator();
             final WriteBatch writeBatch = new WriteBatch();
             final WriteOptions options = new WriteOptions()) {
            iterator.seekToFirst();
            while (iterator.isValid()) {
                final String topic = serializer.deserializeKey(iterator.key());
                final RetainedMessage message = serializer.deserializeValue(iterator.value());
                if (PublishUtil.checkExpiry(message.getTimestamp(), message.getMessageExpiryInterval())) {
                    writeBatch.delete(iterator.key());
                    payloadPersistence.decrementReferenceCounter(message.getPublishId());
                    retainMessageCounter.decrementAndGet();
                    topicTree.remove(topic);
                }
                iterator.next();
            }
            bucket.write(options, writeBatch);
        } catch (final Exception e) {
            log.error("An error occurred while cleaning up retained messages.");
            log.debug("Original Exception:", e);
        }
    }

    public @NotNull BucketChunkResult<Map<String, @NotNull RetainedMessage>> getAllRetainedMessagesChunk(final int bucketIndex,
                                                                                                         final @Nullable String lastTopic,
                                                                                                         final int maxMemory) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        final RocksDB bucket = buckets[bucketIndex];

        try (final RocksIterator iterator = bucket.newIterator()) {
            if (lastTopic == null) {
                iterator.seekToFirst();
            } else {
                iterator.seek(serializer.serializeKey(lastTopic));
                // we already have this one, lets look for the next
                if (iterator.isValid()) {
                    final String deserializedTopic = serializer.deserializeKey(iterator.key());
                    // we double check that in between calls no messages were removed
                    if (deserializedTopic.equals(lastTopic)) {
                        iterator.next();
                    }
                }
            }

            int usedMemory = 0;
            final ImmutableMap.Builder<String, RetainedMessage> retrievedMessages = ImmutableMap.builder();
            String lastFoundTopic = null;

            // we iterate either until the end of the persistence or until the maximum requested messages are found
            while (iterator.isValid() && usedMemory < maxMemory) {

                final String deserializedTopic = serializer.deserializeKey(iterator.key());
                final RetainedMessage deserializedMessage = serializer.deserializeValue(iterator.value());

                // ignore messages with exceeded message expiry interval
                if (PublishUtil.checkExpiry(deserializedMessage.getTimestamp(), deserializedMessage.getMessageExpiryInterval())) {
                    iterator.next();
                    continue;
                }

                final byte[] payload = payloadPersistence.getPayloadOrNull(deserializedMessage.getPublishId());

                // ignore messages with no payload and log a warning for the fact
                if (payload == null) {
                    log.warn("Could not dereference payload for retained message on topic \"{}\" with payload id \"{}\".",
                            deserializedTopic, deserializedMessage.getPublishId());
                    iterator.next();
                    continue;
                }
                deserializedMessage.setMessage(payload);

                lastFoundTopic = deserializedTopic;
                usedMemory += deserializedMessage.getEstimatedSizeInMemory();

                retrievedMessages.put(lastFoundTopic, deserializedMessage);
                iterator.next();
            }

            // if the iterator is not valid any more we know that there is nothing more to get
            return new BucketChunkResult<>(retrievedMessages.build(), !iterator.isValid(), lastFoundTopic, bucketIndex);
        }
    }

    @Override
    public void iterate(final @NotNull ItemCallback callback) {

        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        for (final RocksDB bucket : buckets) {
            try (final RocksIterator iterator = bucket.newIterator()) {
                iterator.seekToFirst();
                while (iterator.isValid()) {
                    final RetainedMessage message = serializer.deserializeValue(iterator.value());
                    final String topic = serializer.deserializeKey(iterator.key());
                    callback.onItem(topic, message);
                    iterator.next();
                }
            }
        }
    }

}
