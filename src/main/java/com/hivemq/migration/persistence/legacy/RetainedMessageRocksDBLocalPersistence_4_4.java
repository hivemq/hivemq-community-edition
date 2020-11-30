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
import com.hivemq.persistence.local.rocksdb.RocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.ThreadPreConditions;
import jetbrains.exodus.ExodusException;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.rocksdb.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.migration.persistence.legacy.serializer.RetainedMessageDeserializer_4_4.*;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * @author Florian Limpöck
 * @since 4.5.0
 */
@LazySingleton
public class RetainedMessageRocksDBLocalPersistence_4_4 extends RocksDBLocalPersistence {

    private static final Logger log = LoggerFactory.getLogger(RetainedMessageRocksDBLocalPersistence_4_4.class);

    public static final String PERSISTENCE_NAME = "retained_messages";
    public static final String PERSISTENCE_VERSION = "040000_R";

    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull AtomicLong retainMessageCounter = new AtomicLong(0);

    @Inject
    public RetainedMessageRocksDBLocalPersistence_4_4(final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                                      final @NotNull PublishPayloadPersistence payloadPersistence,
                                                      final @NotNull PersistenceStartup persistenceStartup) {

        super(localPersistenceFileUtil,
                persistenceStartup,
                InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get(),
                InternalConfigurations.RETAINED_MESSAGE_MEMTABLE_SIZE_PORTION,
                InternalConfigurations.RETAINED_MESSAGE_BLOCK_CACHE_SIZE_PORTION,
                InternalConfigurations.RETAINED_MESSAGE_BLOCK_SIZE,
                false);

        this.payloadPersistence = payloadPersistence;
        final int bucketCount = getBucketCount();
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
            for (final RocksDB bucket : buckets) {
                try (final RocksIterator iterator = bucket.newIterator()) {
                    iterator.seekToFirst();
                    while (iterator.isValid()) {
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

    public void put(@NotNull final RetainedMessage retainedMessage, @NotNull final String topic, final int bucketIndex) {
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
                //persist needs increment.
                retainMessageCounter.incrementAndGet();
            }
        } catch (final Exception e) {
            log.error("An error occurred while persisting a retained message.");
            log.debug("Original Exception:", e);
        }
    }

    public long size() {
        return retainMessageCounter.get();
    }

    public void iterate(final @NotNull RetainedMessageItemCallback_4_4 callback) {
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
