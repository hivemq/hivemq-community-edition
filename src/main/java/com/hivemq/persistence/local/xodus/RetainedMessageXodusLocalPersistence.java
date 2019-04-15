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

package com.hivemq.persistence.local.xodus;

import com.google.common.collect.ImmutableSet;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.persistence.PersistenceFilter;
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
import jetbrains.exodus.env.TransactionalComputable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Set;
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

    private static final Logger log = LoggerFactory.getLogger(RetainedMessageXodusLocalPersistence.class);

    private static final String PERSISTENCE_NAME = "retained_messages";
    private static final String PERSISTENCE_VERSION = "040000";

    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull RetainedMessageXodusSerializer serializer;

    private final AtomicLong retainMessageCounter = new AtomicLong(0);

    @Inject
    public RetainedMessageXodusLocalPersistence(final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                                final @NotNull PublishPayloadPersistence payloadPersistence,
                                                final @NotNull EnvironmentUtil environmentUtil,
                                                final @NotNull PersistenceStartup persistenceStartup) {

        super(environmentUtil, localPersistenceFileUtil, persistenceStartup, InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get());

        this.payloadPersistence = payloadPersistence;
        this.serializer = new RetainedMessageXodusSerializer();

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

    protected void init() {

        try {
            for (final Bucket bucket : buckets) {
                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                        while (cursor.getNext()) {
                            final RetainedMessage message = serializer.deserializeValue(byteIterableToBytes(cursor.getValue()));
                            final Long payloadId = message.getPayloadId();
                            if (payloadId != null) {
                                payloadPersistence.incrementReferenceCounterOnBootstrap(payloadId);
                            }

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
    public void clear(final int bucketIndex) {

        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Bucket bucket = buckets[bucketIndex];

        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            final Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {

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
            payloadPersistence.decrementReferenceCounter(message.getPayloadId());
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

                final Long payloadId = message.getPayloadId();

                final byte[] payload = payloadPersistence.getPayloadOrNull(payloadId);
                if (payload == null) {
                    // In case the payload was just deleted, we return the new retained message for this topic (or null if it was removed).
                    payloadIdExpired.set(true);
                    return null;
                }

                if (PublishUtil.isExpired(message.getTimestamp(), message.getMessageExpiryInterval())) {
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
                    payloadPersistence.decrementReferenceCounter(retainedMessageFromStore.getPayloadId());
                } else {
                    bucket.getStore().put(txn, bytesToByteIterable(serializer.serializeKey(topic)), bytesToByteIterable(serializer.serializeValue(retainedMessage)));
                    log.trace("Creating new retained message for topic {}", topic);
                    //persist needs increment.
                    retainMessageCounter.incrementAndGet();
                }
            }
        });
    }

    @NotNull
    @Override
    public Set<String> getAllTopics(@NotNull final PersistenceFilter filter, final int bucketId) {
        checkArgument(bucketId >= 0 && bucketId < bucketCount, "Bucket index out of range");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Bucket bucket = buckets[bucketId];
        return bucket.getEnvironment().computeInReadonlyTransaction((TransactionalComputable<Set<String>>) txn -> {

            final ImmutableSet.Builder<String> builder = ImmutableSet.builder();

            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                if (cursor.getNext()) {
                    do {
                        final String topic = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));
                        if (filter.match(topic)) {
                            builder.add(topic);
                        }
                    } while (cursor.getNext());
                }
            }
            return builder.build();
        });
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
                        if (PublishUtil.isExpired(message.getTimestamp(), message.getMessageExpiryInterval())) {
                            cursor.deleteCurrent();
                            payloadPersistence.decrementReferenceCounter(message.getPayloadId());
                            retainMessageCounter.decrementAndGet();
                        }

                    } while (cursor.getNext());
                }
            }
        });
    }

}
