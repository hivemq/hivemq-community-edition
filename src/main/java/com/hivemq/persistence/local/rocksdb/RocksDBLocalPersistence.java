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
package com.hivemq.persistence.local.rocksdb;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.persistence.FilePersistence;
import com.hivemq.persistence.LocalPersistence;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.PhysicalMemoryUtil;
import org.rocksdb.*;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Florian Limpöck
 */
public abstract class RocksDBLocalPersistence implements LocalPersistence, FilePersistence {

    protected final AtomicBoolean stopped = new AtomicBoolean(false);
    protected final @NotNull RocksDB[] buckets;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PersistenceStartup persistenceStartup;
    private final AtomicBoolean constructed = new AtomicBoolean(false);
    private final int bucketCount;
    private final int memtableSizePortion;
    private final int blockCacheSizePortion;
    private final int blockSize;
    private final boolean enabled;

    protected RocksDBLocalPersistence(
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PersistenceStartup persistenceStartup,
            final int internalBucketCount,
            final int memtableSizePortion,
            final int blockCacheSizePortion,
            final int blockSize,
            final boolean enabled) {
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.persistenceStartup = persistenceStartup;
        this.bucketCount = internalBucketCount;
        this.buckets = new RocksDB[bucketCount];
        this.memtableSizePortion = memtableSizePortion;
        this.blockCacheSizePortion = blockCacheSizePortion;
        this.blockSize = blockSize;
        this.enabled = enabled;
    }

    @NotNull
    protected abstract String getName();

    @NotNull
    protected abstract String getVersion();

    @NotNull
    protected abstract Options getOptions();

    @NotNull
    protected abstract Logger getLogger();

    public int getBucketCount() {
        return bucketCount;
    }

    protected void postConstruct() {

        //Protect from multiple calls to post construct
        if (constructed.getAndSet(true)) {
            return;
        }
        RocksDB.loadLibrary();
        if (enabled) {
            persistenceStartup.submitPersistenceStart(this);
        } else {
            startExternal();
        }
    }

    @Override
    public void startExternal() {

        final String name = getName();
        final String version = getVersion();
        final Options options = getOptions();
        final Logger logger = getLogger();
        try {
            final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(name, version);

            final long memtableSize = PhysicalMemoryUtil.physicalMemory() / memtableSizePortion / bucketCount;
            final LRUCache cache = new LRUCache(PhysicalMemoryUtil.physicalMemory() / blockCacheSizePortion / bucketCount);
            final BlockBasedTableConfig tableConfig = new BlockBasedTableConfig();
            tableConfig.setBlockCache(cache);
            tableConfig.setBlockSize(blockSize);
            options.setTableFormatConfig(tableConfig);
            options.setWriteBufferSize(memtableSize);

            options.setStatsPersistPeriodSec(InternalConfigurations.ROCKSDB_STATS_PERSIST_PERIOD);
            options.setStatsDumpPeriodSec(InternalConfigurations.ROCKSDB_STATS_PERSIST_PERIOD);
            options.setMaxLogFileSize(InternalConfigurations.ROCKSDB_MAX_LOG_FILE_SIZE);
            options.setKeepLogFileNum(InternalConfigurations.ROCKSDB_LOG_FILE_NUMBER);
            options.setStatsHistoryBufferSize(InternalConfigurations.ROCKSDB_STATS_HISTORY_BUFFER_SIZE);

            for (int i = 0; i < bucketCount; i++) {
                final File persistenceFile = new File(persistenceFolder, name + "_" + i);
                final RocksDB rocksDB = RocksDB.open(options, persistenceFile.getAbsolutePath());
                buckets[i] = rocksDB;
            }

        } catch (final RocksDBException e) {
            logger.error(
                    "An error occurred while opening the {} persistence. Is another HiveMQ instance running?", name);
            logger.info("Original Exception:", e);
            throw new UnrecoverableException();
        }

        init();
    }

    @Override
    public void start() {

        final String name = getName();
        final String version = getVersion();
        final Options options = getOptions();
        final Logger logger = getLogger();

        try {
            final long memtableSize = PhysicalMemoryUtil.physicalMemory() / memtableSizePortion / bucketCount;
            final long blockCacheMaxSize = PhysicalMemoryUtil.physicalMemory() / blockCacheSizePortion;
            final LRUCache cache = new LRUCache(blockCacheMaxSize);
            final BlockBasedTableConfig tableConfig = new BlockBasedTableConfig();
            tableConfig.setBlockCache(cache);
            tableConfig.setBlockSize(blockSize);
            options.setTableFormatConfig(tableConfig);
            options.setWriteBufferSize(memtableSize);

            options.setStatsPersistPeriodSec(InternalConfigurations.ROCKSDB_STATS_PERSIST_PERIOD);
            options.setStatsDumpPeriodSec(InternalConfigurations.ROCKSDB_STATS_PERSIST_PERIOD);
            options.setMaxLogFileSize(InternalConfigurations.ROCKSDB_MAX_LOG_FILE_SIZE);
            options.setKeepLogFileNum(InternalConfigurations.ROCKSDB_LOG_FILE_NUMBER);
            options.setStatsHistoryBufferSize(InternalConfigurations.ROCKSDB_STATS_HISTORY_BUFFER_SIZE);

            final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(name, version);
            final CountDownLatch counter = new CountDownLatch(bucketCount);
            for (int i = 0; i < bucketCount; i++) {
                final int finalI = i;
                persistenceStartup.submitEnvironmentCreate(() -> {
                    try {
                        final File persistenceFile = new File(persistenceFolder, name + "_" + finalI);
                        final RocksDB rocksDB = RocksDB.open(options, persistenceFile.getAbsolutePath());
                        buckets[finalI] = rocksDB;
                        counter.countDown();
                    } catch (final Exception e) {
                        logger.error(
                                "An error occurred while opening the {} persistence. Is another HiveMQ instance running?",
                                name);
                        logger.info("Original Exception:", e);
                        throw new UnrecoverableException();
                    }
                });
            }

            counter.await();

        } catch (final Exception e) {
            logger.error(
                    "An error occurred while opening the {} persistence. Is another HiveMQ instance running?", name);
            logger.info("Original Exception:", e);
            throw new UnrecoverableException();
        }

        init();

    }



    protected abstract void init();

    @Override
    public void stop() {
        stopped.set(true);
        closeDB();
    }

    public void closeDB() {
        for (int i = 0; i < bucketCount; i++) {
            closeDB(i);
        }
    }

    @Override
    public void closeDB(final int bucketIndex) {
        checkBucketIndex(bucketIndex);
        final RocksDB bucket = buckets[bucketIndex];
        bucket.close();
    }

    @NotNull
    protected RocksDB getRocksDb(final @NotNull String key) {
        return buckets[BucketUtils.getBucket(key, bucketCount)];
    }


    @NotNull
    protected int getBucketIndex(final @NotNull String key) {
        return BucketUtils.getBucket(key, bucketCount);
    }

    protected void checkBucketIndex(final int bucketIndex) {
        checkArgument(bucketIndex >= 0 && bucketIndex < buckets.length, "Invalid bucket index: " + bucketIndex);
    }
}
