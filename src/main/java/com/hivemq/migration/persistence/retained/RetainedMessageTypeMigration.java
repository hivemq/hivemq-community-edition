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

package com.hivemq.migration.persistence.retained;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.migration.Migrations;
import com.hivemq.migration.TypeMigration;
import com.hivemq.migration.logging.PayloadExceptionLogging;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PayloadPersistenceException;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistenceProvider;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.Exceptions;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Florian Limpöck
 */
public class RetainedMessageTypeMigration implements TypeMigration {

    private static final Logger log = LoggerFactory.getLogger(RetainedMessageTypeMigration.class);
    private static final Logger migrationLog = LoggerFactory.getLogger(Migrations.MIGRATION_LOGGER_NAME);

    private final @NotNull Provider<RetainedMessageXodusLocalPersistence> xodusLocalPersistenceProvider;
    private final @NotNull Provider<RetainedMessageRocksDBLocalPersistence> rocksDBLocalPersistenceProvider;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PublishPayloadLocalPersistenceProvider publishPayloadLocalPersistenceProvider;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull PayloadExceptionLogging payloadExceptionLogging;

    private final int bucketCount;
    private final @NotNull PersistenceType configuredType;

    @Inject
    public RetainedMessageTypeMigration(final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull Provider<RetainedMessageXodusLocalPersistence> xodusLocalPersistenceProvider,
            final @NotNull Provider<RetainedMessageRocksDBLocalPersistence> rocksDBLocalPersistenceProvider,
            final @NotNull PublishPayloadLocalPersistenceProvider publishPayloadLocalPersistenceProvider,
            final @NotNull SystemInformation systemInformation,
            final @NotNull PayloadExceptionLogging payloadExceptionLogging) {
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.xodusLocalPersistenceProvider = xodusLocalPersistenceProvider;
        this.rocksDBLocalPersistenceProvider = rocksDBLocalPersistenceProvider;
        this.publishPayloadLocalPersistenceProvider = publishPayloadLocalPersistenceProvider;
        this.systemInformation = systemInformation;
        this.bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
        this.payloadExceptionLogging = payloadExceptionLogging;
        this.configuredType = InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get();
    }

    @Override
    public void migrateToType(final @NotNull PersistenceType type) {
        if (type.equals(PersistenceType.FILE_NATIVE)) {
            migrateToRocksDB();
        } else if(type.equals(PersistenceType.FILE)) {
            migrateToXodus();
        } else {
            throw new IllegalArgumentException("Unknown persistence type " + type + " for retained message migration");
        }
    }

    private void migrateToXodus() {

        final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(RetainedMessageRocksDBLocalPersistence.PERSISTENCE_NAME, RetainedMessageRocksDBLocalPersistence.PERSISTENCE_VERSION);

        final File oldPersistenceFolder = new File(persistenceFolder, "retained_messages_0");
        if (!oldPersistenceFolder.exists()) {
            migrationLog.info("no (old) persistence folder (retained_messages) present, skipping migration");
            log.debug("no (old) persistence folder (retained_messages) present, skipping migration");
            return;
        }

        final RetainedMessageXodusLocalPersistence xodus = xodusLocalPersistenceProvider.get();
        final RetainedMessageRocksDBLocalPersistence rocks = rocksDBLocalPersistenceProvider.get();
        final PublishPayloadLocalPersistence publishPayloadLocalPersistence = publishPayloadLocalPersistenceProvider.get();

        final AtomicLong counter = new AtomicLong(0);

        rocks.iterate(new RetainedMessagePersistenceTypeSwitchCallback(bucketCount, publishPayloadLocalPersistence, xodus, payloadExceptionLogging, counter));

        migrationLog.info("Successfully migrated {} retained messages", counter.get());
        log.debug("Successfully migrated {} retained messages", counter.get());

        savePersistenceType(PersistenceType.FILE);

        rocks.stop();

    }

