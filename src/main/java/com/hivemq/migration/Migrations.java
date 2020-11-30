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
package com.hivemq.migration;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.migration.persistence.PersistenceMigrator;
import com.hivemq.persistence.clientqueue.ClientQueueXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static com.hivemq.configuration.info.SystemInformationImpl.DEVELOPMENT_VERSION;

/**
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
public class Migrations {

    public static final String MIGRATION_LOGGER_NAME = "migrations";

    private static final Logger log = LoggerFactory.getLogger(Migrations.class);
    private static final Logger MIGRATION_LOGGER = LoggerFactory.getLogger(MIGRATION_LOGGER_NAME);

    @NotNull
    public static Map<MigrationUnit, PersistenceType> checkForTypeMigration(final @NotNull SystemInformation systemInformation) {

        MIGRATION_LOGGER.info("Checking for migrations (HiveMQ version {}).", systemInformation.getHiveMQVersion());

        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);

        if (systemInformation.getHiveMQVersion().equals(DEVELOPMENT_VERSION)) {
            MIGRATION_LOGGER.info("Skipping migration because it is a Development Snapshot.");
            return Collections.emptyMap();
        }

        if (!metaInformation.isDataFolderPresent()) {
            log.trace("No data folder present, skip migrations.");
            MIGRATION_LOGGER.info("Skipping migration because no data folder is present.");
            return Collections.emptyMap();
        }

        if (!metaInformation.isPersistenceFolderPresent()) {
            log.trace("No persistence folder present, skip migrations.");
            MIGRATION_LOGGER.info("Skipping migration because no persistence folder is present.");
            return Collections.emptyMap();
        }

        final PersistenceType previousRetainedType;
        final PersistenceType previousPayloadType;
        if (!metaInformation.isMetaFilePresent()) {
            log.trace("No meta file present, assuming HiveMQ version 2019.1 => Migration needed.");
            MIGRATION_LOGGER.info("No meta file present, assuming HiveMQ version 2019.1 => Migration needed.");
            previousPayloadType = PersistenceType.FILE;
            previousRetainedType = PersistenceType.FILE;
            final MetaInformation newMetaInformation = new MetaInformation();
            newMetaInformation.setPublishPayloadPersistenceType(previousPayloadType);
            newMetaInformation.setRetainedMessagesPersistenceType(previousRetainedType);
            MetaFileService.writeMetaFile(systemInformation, newMetaInformation);
        } else {
            Preconditions.checkNotNull(metaInformation.getRetainedMessagesPersistenceType());
            Preconditions.checkNotNull(metaInformation.getPublishPayloadPersistenceType());
            previousRetainedType = metaInformation.getRetainedMessagesPersistenceType();
            previousPayloadType = metaInformation.getPublishPayloadPersistenceType();
        }

        final PersistenceType currentRetainedType = InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get();
        final PersistenceType currentPayloadType = InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.get();

        final Map<MigrationUnit, PersistenceType> neededMigrations = new EnumMap<>(MigrationUnit.class);

        if (!previousPayloadType.equals(currentPayloadType) && isPreviousPersistenceExistent(systemInformation, PublishPayloadLocalPersistence.PERSISTENCE_NAME)) {
            neededMigrations.put(MigrationUnit.FILE_PERSISTENCE_PUBLISH_PAYLOAD, currentPayloadType);
        }
        if (!previousRetainedType.equals(currentRetainedType) && isPreviousPersistenceExistent(systemInformation, RetainedMessageLocalPersistence.PERSISTENCE_NAME)) {
            neededMigrations.put(MigrationUnit.FILE_PERSISTENCE_RETAINED_MESSAGES, currentRetainedType);
        }

        if (neededMigrations.isEmpty()) {
            MIGRATION_LOGGER.info("Nothing to migrate found.");
        } else {
            MIGRATION_LOGGER.info("Found following needed migrations: {}", neededMigrations);
        }

        return neededMigrations;
    }


    public static Set<MigrationUnit> checkForValueMigration(final @NotNull SystemInformation systemInformation) {
        MIGRATION_LOGGER.info("Checking for value migrations (HiveMQ version {}).", systemInformation.getHiveMQVersion());

        if (systemInformation.getHiveMQVersion().equals(DEVELOPMENT_VERSION)) {
            MIGRATION_LOGGER.info("Skipping migration because it is a Development Snapshot.");
            return Collections.emptySet();
        }

        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);

        if (!metaInformation.isDataFolderPresent()) {
            log.trace("No data folder present, skip migrations.");
            MIGRATION_LOGGER.info("Skipping migration because no data folder is present.");
            return Collections.emptySet();
        }

        if (!metaInformation.isPersistenceFolderPresent()) {
            log.trace("No persistence folder present, skip migrations.");
            MIGRATION_LOGGER.info("Skipping migration because no persistence folder is present.");
            return Collections.emptySet();
        }

        final Set<MigrationUnit> neededMigrations = new TreeSet<>();
        if(retainedNeeded(metaInformation, systemInformation)){
            neededMigrations.add(MigrationUnit.PAYLOAD_ID_RETAINED_MESSAGES);
        }
        if(queuedNeeded(metaInformation, systemInformation)){
            neededMigrations.add(MigrationUnit.PAYLOAD_ID_CLIENT_QUEUE);
        }

        if (neededMigrations.isEmpty()) {
            MIGRATION_LOGGER.info("Nothing to migrate found.");
        } else {
            MIGRATION_LOGGER.info("Found following needed migrations: {}", neededMigrations);
        }

        return neededMigrations;
    }

    private static boolean retainedNeeded(final @NotNull MetaInformation metaInformation,
            final @NotNull SystemInformation systemInformation) {

        final String previousRetainedVersion;
        if(metaInformation.getRetainedMessagesPersistenceVersion() == null){
            previousRetainedVersion = "NOT_SET";
        } else {
            previousRetainedVersion = metaInformation.getRetainedMessagesPersistenceVersion();
        }

        final PersistenceType currentRetainedType = InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get();
        final String currentRetainedVersion;
        if (currentRetainedType == PersistenceType.FILE){
            currentRetainedVersion = RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION;
        } else {
            currentRetainedVersion = RetainedMessageRocksDBLocalPersistence.PERSISTENCE_VERSION;
        }

        return !previousRetainedVersion.equals(currentRetainedVersion) && isPreviousPersistenceExistent(systemInformation, RetainedMessageLocalPersistence.PERSISTENCE_NAME);
    }

    private static boolean queuedNeeded(final @NotNull MetaInformation metaInformation,
                                          final @NotNull SystemInformation systemInformation) {

        final String previousQueuedVersion;
        if(metaInformation.getQueuedMessagesPersistenceVersion() == null){
            previousQueuedVersion = "NOT_SET";
        } else {
            previousQueuedVersion = metaInformation.getQueuedMessagesPersistenceVersion();
        }

        final String currentQueuedVersion = ClientQueueXodusLocalPersistence.PERSISTENCE_VERSION;

        return !previousQueuedVersion.equals(currentQueuedVersion) && isPreviousPersistenceExistent(systemInformation, ClientQueueXodusLocalPersistence.PERSISTENCE_NAME);
    }

    private static boolean isPreviousPersistenceExistent(final @NotNull SystemInformation systemInformation, final @NotNull String persistence) {
        return new File(systemInformation.getDataFolder() + File.separator + LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME, persistence).exists();
    }

    public static void migrate(final Injector persistenceInjector, final @NotNull Map<MigrationUnit, PersistenceType> typeMigrations, final @NotNull Set<MigrationUnit> valueMigrations) {

        MIGRATION_LOGGER.info("Start migration.");

        // migrate persistences
        final PersistenceMigrator persistenceMigrator = persistenceInjector.getInstance(PersistenceMigrator.class);
        persistenceMigrator.migratePersistenceValues(valueMigrations);
        persistenceMigrator.migratePersistenceTypes(typeMigrations);
        persistenceMigrator.closeAllLegacyPersistences();

    }

    public static void afterMigration(final @NotNull SystemInformation systemInformation) {
        final MigrationFinisher finisher = new MigrationFinisher(systemInformation);
        finisher.finishMigration();

        MIGRATION_LOGGER.info("Finished migration.");
    }

}
