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

package com.hivemq.migration.persistence;

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.migration.MigrationUnit;
import com.hivemq.migration.Migrations;
import com.hivemq.migration.TypeMigration;
import com.hivemq.migration.ValueMigration;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.migration.persistence.payload.PublishPayloadTypeMigration;
import com.hivemq.migration.persistence.queue.ClientQueuePayloadIDMigration;
import com.hivemq.migration.persistence.retained.RetainedMessagePayloadIDMigration;
import com.hivemq.migration.persistence.retained.RetainedMessageTypeMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.Set;

/**
 * @author Florian Limpöck
 * @author Lukas Brandl
 */
@LazySingleton
public class PersistenceMigrator {

    private static final Logger log = LoggerFactory.getLogger(PersistenceMigrator.class);
    private static final Logger migrationlog = LoggerFactory.getLogger(Migrations.MIGRATION_LOGGER_NAME);

    private final @NotNull Provider<PublishPayloadTypeMigration> publishPayloadMigrationProvider;
    private final @NotNull Provider<RetainedMessageTypeMigration> retainedMessageMigrationProvider;
    private final @NotNull Provider<RetainedMessagePayloadIDMigration> retainedMessagePayloadIDMigrationProvider;
    private final @NotNull Provider<ClientQueuePayloadIDMigration> clientQueuePayloadIDMigrationProvider;

    @Inject
    public PersistenceMigrator(
            final @NotNull Provider<PublishPayloadTypeMigration> publishPayloadMigrationProvider,
            final @NotNull Provider<RetainedMessageTypeMigration> retainedMessageMigrationProvider,
            final @NotNull Provider<RetainedMessagePayloadIDMigration> retainedMessagePayloadIDMigrationProvider,
            final @NotNull Provider<ClientQueuePayloadIDMigration> clientQueuePayloadIDMigrationProvider) {
        this.publishPayloadMigrationProvider = publishPayloadMigrationProvider;
        this.retainedMessageMigrationProvider = retainedMessageMigrationProvider;
        this.retainedMessagePayloadIDMigrationProvider = retainedMessagePayloadIDMigrationProvider;
        this.clientQueuePayloadIDMigrationProvider = clientQueuePayloadIDMigrationProvider;
    }

    public void migratePersistenceTypes(final Map<MigrationUnit, PersistenceType> migrations) {

        final long start = System.currentTimeMillis();
        migrationlog.info("Start File Persistence migration.");
        log.info("Migrating File Persistences (this can take a few minutes).");

        for (final Map.Entry<MigrationUnit, PersistenceType> migration : migrations.entrySet()) {

            final TypeMigration migrator;

            final MigrationUnit migrationUnit = migration.getKey();
            final PersistenceType persistenceType = migration.getValue();

            switch (migrationUnit) {
                case FILE_PERSISTENCE_PUBLISH_PAYLOAD:
                    migrator = publishPayloadMigrationProvider.get();
                    break;
                case FILE_PERSISTENCE_RETAINED_MESSAGES:
                    migrator = retainedMessageMigrationProvider.get();
                    break;
                default:
                    continue;
            }

            final long startOne = System.currentTimeMillis();
            migrationlog.info("Migrating {} to type {}.", migrationUnit, persistenceType);
            log.debug("Migrating {} to type {}.", migrationUnit, persistenceType);

            migrator.migrateToType(persistenceType);

            migrationlog.info(
                    "Migrated {} to type {} successfully in {} ms",
                    migrationUnit,
                    persistenceType,
                    (System.currentTimeMillis() - startOne));
            log.debug(
                    "Migrated {} to type {} successfully in {} ms",
                    migrationUnit,
                    persistenceType,
                    (System.currentTimeMillis() - startOne));
        }

        log.info("File Persistences successfully migrated in " + (System.currentTimeMillis() - start) + " ms");
        migrationlog.info("File Persistences successfully migrated in " + (System.currentTimeMillis() - start) + " ms");

    }

    public void closeAllLegacyPersistences() {
        retainedMessagePayloadIDMigrationProvider.get().closeLegacy();
        clientQueuePayloadIDMigrationProvider.get().closeLegacy();
    }

    public void migratePersistenceValues(final @NotNull Set<MigrationUnit> valueMigrations) {

        for (final MigrationUnit migrationUnit : valueMigrations) {
            final ValueMigration migrator;
            switch (migrationUnit) {
                case PAYLOAD_ID_RETAINED_MESSAGES:
                    migrator = retainedMessagePayloadIDMigrationProvider.get();
                    break;
                case PAYLOAD_ID_CLIENT_QUEUE:
                    migrator = clientQueuePayloadIDMigrationProvider.get();
                    break;
                default:
                    continue;
            }
            final long startOne = System.currentTimeMillis();
            migrationlog.info("Migrating {}.", migrationUnit);
            log.debug("Migrating {}.", migrationUnit);

            migrator.migrateToValue();

            migrationlog.info(
                    "Migrated {} successfully in {} ms",
                    migrationUnit,
                    (System.currentTimeMillis() - startOne));
            log.debug("Migrated {} successfully in {} ms", migrationUnit, (System.currentTimeMillis() - startOne));
        }

    }

}