    private void migrateToRocksDB() {

        final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(RetainedMessageXodusLocalPersistence.PERSISTENCE_NAME, RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION);

        final File oldPersistenceFolder = new File(persistenceFolder, "retained_messages_0");
        if (!oldPersistenceFolder.exists()) {
            migrationLog.info("no (old) persistence folder (retained_messages) present, skipping migration");
            log.debug("no (old) persistence folder (retained_messages) present, skipping migration");
            return;
        }

        final RetainedMessageXodusLocalPersistence xodus = xodusLocalPersistenceProvider.get();
        final RetainedMessageRocksDBLocalPersistence rocks = rocksDBLocalPersistenceProvider.get();
        final PublishPayloadLocalPersistence publishPayloadLocalPersistence = publishPayloadLocalPersistenceProvider.get();

        final AtomicLong counter = new AtomicLong(0);

        xodus.iterate(new RetainedMessagePersistenceTypeSwitchCallback(bucketCount, publishPayloadLocalPersistence, rocks, payloadExceptionLogging, counter));

        migrationLog.info("Successfully migrated {} retained messages", counter.get());
        log.debug("Successfully migrated {} retained messages", counter.get());

        savePersistenceType(PersistenceType.FILE_NATIVE);

        xodus.stop();
    }

    private void savePersistenceType(final @NotNull PersistenceType persistenceType) {
        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);
        metaFile.setRetainedMessagesPersistenceType(persistenceType);
        metaFile.setRetainedMessagesPersistenceVersion(persistenceType == PersistenceType.FILE_NATIVE ? RetainedMessageRocksDBLocalPersistence.PERSISTENCE_VERSION : RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION);
        MetaFileService.writeMetaFile(systemInformation, metaFile);
    }

    private boolean checkPreviousType(final @NotNull PersistenceType persistenceType) {

        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        final PersistenceType metaType = metaInformation.getRetainedMessagesPersistenceType();

        if (metaType != null && metaType.equals(persistenceType)) {
            //should never happen since getNeededMigrations() will skip those.
            migrationLog.info("Retained message persistence is already migrated to current type {}, skipping migration", persistenceType);
            log.debug("Retained message persistence is already migrated to current type {}, skipping migration", persistenceType);
            return false;
        }
        return true;
    }


    @VisibleForTesting
    static class RetainedMessagePersistenceTypeSwitchCallback implements RetainedMessageLocalPersistence.ItemCallback {

        private final int bucketCount;
        private final @NotNull PublishPayloadLocalPersistence payloadLocalPersistence;
        private final @NotNull RetainedMessageLocalPersistence retainedMessageLocalPersistence;
        private final @NotNull PayloadExceptionLogging payloadExceptionLogging;
        private final @NotNull AtomicLong counter;

        RetainedMessagePersistenceTypeSwitchCallback(final int bucketCount,
                final @NotNull PublishPayloadLocalPersistence payloadLocalPersistence,
                final @NotNull RetainedMessageLocalPersistence retainedMessageLocalPersistence,
                final @NotNull PayloadExceptionLogging payloadExceptionLogging,
                final @NotNull AtomicLong counter) {
            this.bucketCount = bucketCount;
            this.payloadLocalPersistence = payloadLocalPersistence;
            this.retainedMessageLocalPersistence = retainedMessageLocalPersistence;
            this.payloadExceptionLogging = payloadExceptionLogging;
            this.counter = counter;
        }

        @Override
        public void onItem(final @NotNull String topic, final @NotNull RetainedMessage message) {
            Preconditions.checkNotNull(message.getPayloadId(), "Payload ID must never be null here");
            try {
                final int bucketIndex = BucketUtils.getBucket(topic, bucketCount);
                final byte[] bytes = payloadLocalPersistence.get(message.getPayloadId());
                if (bytes == null) {
                    payloadExceptionLogging.addLogging(message.getPayloadId(), true, topic);
                    return;
                }
                retainedMessageLocalPersistence.put(message, topic, bucketIndex);
                counter.incrementAndGet();

            } catch (final PayloadPersistenceException payloadException) {
                payloadExceptionLogging.addLogging(message.getPayloadId(), true, topic);
            } catch (final Throwable throwable) {
                log.warn("Could not migrate retained message for topic {}, original exception: ", topic, throwable);
                Exceptions.rethrowError(throwable);
            }
        }
    }
}
