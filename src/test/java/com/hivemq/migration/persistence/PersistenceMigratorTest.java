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

import com.google.common.collect.ImmutableMap;
import com.hivemq.migration.MigrationUnit;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.migration.persistence.payload.PublishPayloadTypeMigration;
import com.hivemq.migration.persistence.queue.ClientQueuePayloadIDMigration;
import com.hivemq.migration.persistence.retained.RetainedMessagePayloadIDMigration;
import com.hivemq.migration.persistence.retained.RetainedMessageTypeMigration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 */
public class PersistenceMigratorTest {

    PersistenceMigrator persistenceMigrator;
    @Mock
    private PublishPayloadTypeMigration publishPayloadTypeMigration;
    @Mock
    private RetainedMessageTypeMigration retainedMessageTypeMigration;
    @Mock
    private ClientQueuePayloadIDMigration clientQueuePayloadIDMigration;
    @Mock
    private RetainedMessagePayloadIDMigration retainedMessagePayloadIDMigration;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        persistenceMigrator = new PersistenceMigrator(() -> publishPayloadTypeMigration, () -> retainedMessageTypeMigration,
                () -> retainedMessagePayloadIDMigration, () -> clientQueuePayloadIDMigration);
    }

    @Test
    public void test_migrate_all_to_native() {

        persistenceMigrator.migratePersistenceTypes(ImmutableMap.of(
                MigrationUnit.FILE_PERSISTENCE_PUBLISH_PAYLOAD, PersistenceType.FILE_NATIVE,
                MigrationUnit.FILE_PERSISTENCE_RETAINED_MESSAGES, PersistenceType.FILE_NATIVE));


        verify(publishPayloadTypeMigration).migrateToType(PersistenceType.FILE_NATIVE);
        verify(retainedMessageTypeMigration).migrateToType(PersistenceType.FILE_NATIVE);

    }

    @Test
    public void test_migrate_all_to_file() {

        persistenceMigrator.migratePersistenceTypes(ImmutableMap.of(
                MigrationUnit.FILE_PERSISTENCE_PUBLISH_PAYLOAD, PersistenceType.FILE,
                MigrationUnit.FILE_PERSISTENCE_RETAINED_MESSAGES, PersistenceType.FILE));

        verify(publishPayloadTypeMigration).migrateToType(PersistenceType.FILE);
        verify(retainedMessageTypeMigration).migrateToType(PersistenceType.FILE);

    }

    @Test
    public void test_migrate_publish_to_file_and_retained_to_native() {

        persistenceMigrator.migratePersistenceTypes(ImmutableMap.of(
                MigrationUnit.FILE_PERSISTENCE_PUBLISH_PAYLOAD, PersistenceType.FILE,
                MigrationUnit.FILE_PERSISTENCE_RETAINED_MESSAGES, PersistenceType.FILE_NATIVE));

        verify(publishPayloadTypeMigration).migrateToType(PersistenceType.FILE);
        verify(retainedMessageTypeMigration).migrateToType(PersistenceType.FILE_NATIVE);

    }

    @Test
    public void test_migrate_publish_to_nativr_and_retained_to_file() {

        persistenceMigrator.migratePersistenceTypes(ImmutableMap.of(
                MigrationUnit.FILE_PERSISTENCE_PUBLISH_PAYLOAD, PersistenceType.FILE_NATIVE,
                MigrationUnit.FILE_PERSISTENCE_RETAINED_MESSAGES, PersistenceType.FILE));

        verify(publishPayloadTypeMigration).migrateToType(PersistenceType.FILE_NATIVE);
        verify(retainedMessageTypeMigration).migrateToType(PersistenceType.FILE);

    }
}