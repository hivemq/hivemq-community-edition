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

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.FilePersistence;
import com.hivemq.persistence.LocalPersistence;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.LRUCache;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.Statistics;
import org.slf4j.Logger;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class RocksDBLocalPersistence implements LocalPersistence, FilePersistence {

    protected final AtomicBoolean stopped = new AtomicBoolean(false);
    protected final @NotNull RocksDB[] buckets;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PersistenceStartup persistenceStartup;
    private final int bucketCount;
    private final int memTableSizePortion;
    private final int blockCacheSizePortion;
    private final int blockSize;
    private final boolean enabled;

    protected RocksDBLocalPersistence(
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PersistenceStartup persistenceStartup,
            final int bucketCount,
            final int memTableSizePortion,
            final int blockCacheSizePortion,
            final int blockSize,
            final boolean enabled) {
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.persistenceStartup = persistenceStartup;
        this.bucketCount = bucketCount;
        this.buckets = new RocksDB[bucketCount];
        this.memTableSizePortion = memTableSizePortion;
        this.blockCacheSizePortion = blockCacheSizePortion;
        this.blockSize = blockSize;
        this.enabled = enabled;
    }

    protected abstract @NotNull String getName();

    protected abstract @NotNull String getVersion();

    protected abstract @NotNull Logger getLogger();

    public int getBucketCount() {
        return bucketCount;
    }

    protected void postConstruct() {
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
        final Options options = new Options();
        final Logger logger = getLogger();
        try {
            final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(name, version);
            final long memTableSize = physicalMemory() / memTableSizePortion / bucketCount;
            final LRUCache cache = new LRUCache(physicalMemory() / blockCacheSizePortion / bucketCount);
            final BlockBasedTableConfig tableConfig = new BlockBasedTableConfig();
            tableConfig.setBlockCache(cache);
            tableConfig.setBlockSize(blockSize);
            options.setStatistics(new Statistics());
            options.setCreateIfMissing(true);
            options.setTableFormatConfig(tableConfig);
            options.setWriteBufferSize(memTableSize);

            options.setStatsPersistPeriodSec(InternalConfigurations.ROCKSDB_STATS_PERSIST_PERIOD_SEC);
            options.setStatsDumpPeriodSec(InternalConfigurations.ROCKSDB_STATS_PERSIST_PERIOD_SEC);
            options.setMaxLogFileSize(InternalConfigurations.ROCKSDB_MAX_LOG_FILE_SIZE_BYTES);
            options.setKeepLogFileNum(InternalConfigurations.ROCKSDB_LOG_FILES_COUNT);
            options.setStatsHistoryBufferSize(InternalConfigurations.OCKSDB_STATS_HISTORY_BUFFER_SIZE_BYTES);

            for (int i = 0; i < bucketCount; i++) {
                final File persistenceFile = new File(persistenceFolder, name + "_" + i);
                final RocksDB rocksDB = RocksDB.open(options, persistenceFile.getAbsolutePath());
                buckets[i] = rocksDB;
            }

        } catch (final RocksDBException e) {
            logger.error("An error occurred while opening the {} persistence. Is another HiveMQ instance running?",
                    name);
            logger.info("Original Exception:", e);
            throw new UnrecoverableException();
        }

        init();
    }

    @Override
    public void start() {

        final String name = getName();
        final String version = getVersion();
        final Options options = new Options();
        final Logger logger = getLogger();

        try {
            final long memTableSize = physicalMemory() / memTableSizePortion / bucketCount;
            final long blockCacheMaxSize = physicalMemory() / blockCacheSizePortion;
            final LRUCache cache = new LRUCache(blockCacheMaxSize);
            final BlockBasedTableConfig tableConfig = new BlockBasedTableConfig();
            tableConfig.setBlockCache(cache);
            tableConfig.setBlockSize(blockSize);
            options.setStatistics(new Statistics());
            options.setCreateIfMissing(true);
            options.setTableFormatConfig(tableConfig);
            options.setWriteBufferSize(memTableSize);

            options.setStatsPersistPeriodSec(InternalConfigurations.ROCKSDB_STATS_PERSIST_PERIOD_SEC);
            options.setStatsDumpPeriodSec(InternalConfigurations.ROCKSDB_STATS_PERSIST_PERIOD_SEC);
            options.setMaxLogFileSize(InternalConfigurations.ROCKSDB_MAX_LOG_FILE_SIZE_BYTES);
            options.setKeepLogFileNum(InternalConfigurations.ROCKSDB_LOG_FILES_COUNT);
            options.setStatsHistoryBufferSize(InternalConfigurations.OCKSDB_STATS_HISTORY_BUFFER_SIZE_BYTES);

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
            logger.error("An error occurred while opening the {} persistence. Is another HiveMQ instance running?",
                    name);
            logger.info("Original Exception:", e);
            throw new UnrecoverableException();
        }

        init();
    }

    protected static long physicalMemory() {
        final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            final com.sun.management.OperatingSystemMXBean sunBeam =
                    (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;

            final long physicalMemory = sunBeam.getTotalPhysicalMemorySize();
            if (physicalMemory > 0) {
                return physicalMemory;
            }
        }

        final long heap = Runtime.getRuntime().maxMemory();
        final double fallbackEstimation = 1.5;
        return (long) (heap * fallbackEstimation);
    }

    /**
     * Overwrite this method to configure options and overwrite default values
     *
     * @param options the options object which can be configured
     */
    protected void configureOptions(final @NotNull Options options) {
        // default noop
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

    protected @NotNull RocksDB getRocksDb(final @NotNull String key) {
        return buckets[BucketUtils.getBucket(key, bucketCount)];
    }

    protected int getBucketIndex(final @NotNull String key) {
        return BucketUtils.getBucket(key, bucketCount);
    }

    protected void checkBucketIndex(final int bucketIndex) {
        checkArgument(bucketIndex >= 0 && bucketIndex < buckets.length, "Invalid bucket index: " + bucketIndex);
    }
}
