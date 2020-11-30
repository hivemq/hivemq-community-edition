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
package com.hivemq.migration.persistence.retained;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.migration.Migrations;
import com.hivemq.migration.ValueMigration;
import com.hivemq.migration.logging.PayloadExceptionLogging;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.migration.persistence.legacy.*;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.*;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.Exceptions;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Florian Limpöck
 * @since 4.5.0
 */
@LazySingleton
public class RetainedMessagePayloadIDMigration implements ValueMigration {

    private static final Logger log = LoggerFactory.getLogger(RetainedMessagePayloadIDMigration.class);
    private static final Logger migrationLog = LoggerFactory.getLogger(Migrations.MIGRATION_LOGGER_NAME);
    private static final String FIRST_BUCKET_FOLDER = "retained_messages_0";

    private final @NotNull Provider<RetainedMessageXodusLocalPersistence_4_4> retainedMessageXodusLocalPersistence_4_4Provider;
    private final @NotNull Provider<RetainedMessageRocksDBLocalPersistence_4_4> retainedMessageRocksDBLocalPersistence_4_4Provider;
    private final @NotNull Provider<PublishPayloadXodusLocalPersistence_4_4> publishPayloadXodusLocalPersistence_4_4Provider;
    private final @NotNull Provider<PublishPayloadRocksDBLocalPersistence_4_4> publishPayloadRocksDBLocalPersistence_4_4Provider;
    private final @NotNull Provider<RetainedMessageXodusLocalPersistence> localXodusPersistenceProvider;
    private final @NotNull Provider<RetainedMessageRocksDBLocalPersistence> localRocksPersistenceProvider;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull Provider<PublishPayloadRocksDBLocalPersistence> publishPayloadRocksDBLocalPersistenceProvider;
    private final @NotNull Provider<PublishPayloadXodusLocalPersistence> publishPayloadXodusLocalPersistenceProvider;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull PayloadExceptionLogging payloadExceptionLogging;

    private final int bucketCount;

    private final @NotNull AtomicReference<PersistenceType> previousPayloadType = new AtomicReference<>();
    private final @NotNull AtomicReference<PersistenceType> previousRetainedType = new AtomicReference<>();

    @Inject
    public RetainedMessagePayloadIDMigration(final @NotNull Provider<RetainedMessageXodusLocalPersistence_4_4> retainedMessageXodusLocalPersistence_4_4Provider,
                                             final @NotNull Provider<RetainedMessageRocksDBLocalPersistence_4_4> retainedMessageRocksDBLocalPersistence_4_4Provider,
                                             final @NotNull Provider<PublishPayloadXodusLocalPersistence_4_4> publishPayloadXodusLocalPersistence_4_4Provider,
                                             final @NotNull Provider<PublishPayloadRocksDBLocalPersistence_4_4> publishPayloadRocksDBLocalPersistence_4_4Provider,
                                             final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                             final @NotNull Provider<RetainedMessageXodusLocalPersistence> localXodusPersistenceProvider,
                                             final @NotNull Provider<RetainedMessageRocksDBLocalPersistence> localRocksPersistenceProvider,
                                             final @NotNull Provider<PublishPayloadRocksDBLocalPersistence> publishPayloadRocksDBLocalPersistenceProvider,
                                             final @NotNull Provider<PublishPayloadXodusLocalPersistence> publishPayloadXodusLocalPersistenceProvider,
                                             final @NotNull SystemInformation systemInformation,
                                             final @NotNull PayloadExceptionLogging payloadExceptionLogging) {
        this.retainedMessageXodusLocalPersistence_4_4Provider = retainedMessageXodusLocalPersistence_4_4Provider;
        this.retainedMessageRocksDBLocalPersistence_4_4Provider = retainedMessageRocksDBLocalPersistence_4_4Provider;
        this.publishPayloadXodusLocalPersistence_4_4Provider = publishPayloadXodusLocalPersistence_4_4Provider;
        this.publishPayloadRocksDBLocalPersistence_4_4Provider = publishPayloadRocksDBLocalPersistence_4_4Provider;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.localRocksPersistenceProvider = localRocksPersistenceProvider;
        this.localXodusPersistenceProvider = localXodusPersistenceProvider;
        this.publishPayloadRocksDBLocalPersistenceProvider = publishPayloadRocksDBLocalPersistenceProvider;
        this.publishPayloadXodusLocalPersistenceProvider = publishPayloadXodusLocalPersistenceProvider;
        this.systemInformation = systemInformation;
        this.bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
        this.payloadExceptionLogging = payloadExceptionLogging;
    }

    private boolean oldFolderMissing(final @NotNull File persistenceFolder) {
        final File oldPersistenceFolder = new File(persistenceFolder, FIRST_BUCKET_FOLDER);
        if (!oldPersistenceFolder.exists()) {
            migrationLog.info("No (old) persistence folder (retained_messages) present, skipping migration.");
            log.debug("No (old) persistence folder (retained_messages) present, skipping migration.");
            return true;
        }
        return false;
    }

