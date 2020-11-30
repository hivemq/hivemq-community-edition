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
package com.hivemq.migration.persistence.queue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.migration.Migrations;
import com.hivemq.migration.ValueMigration;
import com.hivemq.migration.logging.PayloadExceptionLogging;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.migration.persistence.legacy.*;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.clientqueue.ClientQueueEntry;
import com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl;
import com.hivemq.persistence.clientqueue.ClientQueueXodusLocalPersistence;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.payload.PublishPayloadRocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadXodusLocalPersistence;
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
 * @author Lukas Brandl
 * @since 4.5.0
 */
@LazySingleton
public class ClientQueuePayloadIDMigration implements ValueMigration {

    private static final Logger log = LoggerFactory.getLogger(ClientQueuePayloadIDMigration.class);
    private static final Logger migrationLog = LoggerFactory.getLogger(Migrations.MIGRATION_LOGGER_NAME);
    private static final String FIRST_BUCKET_FOLDER = "client_queue_0";

    private final @NotNull Provider<ClientSessionLocalPersistence> sessionLocalPersistenceProvider;
    private final @NotNull Provider<ClientQueueXodusLocalPersistence> localXodusPersistenceProvider;
    private final @NotNull Provider<ClientQueueXodusLocalPersistence_4_4> clientQueueXodusLocalPersistence_4_4Provider;
    private final @NotNull Provider<PublishPayloadXodusLocalPersistence_4_4> publishPayloadXodusLocalPersistence_4_4Provider;
    private final @NotNull Provider<PublishPayloadRocksDBLocalPersistence_4_4> publishPayloadRocksDBLocalPersistence_4_4Provider;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull Provider<PublishPayloadRocksDBLocalPersistence> publishPayloadRocksDBLocalPersistenceProvider;
    private final @NotNull Provider<PublishPayloadXodusLocalPersistence> publishPayloadXodusLocalPersistenceProvider;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull PayloadExceptionLogging payloadExceptionLogging;
    private final @NotNull PublishPayloadPersistence publishPayloadPersistence;

    private final int bucketCount;

    private final @NotNull AtomicReference<PersistenceType> previousPayloadType = new AtomicReference<>();

