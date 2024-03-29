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
import com.hivemq.persistence.local.DeltaCounter;
import com.hivemq.persistence.local.rocksdb.RocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.ThreadPreConditions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.persistence.local.xodus.RetainedMessageSerializer.deserializeKey;
import static com.hivemq.persistence.local.xodus.RetainedMessageSerializer.deserializeValue;
import static com.hivemq.persistence.local.xodus.RetainedMessageSerializer.serializeKey;
import static com.hivemq.persistence.local.xodus.RetainedMessageSerializer.serializeValue;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

@LazySingleton
public class RetainedMessageRocksDBLocalPersistence extends RocksDBLocalPersistence
        implements RetainedMessageLocalPersistence {

    private static final Logger log = LoggerFactory.getLogger(RetainedMessageRocksDBLocalPersistence.class);

    public static final String PERSISTENCE_VERSION = "040500_R";
    @VisibleForTesting
    public final @NotNull PublishTopicTree[] topicTrees;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
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
                InternalConfigurations.RETAINED_MESSAGE_BLOCK_SIZE_BYTES,
                InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get() == PersistenceType.FILE_NATIVE);

        this.payloadPersistence = payloadPersistence;
        final int bucketCount = getBucketCount();
        this.topicTrees = new PublishTopicTree[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            topicTrees[i] = new PublishTopicTree();
        }
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
    protected @NotNull Logger getLogger() {
        return log;
    }

    @PostConstruct
    protected void postConstruct() {
        super.postConstruct();
    }

    @Override
    public void init() {
        try {
            final DeltaCounter deltaCounter = DeltaCounter.finishWith(retainMessageCounter::addAndGet);

            for (int i = 0; i < buckets.length; i++) {

                final RocksDB bucket = buckets[i];
                final PublishTopicTree publishTopicTree = topicTrees[i];

                try (final RocksIterator iterator = bucket.newIterator()) {
                    iterator.seekToFirst();
                    while (iterator.isValid()) {
                        final RetainedMessage message = deserializeValue(iterator.value());
                        payloadPersistence.incrementReferenceCounterOnBootstrap(message.getPublishId());
                        final String topic = deserializeKey(iterator.key());
                        publishTopicTree.add(topic);
                        deltaCounter.increment();
                        iterator.next();
                    }
                }
            }

            deltaCounter.run();

        } catch (final Exception e) {
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

            final DeltaCounter retainMessageDelta = DeltaCounter.finishWith(retainMessageCounter::addAndGet);
            iterator.seekToFirst();

            while (iterator.isValid()) {
                final RetainedMessage message = deserializeValue(iterator.value());
                payloadPersistence.decrementReferenceCounter(message.getPublishId());
                retainMessageDelta.decrement();
                writeBatch.delete(iterator.key());
                iterator.next();
            }
            bucket.write(options, writeBatch);

            retainMessageDelta.run();

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
    public void remove(final @NotNull String topic, final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        final RocksDB bucket = buckets[bucketIndex];

        try {
            final byte[] key = serializeKey(topic);
            final byte[] removed = bucket.get(key);
            if (removed == null) {
                log.trace("Removing retained message for topic {} (no message was stored previously)", topic);
                return;
            }

            final RetainedMessage message = deserializeValue(removed);

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

    @Override
    public @Nullable RetainedMessage get(final @NotNull String topic, final int bucketIndex) {
        try {
            checkNotNull(topic, "Topic must not be null");
            ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

            final RocksDB bucket = buckets[bucketIndex];

            final byte[] messageAsBytes = bucket.get(serializeKey(topic));
            if (messageAsBytes == null) {
                return null;
            }
            final RetainedMessage message = deserializeValue(messageAsBytes);
            if (message.hasExpired()) {
                return null;
            }

            final byte[] payload = payloadPersistence.get(message.getPublishId());
            if (payload == null) {
                log.warn("No payload was found for the retained message on topic {}.", topic);
                return null;
            }
            message.setMessage(payload);
            return message;
        } catch (final Exception e) {
            log.error("An error occurred while getting a retained message.");
            log.debug("Original Exception:", e);
            return null;
        }
    }

    @Override
    public void put(
            final @NotNull RetainedMessage retainedMessage, final @NotNull String topic, final int bucketIndex) {

        checkNotNull(topic, "Topic must not be null");
        checkNotNull(retainedMessage, "Retained message must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final RocksDB bucket = buckets[bucketIndex];

        try {
            final byte[] serializedTopic = serializeKey(topic);
            final byte[] valueAsBytes = bucket.get(serializedTopic);
            if (valueAsBytes != null) {
                final RetainedMessage retainedMessageFromStore = deserializeValue(valueAsBytes);
                log.trace("Replacing retained message for topic {}", topic);
                bucket.put(serializedTopic, serializeValue(retainedMessage));
                // The previous retained message is replaced, so we have to decrement the reference count.
                payloadPersistence.decrementReferenceCounter(retainedMessageFromStore.getPublishId());
            } else {
                log.trace("Creating new retained message for topic {}", topic);
                bucket.put(serializedTopic, serializeValue(retainedMessage));
                topicTrees[bucketIndex].add(topic);
                //persist needs increment.
                retainMessageCounter.incrementAndGet();
            }
            payloadPersistence.add(retainedMessage.getMessage(), retainedMessage.getPublishId());
        } catch (final Exception e) {
            log.error("An error occurred while persisting a retained message.");
            log.debug("Original Exception:", e);
        }

    }

    @Override
    public @NotNull Set<String> getAllTopics(
            final @NotNull String subscription, final int bucketId) {

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
                final String topic = deserializeKey(iterator.key());
                final RetainedMessage message = deserializeValue(iterator.value());
                if (message.hasExpired()) {
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

    public @NotNull BucketChunkResult<Map<String, @NotNull RetainedMessage>> getAllRetainedMessagesChunk(
            final int bucketIndex, final @Nullable String lastTopic, final int maxMemory) {

        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        final RocksDB bucket = buckets[bucketIndex];

        try (final RocksIterator iterator = bucket.newIterator()) {
            if (lastTopic == null) {
                iterator.seekToFirst();
            } else {
                iterator.seek(serializeKey(lastTopic));
                // we already have this one, lets look for the next
                if (iterator.isValid()) {
                    final String deserializedTopic = deserializeKey(iterator.key());
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

                final String deserializedTopic = deserializeKey(iterator.key());
                final RetainedMessage deserializedMessage = deserializeValue(iterator.value());

                // ignore messages with exceeded message expiry interval
                if (deserializedMessage.hasExpired()) {
                    iterator.next();
                    continue;
                }

                final byte[] payload = payloadPersistence.get(deserializedMessage.getPublishId());

                // ignore messages with no payload and log a warning for the fact
                if (payload == null) {
                    log.warn(
                            "Could not dereference payload for retained message on topic \"{}\" with payload id \"{}\".",
                            deserializedTopic,
                            deserializedMessage.getPublishId());
                    iterator.next();
                    continue;
                }
                deserializedMessage.setMessage(payload);

                lastFoundTopic = deserializedTopic;
                usedMemory += deserializedMessage.getEstimatedSizeInMemory();

                retrievedMessages.put(lastFoundTopic, deserializedMessage);
                iterator.next();
            }

            // if the iterator is not valid anymore we know that there is nothing more to get
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
                    final RetainedMessage message = deserializeValue(iterator.value());
                    final String topic = deserializeKey(iterator.key());
                    callback.onItem(topic, message);
                    iterator.next();
                }
            }
        }
    }
}