    private void savePersistenceVersion(final @NotNull PersistenceType persistenceType) {
        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);
        metaFile.setRetainedMessagesPersistenceVersion(persistenceType == PersistenceType.FILE_NATIVE ? RetainedMessageRocksDBLocalPersistence.PERSISTENCE_VERSION : RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION);
        MetaFileService.writeMetaFile(systemInformation, metaFile);
    }

    @Override
    public void migrateToValue() {

        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);

        final RetainedMessageLocalPersistence retainedMessageLocalPersistence;
        if (metaFile.getRetainedMessagesPersistenceType() == PersistenceType.FILE_NATIVE) {
            final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(RetainedMessageRocksDBLocalPersistence.PERSISTENCE_NAME, RetainedMessageRocksDBLocalPersistence_4_4.PERSISTENCE_VERSION);
            if (oldFolderMissing(persistenceFolder)) {
                return;
            }
            retainedMessageLocalPersistence = localRocksPersistenceProvider.get();
            previousRetainedType.set(PersistenceType.FILE_NATIVE);
        } else {
            final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(RetainedMessageXodusLocalPersistence.PERSISTENCE_NAME, RetainedMessageXodusLocalPersistence_4_4.PERSISTENCE_VERSION);
            if (oldFolderMissing(persistenceFolder)) {
                return;
            }
            retainedMessageLocalPersistence = localXodusPersistenceProvider.get();
            previousRetainedType.set(PersistenceType.FILE);
        }

        final PublishPayloadLocalPersistence publishPayloadLocalPersistence;
        final PublishPayloadLocalPersistence_4_4 legacyPayloadPersistence;
        if (metaFile.getPublishPayloadPersistenceType() == PersistenceType.FILE_NATIVE) {
            legacyPayloadPersistence = publishPayloadRocksDBLocalPersistence_4_4Provider.get();
            publishPayloadLocalPersistence = publishPayloadRocksDBLocalPersistenceProvider.get();
            previousPayloadType.set(PersistenceType.FILE_NATIVE);
        } else {
            legacyPayloadPersistence = publishPayloadXodusLocalPersistence_4_4Provider.get();
            publishPayloadLocalPersistence = publishPayloadXodusLocalPersistenceProvider.get();
            previousPayloadType.set(PersistenceType.FILE);
        }
        final RetainedMessagePersistenceValueSwitchCallback iterationCallback = new RetainedMessagePersistenceValueSwitchCallback(bucketCount, publishPayloadLocalPersistence, retainedMessageLocalPersistence, payloadExceptionLogging, legacyPayloadPersistence);
        if (metaFile.getRetainedMessagesPersistenceType() == PersistenceType.FILE_NATIVE) {
            retainedMessageRocksDBLocalPersistence_4_4Provider.get().iterate(iterationCallback);
            savePersistenceVersion(PersistenceType.FILE_NATIVE);
        } else {
            retainedMessageXodusLocalPersistence_4_4Provider.get().iterate(iterationCallback);
            savePersistenceVersion(PersistenceType.FILE);
        }
        retainedMessageLocalPersistence.bootstrapPayloads();
    }

    public void closeLegacy() {
        if (previousPayloadType.get() == PersistenceType.FILE_NATIVE) {
            publishPayloadRocksDBLocalPersistence_4_4Provider.get().closeDB();
        } else {
            publishPayloadXodusLocalPersistence_4_4Provider.get().closeDB();
        }
        if (previousRetainedType.get() == PersistenceType.FILE_NATIVE) {
            retainedMessageRocksDBLocalPersistence_4_4Provider.get().closeDB();
        } else {
            retainedMessageXodusLocalPersistence_4_4Provider.get().closeDB();
        }
    }

    @VisibleForTesting
    static class RetainedMessagePersistenceValueSwitchCallback implements RetainedMessageItemCallback_4_4 {

        private final int bucketCount;
        private final @NotNull PublishPayloadLocalPersistence payloadLocalPersistence;
        private final @NotNull RetainedMessageLocalPersistence retainedMessageLocalPersistence;
        private final @NotNull PayloadExceptionLogging payloadExceptionLogging;
        private final @NotNull PublishPayloadLocalPersistence_4_4 legacyPayloadPersistence;

        RetainedMessagePersistenceValueSwitchCallback(final int bucketCount,
                                                      final @NotNull PublishPayloadLocalPersistence payloadLocalPersistence,
                                                      final @NotNull RetainedMessageLocalPersistence retainedMessageLocalPersistence,
                                                      final @NotNull PayloadExceptionLogging payloadExceptionLogging,
                                                      final @NotNull PublishPayloadLocalPersistence_4_4 legacyPayloadPersistence) {
            this.bucketCount = bucketCount;
            this.payloadLocalPersistence = payloadLocalPersistence;
            this.retainedMessageLocalPersistence = retainedMessageLocalPersistence;
            this.payloadExceptionLogging = payloadExceptionLogging;
            this.legacyPayloadPersistence = legacyPayloadPersistence;
        }

        @Override
        public void onItem(final @NotNull String topic, final @NotNull RetainedMessage message) {
            try {
                final int bucketIndex = BucketUtils.getBucket(topic, bucketCount);
                final byte[] bytes = legacyPayloadPersistence.get(message.getPublishId());
                if (bytes == null) {
                    payloadExceptionLogging.addLogging(message.getPublishId(), true, topic);
                    return;
                }
                final long newPayloadId = PublishPayloadPersistenceImpl.createId();
                payloadLocalPersistence.put(newPayloadId, bytes);
                message.setPublishId(newPayloadId);
                retainedMessageLocalPersistence.put(message, topic, bucketIndex);

            } catch (final PayloadPersistenceException payloadException) {
                payloadExceptionLogging.addLogging(message.getPublishId(), true, topic);
            } catch (final Throwable throwable) {
                log.warn("Could not migrate retained message for topic {}, original exception: ", topic, throwable);
                Exceptions.rethrowError(throwable);
            }
        }
    }


}