    @Inject
    public ClientQueuePayloadIDMigration(final @NotNull Provider<ClientSessionLocalPersistence> sessionLocalPersistenceProvider,
                                         final @NotNull Provider<ClientQueueXodusLocalPersistence> localXodusPersistenceProvider,
                                         final @NotNull Provider<ClientQueueXodusLocalPersistence_4_4> clientQueueXodusLocalPersistence_4_4Provider,
                                         final @NotNull Provider<PublishPayloadXodusLocalPersistence_4_4> publishPayloadXodusLocalPersistence_4_4Provider,
                                         final @NotNull Provider<PublishPayloadRocksDBLocalPersistence_4_4> publishPayloadRocksDBLocalPersistence_4_4Provider,
                                         final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                         final @NotNull Provider<PublishPayloadRocksDBLocalPersistence> publishPayloadRocksDBLocalPersistenceProvider,
                                         final @NotNull Provider<PublishPayloadXodusLocalPersistence> publishPayloadXodusLocalPersistenceProvider,
                                         final @NotNull SystemInformation systemInformation,
                                         final @NotNull PayloadExceptionLogging payloadExceptionLogging,
                                         final @NotNull PublishPayloadPersistence publishPayloadPersistence) {
        this.sessionLocalPersistenceProvider = sessionLocalPersistenceProvider;
        this.localXodusPersistenceProvider = localXodusPersistenceProvider;
        this.clientQueueXodusLocalPersistence_4_4Provider = clientQueueXodusLocalPersistence_4_4Provider;
        this.publishPayloadXodusLocalPersistence_4_4Provider = publishPayloadXodusLocalPersistence_4_4Provider;
        this.publishPayloadRocksDBLocalPersistence_4_4Provider = publishPayloadRocksDBLocalPersistence_4_4Provider;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.publishPayloadRocksDBLocalPersistenceProvider = publishPayloadRocksDBLocalPersistenceProvider;
        this.publishPayloadXodusLocalPersistenceProvider = publishPayloadXodusLocalPersistenceProvider;
        this.systemInformation = systemInformation;
        this.bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
        this.payloadExceptionLogging = payloadExceptionLogging;
        this.publishPayloadPersistence = publishPayloadPersistence;
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

    @Override
    public void migrateToValue() {

        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);

        final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(ClientQueueXodusLocalPersistence.PERSISTENCE_NAME, ClientQueueXodusLocalPersistence_4_4.PERSISTENCE_VERSION);
        if (oldFolderMissing(persistenceFolder)) {
            return;
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

        final var iterationCallback = new QueuedMessagePersistenceValueSwitchCallback(bucketCount,
                publishPayloadLocalPersistence, localXodusPersistenceProvider.get(), payloadExceptionLogging,
                legacyPayloadPersistence, sessionLocalPersistenceProvider.get(), publishPayloadPersistence);

        clientQueueXodusLocalPersistence_4_4Provider.get().iterate(iterationCallback);
    }

    public void closeLegacy() {
        if (previousPayloadType.get() == PersistenceType.FILE_NATIVE) {
            publishPayloadRocksDBLocalPersistence_4_4Provider.get().closeDB();
        } else {
            publishPayloadXodusLocalPersistence_4_4Provider.get().closeDB();
        }
        clientQueueXodusLocalPersistence_4_4Provider.get().closeDB();
    }

    @VisibleForTesting
    static class QueuedMessagePersistenceValueSwitchCallback implements ClientQueueXodusLocalPersistence_4_4.QueueCallback_4_4 {

        private final int bucketCount;
        private final @NotNull PublishPayloadLocalPersistence payloadLocalPersistence;
        private final @NotNull ClientQueueXodusLocalPersistence clientQueueXodusLocalPersistence;
        private final @NotNull PayloadExceptionLogging payloadExceptionLogging;
        private final @NotNull PublishPayloadLocalPersistence_4_4 legacyPayloadPersistence;
        private final @NotNull ClientSessionLocalPersistence sessionLocalPersistence;
        private final @NotNull PublishPayloadPersistence publishPayloadPersistence;

        QueuedMessagePersistenceValueSwitchCallback(final int bucketCount,
                                                    final @NotNull PublishPayloadLocalPersistence payloadLocalPersistence,
                                                    final @NotNull ClientQueueXodusLocalPersistence clientQueueXodusLocalPersistence,
                                                    final @NotNull PayloadExceptionLogging payloadExceptionLogging,
                                                    final @NotNull PublishPayloadLocalPersistence_4_4 legacyPayloadPersistence,
                                                    final @NotNull ClientSessionLocalPersistence sessionLocalPersistence,
                                                    final @NotNull PublishPayloadPersistence publishPayloadPersistence) {
            this.bucketCount = bucketCount;
            this.payloadLocalPersistence = payloadLocalPersistence;
            this.clientQueueXodusLocalPersistence = clientQueueXodusLocalPersistence;
            this.payloadExceptionLogging = payloadExceptionLogging;
            this.legacyPayloadPersistence = legacyPayloadPersistence;
            this.sessionLocalPersistence = sessionLocalPersistence;
            this.publishPayloadPersistence = publishPayloadPersistence;
        }

        @Override
        public void onItem(final ClientQueuePersistenceImpl.@NotNull Key key, final @NotNull ImmutableList<ClientQueueEntry> messages) {
            try {
                if (messages.isEmpty()) {
                    //no need to migrate no messages
                    return;
                }
                final int bucketIndex = BucketUtils.getBucket(key.getQueueId(), bucketCount);
                if (!key.isShared()) {
                    final ClientSession session = sessionLocalPersistence.getSession(key.getQueueId(), true, false);
                    if (session == null) {
                        // no need to migrate queued messages for expired sessions
                        return;
                    }
                }
                for (final ClientQueueEntry queueEntry : messages) {
                    if (queueEntry.getMessageWithID() instanceof PUBLISH_4_4) {
                        final PUBLISH_4_4 legacyPublish = (PUBLISH_4_4) queueEntry.getMessageWithID();
                        final byte[] bytes = legacyPayloadPersistence.get(legacyPublish.getPayloadID());
                        if (bytes == null) {
                            payloadExceptionLogging.addLogging(legacyPublish.getPayloadID(), null, null);
                            continue;
                        }
                        payloadLocalPersistence.put(legacyPublish.getPublish().getPublishId(), bytes);
                        publishPayloadPersistence.incrementReferenceCounterOnBootstrap(legacyPublish.getPublish().getPublishId());
                        clientQueueXodusLocalPersistence.add(key.getQueueId(), key.isShared(), legacyPublish.getPublish(),
                                Long.MAX_VALUE, MqttConfigurationService.QueuedMessagesStrategy.DISCARD, queueEntry.isRetained(), bucketIndex);
                    }
                    if (queueEntry.getMessageWithID() instanceof PUBREL) {
                        clientQueueXodusLocalPersistence.replace(key.getQueueId(), (PUBREL) queueEntry.getMessageWithID(), bucketIndex);
                    }
                }
            } catch (final Throwable throwable) {
                log.warn("Could not migrate queued messages for queue id " + key.getQueueId() + ". Original exception: ", throwable);
                Exceptions.rethrowError(throwable);
            }
        }
    }


}
