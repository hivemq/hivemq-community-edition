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

import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.FilePersistence;
import com.hivemq.persistence.LocalPersistence;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.util.LocalPersistenceFileUtil;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.io.FileDataWriter;
import jetbrains.exodus.log.LogConfig;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;
import static com.hivemq.configuration.service.InternalConfigurations.PERSISTENCE_CLOSE_RETRIES;
import static com.hivemq.configuration.service.InternalConfigurations.PERSISTENCE_CLOSE_RETRY_INTERVAL_MSEC;

public abstract class XodusLocalPersistence implements LocalPersistence, FilePersistence {

    private final @NotNull EnvironmentUtil environmentUtil;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PersistenceStartup persistenceStartup;
    protected final @NotNull AtomicBoolean stopped = new AtomicBoolean(false);

    protected @NotNull Bucket[] buckets;
    protected int bucketCount;
    private final boolean enabled;

    private final int closeRetries;
    private final int closeRetryInterval;

    protected XodusLocalPersistence(
            final @NotNull EnvironmentUtil environmentUtil,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PersistenceStartup persistenceStartup,
            final int bucketCount,
            final boolean enabled) {

        this.environmentUtil = environmentUtil;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.persistenceStartup = persistenceStartup;
        this.bucketCount = bucketCount;
        this.buckets = new Bucket[bucketCount];
        this.enabled = enabled;

        this.closeRetries = PERSISTENCE_CLOSE_RETRIES.get();
        this.closeRetryInterval = PERSISTENCE_CLOSE_RETRY_INTERVAL_MSEC.get();
    }

    protected abstract @NotNull String getName();

    protected abstract @NotNull String getVersion();

    public int getBucketCount() {
        return bucketCount;
    }

    protected abstract @NotNull StoreConfig getStoreConfig();

    protected abstract @NotNull Logger getLogger();

    protected void postConstruct() {
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
        final StoreConfig storeConfig = getStoreConfig();
        final Logger logger = getLogger();

        try {
            final EnvironmentConfig environmentConfig = environmentUtil.createEnvironmentConfig(name);
            final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(name, version);

            for (int i = 0; i < bucketCount; i++) {
                final File persistenceFile = new File(persistenceFolder, name + "_" + i);
                if (!persistenceFile.exists() && !persistenceFile.mkdirs()) {
                    logger.error("Failed to create directory: {}", persistenceFile);
                    throw new UnrecoverableException();
                }

                final LogConfig logConfig = new LogConfig();
                logConfig.setDir(persistenceFile);
                logConfig.setWriter(new XodusNoLockDataWriter(persistenceFile, logConfig));

                final Environment environment = Environments.newContextualInstance(logConfig, environmentConfig);
                final Store store =
                        environment.computeInTransaction(txn -> environment.openStore(name, storeConfig, txn));

                buckets[i] = new Bucket(environment, store);
            }

        } catch (final ExodusException e) {
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
        final StoreConfig storeConfig = getStoreConfig();
        final Logger logger = getLogger();

        try {
            final EnvironmentConfig environmentConfig = environmentUtil.createEnvironmentConfig(name);
            final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(name, version);

            final CountDownLatch counter = new CountDownLatch(bucketCount);

            for (int i = 0; i < bucketCount; i++) {
                final int finalI = i;
                persistenceStartup.submitEnvironmentCreate(() -> {

                    final File persistenceFile = new File(persistenceFolder, name + "_" + finalI);
                    if (!persistenceFile.exists() && !persistenceFile.mkdirs()) {
                        throw new ExodusException("Failed to create directory: " + persistenceFile);
                    }

                    final LogConfig logConfig = new LogConfig();
                    logConfig.setDir(persistenceFile);
                    logConfig.setWriter(new XodusNoLockDataWriter(persistenceFile, logConfig));

                    final Environment environment = Environments.newContextualInstance(logConfig, environmentConfig);
                    final Store store =
                            environment.computeInTransaction(txn -> environment.openStore(name, storeConfig, txn));

                    buckets[finalI] = new Bucket(environment, store);
                    counter.countDown();
                });
            }

            counter.await();

        } catch (final ExodusException | InterruptedException e) {
            logger.error("An error occurred while opening the {} persistence. Is another HiveMQ instance running?",
                    name);
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

        final Bucket bucket = buckets[bucketIndex];
        if (bucket == null) {
            //bucket not initialized
            return;
        }
        if (bucket.close()) {
            if (bucket.getEnvironment().isOpen()) {
                new EnvironmentCloser(getName() + "-closer",
                        bucket.getEnvironment(),
                        closeRetries,
                        closeRetryInterval).close();
            }
        }
    }

    public @NotNull Bucket getBucket(final @NotNull String key) {
        return buckets[BucketUtils.getBucket(key, bucketCount)];
    }

    protected void checkBucketIndex(final int bucketIndex) {
        checkArgument(bucketIndex >= 0 && bucketIndex < buckets.length, "Invalid bucket index: " + bucketIndex);
    }

    private static final class XodusNoLockDataWriter extends FileDataWriter {

        private XodusNoLockDataWriter(final @NotNull File persistenceFile, final @NotNull LogConfig logConfig) {
            super(persistenceFile, logConfig.getLockId());
        }

        @Override
        public boolean lock(final long timeout) {
            return true;
        }
    }
}
