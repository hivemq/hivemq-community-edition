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

import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.rocksdb.RocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadRocksDBLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.migration.persistence.legacy.serializer.PublishPayloadRocksDBSerializer_4_4.serializeKey;

/**
 * @author Florian Limpöck
 * @author Lukas Brandl
 * @since 4.5.0
 */
@LazySingleton
public class PublishPayloadRocksDBLocalPersistence_4_4 extends RocksDBLocalPersistence implements PublishPayloadLocalPersistence_4_4 {

    private static final Logger log = LoggerFactory.getLogger(PublishPayloadRocksDBLocalPersistence.class);

    public static final String PERSISTENCE_VERSION = "040000_R";

    @Inject
    public PublishPayloadRocksDBLocalPersistence_4_4(final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                                     final @NotNull PersistenceStartup persistenceStartup) {
        super(localPersistenceFileUtil,
                persistenceStartup,
                InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.get(),
                InternalConfigurations.PAYLOAD_PERSISTENCE_MEMTABLE_SIZE_PORTION.get(),
                InternalConfigurations.PAYLOAD_PERSISTENCE_BLOCK_CACHE_SIZE_PORTION.get(),
                InternalConfigurations.PAYLOAD_PERSISTENCE_BLOCK_SIZE,
                false);
    }

    @NotNull
    protected String getName() {
        return PublishPayloadLocalPersistence.PERSISTENCE_NAME;
    }

    @NotNull
    protected String getVersion() {
        return PERSISTENCE_VERSION;
    }

    @NotNull
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected @NotNull Options getOptions() {
        return new Options()
                .setCreateIfMissing(true)
                .setStatistics(new Statistics());
    }

    @PostConstruct
    protected void postConstruct() {
        super.postConstruct();
    }

    @Override
    protected void init() {
        //noop
    }

    @Nullable
    public byte[] get(final long id) {
        final RocksDB bucket = getRocksDb(Long.toString(id));
        try {
            return bucket.get(serializeKey(id));
        } catch (final RocksDBException e) {
            log.error("Could not get a payload because of an exception: ", e);
        }
        return null;
    }

    public void put(final long id, @NotNull final byte[] payload) {
        checkNotNull(payload, "payload must not be null");
        final RocksDB bucket = getRocksDb(Long.toString(id));
        try {
            bucket.put(serializeKey(id), payload);
        } catch (final RocksDBException e) {
            log.error("Could not put a payload because of an exception: ", e);
        }
    }
}
