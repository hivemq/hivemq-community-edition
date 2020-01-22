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

package com.hivemq.migration.persistence;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.migration.MigrationUnit;
import com.hivemq.migration.Migrations;
import com.hivemq.migration.TypeMigration;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.migration.persistence.payload.PublishPayloadTypeMigration;
import com.hivemq.migration.persistence.retained.RetainedMessageTypeMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class PersistenceMigrator {

    private static final Logger log = LoggerFactory.getLogger(PersistenceMigrator.class);
    private static final Logger migrationlog = LoggerFactory.getLogger(Migrations.MIGRATION_LOGGER_NAME);

    private final @NotNull Provider<PublishPayloadTypeMigration> publishPayloadMigrationProvider;
    private final @NotNull Provider<RetainedMessageTypeMigration> retainedMessageMigrationProvider;

    @Inject
    public PersistenceMigrator(final @NotNull Provider<PublishPayloadTypeMigration> publishPayloadMigrationProvider,
                               final @NotNull Provider<RetainedMessageTypeMigration> retainedMessageMigrationProvider) {
        this.publishPayloadMigrationProvider = publishPayloadMigrationProvider;
        this.retainedMessageMigrationProvider = retainedMessageMigrationProvider;
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

            migrationlog.info("Migrated {} to type {} successfully in {} ms", migrationUnit, persistenceType, (System.currentTimeMillis() - startOne));
            log.debug("Migrated {} to type {} successfully in {} ms", migrationUnit, persistenceType, (System.currentTimeMillis() - startOne));
        }

        log.info("File Persistences successfully migrated in " + (System.currentTimeMillis() - start) + " ms");
        migrationlog.info("File Persistences successfully migrated in " + (System.currentTimeMillis() - start) + " ms");

    }

}
