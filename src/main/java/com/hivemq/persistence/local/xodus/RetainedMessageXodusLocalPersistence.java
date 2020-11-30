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
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.PublishUtil;
import com.hivemq.util.ThreadPreConditions;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.persistence.local.xodus.XodusUtils.*;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
@LazySingleton
public class RetainedMessageXodusLocalPersistence extends XodusLocalPersistence implements RetainedMessageLocalPersistence {

    private static final Logger log = LoggerFactory.getLogger(
            RetainedMessageXodusLocalPersistence.class);

    public static final String PERSISTENCE_VERSION = "040500";

    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull RetainedMessageXodusSerializer serializer;

    private final AtomicLong retainMessageCounter = new AtomicLong(0);

    @VisibleForTesting
    final ConcurrentHashMap<Integer, PublishTopicTree> topicTrees = new ConcurrentHashMap<>();

    @Inject
    public RetainedMessageXodusLocalPersistence(final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                                final @NotNull PublishPayloadPersistence payloadPersistence,
                                                final @NotNull EnvironmentUtil environmentUtil,
                                                final @NotNull PersistenceStartup persistenceStartup) {

        super(environmentUtil,
                localPersistenceFileUtil,
                persistenceStartup,
                InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get(),
                //check if enabled
                InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get().equals(PersistenceType.FILE));
        this.payloadPersistence = payloadPersistence;
        this.serializer = new RetainedMessageXodusSerializer();
        for (int i = 0; i < bucketCount; i++) {
            topicTrees.put(i, new PublishTopicTree());
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
    protected StoreConfig getStoreConfig() {
        return StoreConfig.WITHOUT_DUPLICATES;
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
                final Bucket bucket = buckets[i];
                final int bucketIndex = i;
                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                        while (cursor.getNext()) {
                            final RetainedMessage message = serializer.deserializeValue(byteIterableToBytes(cursor.getValue()));
                            payloadPersistence.incrementReferenceCounterOnBootstrap(message.getPublishId());
                            final String topic = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));
                            topicTrees.get(bucketIndex).add(topic);
                            retainMessageCounter.incrementAndGet();
                        }
                    }
                });
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
            for (final Bucket bucket : buckets) {
                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                        while (cursor.getNext()) {
                            final RetainedMessage message = serializer.deserializeValue(byteIterableToBytes(cursor.getValue()));
                            final long payloadId = message.getPublishId();
                            payloadPersistence.incrementReferenceCounterOnBootstrap(payloadId);
                        }
                    }
                });
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
        topicTrees.put(bucketIndex, new PublishTopicTree());

        final Bucket bucket = buckets[bucketIndex];

        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            final Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                final RetainedMessage message = serializer.deserializeValue(byteIterableToBytes(cursor.getValue()));
                payloadPersistence.decrementReferenceCounter(message.getPublishId());
                retainMessageCounter.decrementAndGet();
                cursor.deleteCurrent();
            }
        });
    }

    @Override
    public long size() {
        return retainMessageCounter.get();
    }

    @Override
    public void remove(@NotNull final String topic, final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        final Bucket bucket = buckets[bucketIndex];

        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            final ByteIterable key = stringToByteIterable(topic);
            final ByteIterable byteIterable = bucket.getStore().get(txn, bytesToByteIterable(serializer.serializeKey(topic)));
            if (byteIterable == null) {
                log.trace("Removing retained message for topic {} (no message was stored previously)", topic);
                return;
            }

            final RetainedMessage message = serializer.deserializeValue(byteIterableToBytes(byteIterable));

            log.trace("Removing retained message for topic {}", topic);
            bucket.getStore().delete(txn, key);
            topicTrees.get(bucketIndex).remove(topic);
            payloadPersistence.decrementReferenceCounter(message.getPublishId());
            retainMessageCounter.decrementAndGet();
        });

    }

    @Nullable
    @Override
    public RetainedMessage get(@NotNull final String topic, final int bucketIndex) {
        return tryGetLocally(topic, 0, bucketIndex);
    }

    private RetainedMessage tryGetLocally(@NotNull final String topic, final int retry, final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Bucket bucket = buckets[bucketIndex];
        final AtomicBoolean payloadIdExpired = new AtomicBoolean(false);

        final RetainedMessage retainedMessage = bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            final ByteIterable byteIterable = bucket.getStore().get(txn, bytesToByteIterable(serializer.serializeKey(topic)));
            if (byteIterable != null) {

                final RetainedMessage message = serializer.deserializeValue(byteIterableToBytes(byteIterable));

                final byte[] payload = payloadPersistence.getPayloadOrNull(message.getPublishId());
                if (payload == null) {
                    // In case the payload was just deleted, we return the new retained message for this topic (or null if it was removed).
                    payloadIdExpired.set(true);
                    return null;
                }

                if (PublishUtil.checkExpiry(message.getTimestamp(), message.getMessageExpiryInterval())) {
                    return null;
                }
                message.setMessage(payload);
                return message;
            }

            //Not found :(
            return null;
        });

        if (payloadIdExpired.get()) {
            if (retry < 100) {
                return tryGetLocally(topic, retry + 1, bucketIndex);
            } else {
                log.warn("No payload was found for the retained message on topic {}.", topic);
                return null;
            }
        }
        return retainedMessage;
    }

    @Override
    public void put(@NotNull final RetainedMessage retainedMessage, @NotNull final String topic, final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        checkNotNull(retainedMessage, "Retained message must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Bucket bucket = buckets[bucketIndex];

        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                final ByteIterable byteIterable = cursor.getSearchKey(bytesToByteIterable(serializer.serializeKey(topic)));
                if (byteIterable != null) {
                    final RetainedMessage retainedMessageFromStore = serializer.deserializeValue(byteIterableToBytes(cursor.getValue()));
                    log.trace("Replacing retained message for topic {}", topic);
                    bucket.getStore().put(txn, bytesToByteIterable(serializer.serializeKey(topic)), bytesToByteIterable(serializer.serializeValue(retainedMessage)));
                    // The previous retained message is replaced, so we have to decrement the reference count.
                    payloadPersistence.decrementReferenceCounter(retainedMessageFromStore.getPublishId());
                } else {
                    bucket.getStore().put(txn, bytesToByteIterable(serializer.serializeKey(topic)), bytesToByteIterable(serializer.serializeValue(retainedMessage)));
                    log.trace("Creating new retained message for topic {}", topic);
                    //persist needs increment.
                    retainMessageCounter.incrementAndGet();
                    topicTrees.get(bucketIndex).add(topic);
                }
            }
        });
    }

    @NotNull
    @Override
    public Set<String> getAllTopics(@NotNull final String subscription, final int bucketId) {
        checkArgument(bucketId >= 0 && bucketId < bucketCount, "Bucket index out of range");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        return topicTrees.get(bucketId).get(subscription);
    }

    @Override
    public void cleanUp(final int bucketId) {
        checkArgument(bucketId >= 0 && bucketId < bucketCount, "Bucket index out of range");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        if (stopped.get()) {
            return;
        }

        final Bucket bucket = buckets[bucketId];
        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                if (cursor.getNext()) {
                    do {
                        final RetainedMessage message = serializer.deserializeValue(byteIterableToBytes(cursor.getValue()));
                        if (PublishUtil.checkExpiry(message.getTimestamp(), message.getMessageExpiryInterval())) {
                            cursor.deleteCurrent();
                            payloadPersistence.decrementReferenceCounter(message.getPublishId());
                            retainMessageCounter.decrementAndGet();
                            topicTrees.get(bucketId).remove(serializer.deserializeKey(byteIterableToBytes(cursor.getKey())));
                        }

                    } while (cursor.getNext());
                }
            }
        });
    }

    @Override
    public @NotNull BucketChunkResult<Map<String, @NotNull RetainedMessage>> getAllRetainedMessagesChunk(final int bucketIndex,
                                                                                                         final @Nullable String lastTopic,
                                                                                                         final int maxMemory) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        final Bucket bucket = buckets[bucketIndex];

        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            int usedMemory = 0;
            final ImmutableMap.Builder<String, RetainedMessage> retrievedMessages = ImmutableMap.builder();
            String lastFoundTopic = lastTopic;
            boolean hasNext = true;

            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                if (lastTopic == null) {
                    hasNext = cursor.getNext();
                } else {
                    final ByteIterable lastTopicKey = bytesToByteIterable(serializer.serializeKey(lastFoundTopic));
                    final ByteIterable foundKey = cursor.getSearchKeyRange(lastTopicKey);

                    if (foundKey == null) {
                        return new BucketChunkResult<>(retrievedMessages.build(), true, lastTopic, bucketIndex);
                    }

                    // we already have this one, lets look for the next
                    if (cursor.getKey().equals(lastTopicKey)) {
                        //jump to the next key
                        hasNext = cursor.getNext();
                    }
                }

                // we iterate either until the end of the persistence or until the maximum requested messages are found
                while (hasNext && usedMemory < maxMemory) {

                    final String deserializedTopic = byteIterableToString(cursor.getKey());
                    final RetainedMessage deserializedMessage = serializer.deserializeValue(byteIterableToBytes(cursor.getValue()));

                    // ignore messages with exceeded message expiry interval
                    if (PublishUtil.checkExpiry(deserializedMessage.getTimestamp(), deserializedMessage.getMessageExpiryInterval())) {
                        hasNext = cursor.getNext();
                        continue;
                    }

                    final byte[] payload = payloadPersistence.getPayloadOrNull(deserializedMessage.getPublishId());

                    // ignore messages with no payload and log a warning for the fact
                    if (payload == null) {
                        log.warn("Could not dereference payload for retained message on topic \"{}\" with payload id \"{}\".",
                                deserializedTopic, deserializedMessage.getPublishId());
                        hasNext = cursor.getNext();
                        continue;
                    }
                    deserializedMessage.setMessage(payload);

                    lastFoundTopic = deserializedTopic;
                    usedMemory += deserializedMessage.getEstimatedSizeInMemory();

                    retrievedMessages.put(lastFoundTopic, deserializedMessage);
                    hasNext = cursor.getNext();
                }
            }
            // if the cursor has no next value any more we know that there is nothing more to get
            return new BucketChunkResult<>(retrievedMessages.build(), !hasNext, lastFoundTopic, bucketIndex);
        });
    }

    @Override
    public void iterate(final @NotNull RetainedMessageLocalPersistence.ItemCallback callback) {

        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        for (final Bucket bucket : buckets) {
            bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                    while (cursor.getNext()) {
                        final RetainedMessage message = serializer.deserializeValue(byteIterableToBytes(cursor.getValue()));
                        final String topic = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));
                        callback.onItem(topic, message);
                    }
                }
            });
        }
    }

}
