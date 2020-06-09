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
package com.hivemq.migration.persistence.payload;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.migration.Migrations;
import com.hivemq.migration.TypeMigration;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadRocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadXodusLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;

import static com.hivemq.migration.meta.PersistenceType.FILE;
import static com.hivemq.migration.meta.PersistenceType.FILE_NATIVE;

/**
 * @author Florian Limpöck
 */
public class PublishPayloadTypeMigration implements TypeMigration {

    private static final Logger log = LoggerFactory.getLogger(PublishPayloadTypeMigration.class);
    private static final Logger migrationLog = LoggerFactory.getLogger(Migrations.MIGRATION_LOGGER_NAME);

    private final @NotNull SystemInformation systemInformation;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull Provider<PublishPayloadRocksDBLocalPersistence> persistenceRocksDBProvider;
    private final @NotNull Provider<PublishPayloadXodusLocalPersistence> persistenceXodusProvider;

    @Inject
    public PublishPayloadTypeMigration(final @NotNull SystemInformation systemInformation,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull Provider<PublishPayloadRocksDBLocalPersistence> persistenceRocksDBProvider,
            final @NotNull Provider<PublishPayloadXodusLocalPersistence> persistenceXodusProvider) {
        this.systemInformation = systemInformation;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.persistenceRocksDBProvider = persistenceRocksDBProvider;
        this.persistenceXodusProvider = persistenceXodusProvider;
    }

    @Override
    public void migrateToType(final @NotNull PersistenceType type) {
        if (type.equals(FILE_NATIVE)) {
            migrateToRocksDB();
        } else if(type.equals(PersistenceType.FILE)) {
            migrateToXodus();
        } else {
            throw new IllegalArgumentException("Unknown persistence type " + type + " for publish payload migration");
        }
    }

    private void migrateToXodus() {

        final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(PublishPayloadLocalPersistence.PERSISTENCE_NAME, PublishPayloadRocksDBLocalPersistence.PERSISTENCE_VERSION);

        final File publish_payload_store_0 = new File(persistenceFolder, "publish_payload_store_0");
        if (!publish_payload_store_0.exists()) {
            migrationLog.info("No (old) persistence folder (publish_payload) present, skipping migration.");
            log.debug("No (old) persistence folder (publish_payload) present, skipping migration.");
            return;
        }

        final PublishPayloadXodusLocalPersistence xodusPersistence = persistenceXodusProvider.get();
        final PublishPayloadRocksDBLocalPersistence rocksdbPersistence = persistenceRocksDBProvider.get();

        migrateFromTo(rocksdbPersistence, xodusPersistence, FILE);
    }

    private void migrateToRocksDB() {

        final File persistenceFolder = localPersistenceFileUtil.getVersionedLocalPersistenceFolder(PublishPayloadLocalPersistence.PERSISTENCE_NAME, PublishPayloadXodusLocalPersistence.PERSISTENCE_VERSION);

        final File publish_payload_store_0 = new File(persistenceFolder, "publish_payload_store_0");
        if (!publish_payload_store_0.exists()) {
            migrationLog.info("No (old) persistence folder (publish_payload) present, skipping migration.");
            log.debug("No (old) persistence folder (publish_payload) present, skipping migration.");
            return;
        }

        final PublishPayloadXodusLocalPersistence xodusPersistence = persistenceXodusProvider.get();
        final PublishPayloadRocksDBLocalPersistence rocksdbPersistence = persistenceRocksDBProvider.get();

        migrateFromTo(xodusPersistence, rocksdbPersistence, FILE_NATIVE);
    }

    private void migrateFromTo(final @NotNull PublishPayloadLocalPersistence from, final @NotNull PublishPayloadLocalPersistence to, final @NotNull PersistenceType persistenceType) {

        from.iterate((id, payload) -> {
            if (payload == null) {
                return;
            }
            to.put(id, payload);
        });

        savePersistenceType(persistenceType);

        //we must init rocks db again to set correct maxID for publish payload persistence.
        to.init();
        from.closeDB();
    }

    private void savePersistenceType(final @NotNull PersistenceType persistenceType) {
        final MetaInformation metaFile = MetaFileService.readMetaFile(systemInformation);
        metaFile.setPublishPayloadPersistenceType(persistenceType);
        metaFile.setPublishPayloadPersistenceVersion(persistenceType == FILE_NATIVE ? PublishPayloadRocksDBLocalPersistence.PERSISTENCE_VERSION : PublishPayloadXodusLocalPersistence.PERSISTENCE_VERSION);
        MetaFileService.writeMetaFile(systemInformation, metaFile);
    }

}
