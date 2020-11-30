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
package com.hivemq.migration.persistence.legacy;

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.persistence.local.xodus.XodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.ThreadPreConditions;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.migration.persistence.legacy.serializer.RetainedMessageDeserializer_4_4.*;
import static com.hivemq.persistence.local.xodus.XodusUtils.byteIterableToBytes;
import static com.hivemq.persistence.local.xodus.XodusUtils.bytesToByteIterable;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * @author Florian Limpöck
 * @since 4.5.0
 */
@LazySingleton
public class RetainedMessageXodusLocalPersistence_4_4 extends XodusLocalPersistence {

    private static final Logger log = LoggerFactory.getLogger(RetainedMessageXodusLocalPersistence_4_4.class);

    public static final String PERSISTENCE_NAME = "retained_messages";
    public static final String PERSISTENCE_VERSION = "040000";

    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final AtomicLong retainMessageCounter = new AtomicLong(0);

    @Inject
    public RetainedMessageXodusLocalPersistence_4_4(
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull EnvironmentUtil environmentUtil,
            final @NotNull PersistenceStartup persistenceStartup) {
        super(environmentUtil,
                localPersistenceFileUtil,
                persistenceStartup,
                InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get(),
                //check if enabled
                false);
        this.payloadPersistence = payloadPersistence;
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
            for (final Bucket bucket : buckets) {
                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                        while (cursor.getNext()) {
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

    public long size() {
        return retainMessageCounter.get();
    }

    public void iterate(final @NotNull RetainedMessageItemCallback_4_4 callback) {

        for (final Bucket bucket : buckets) {
            bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                    while (cursor.getNext()) {
                        final RetainedMessage message =
                                deserializeValue(byteIterableToBytes(cursor.getValue()));
                        final String topic = deserializeKey(byteIterableToBytes(cursor.getKey()));
                        callback.onItem(topic, message);
                    }
                }
            });
        }
    }

    public void put(
            @NotNull final RetainedMessage retainedMessage,
            @NotNull final String topic,
            final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        checkNotNull(retainedMessage, "Retained message must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Bucket bucket = buckets[bucketIndex];

        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                final ByteIterable byteIterable = cursor.getSearchKey(bytesToByteIterable(serializeKey(topic)));
                if (byteIterable != null) {
                    final RetainedMessage retainedMessageFromStore = deserializeValue(byteIterableToBytes(cursor.getValue()));
                    log.trace("Replacing retained message for topic {}", topic);
                    bucket.getStore().put(txn, bytesToByteIterable(serializeKey(topic)), bytesToByteIterable(serializeValue(retainedMessage)));
                    // The previous retained message is replaced, so we have to decrement the reference count.
                    payloadPersistence.decrementReferenceCounter(retainedMessageFromStore.getPublishId());
                } else {
                    bucket.getStore().put(txn, bytesToByteIterable(serializeKey(topic)), bytesToByteIterable(serializeValue(retainedMessage)));
                    log.trace("Creating new retained message for topic {}", topic);
                    //persist needs increment.
                    retainMessageCounter.incrementAndGet();
                }
            }
        });
    }

}
