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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.ioc.GuiceBootstrap;
import com.hivemq.configuration.ConfigurationBootstrap;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.migration.MigrationUnit;
import com.hivemq.migration.Migrations;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadRocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadXodusLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 */
@SuppressWarnings("NullabilityAnnotations")
public class PublishPayloadTypeMigrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SystemInformation systemInformation;

    private File dataFolder;
    private FullConfigurationService configurationService;

    @Before
    public void before() throws IOException {
        MockitoAnnotations.initMocks(this);
        dataFolder = temporaryFolder.newFolder();
        when(systemInformation.getDataFolder()).thenReturn(dataFolder);
        when(systemInformation.getConfigFolder()).thenReturn(temporaryFolder.newFolder());
        when(systemInformation.getHiveMQVersion()).thenReturn("2019.2");

        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(4);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(4);
        final File file = new File(dataFolder, LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME);
        file.mkdirs();
        new File(file, RetainedMessageLocalPersistence.PERSISTENCE_NAME).mkdir();
        new File(file, PublishPayloadLocalPersistence.PERSISTENCE_NAME).mkdir();

        configurationService = ConfigurationBootstrap.bootstrapConfig(systemInformation);
    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);
        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(64);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(64);
        FileUtils.forceDelete(dataFolder);
    }

    @Test
    public void test_payload_migration_xodus_to_rocks() throws Exception {


        final Map<MigrationUnit, PersistenceType>
                migrations = Migrations.checkForTypeMigration(systemInformation);

        assertEquals(2, migrations.size());
        assertEquals(PersistenceType.FILE_NATIVE, migrations.get(MigrationUnit.FILE_PERSISTENCE_PUBLISH_PAYLOAD));

        final Injector persistenceInjector =
                GuiceBootstrap.persistenceInjector(systemInformation, new MetricRegistry(), new HivemqId(), configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final PublishPayloadXodusLocalPersistence xodus = persistenceInjector.getInstance(PublishPayloadXodusLocalPersistence.class);
        for (int i = 0; i < 1000; i++) {
            xodus.put(i, ("message" + i).getBytes());
        }

        Migrations.migrate(persistenceInjector, migrations, ImmutableSet.of());

        final PublishPayloadRocksDBLocalPersistence rocks = persistenceInjector.getInstance(PublishPayloadRocksDBLocalPersistence.class);
        for (int i = 0; i < 1000; i++) {
            assertEquals("message" + i, new String(rocks.get(i)));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(PublishPayloadRocksDBLocalPersistence.PERSISTENCE_VERSION, metaInformation.getPublishPayloadPersistenceVersion());
        assertEquals(PersistenceType.FILE_NATIVE, metaInformation.getPublishPayloadPersistenceType());
    }

    @Test
    public void test_payload_migration_rocks_to_xodus() throws Exception {


        MetaFileService.writeMetaFile(systemInformation, getMetaInformation());
        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE);

        final Map<MigrationUnit, PersistenceType> migrations = Migrations.checkForTypeMigration(systemInformation);

        assertEquals(1, migrations.size());
        assertEquals(PersistenceType.FILE, migrations.get(MigrationUnit.FILE_PERSISTENCE_PUBLISH_PAYLOAD));

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation, new MetricRegistry(), new HivemqId(), configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final PublishPayloadRocksDBLocalPersistence rocks = persistenceInjector.getInstance(PublishPayloadRocksDBLocalPersistence.class);
        for (int i = 0; i < 1000; i++) {
            rocks.put(i, ("message" + i).getBytes());
        }

        Migrations.migrate(persistenceInjector, migrations, ImmutableSet.of());

        final PublishPayloadXodusLocalPersistence xodus = persistenceInjector.getInstance(PublishPayloadXodusLocalPersistence.class);
        for (int i = 0; i < 1000; i++) {
            assertEquals("message" + i, new String(xodus.get(i)));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(PublishPayloadXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getPublishPayloadPersistenceVersion());
        assertEquals(PersistenceType.FILE, metaInformation.getPublishPayloadPersistenceType());

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);
    }

    @Test
    public void test_payload_migration_stays_xodus() throws Exception {

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE);

        final Map<MigrationUnit, PersistenceType> migrations = Migrations.checkForTypeMigration(systemInformation);

        assertEquals(1, migrations.size());
        assertFalse(migrations.containsKey(MigrationUnit.FILE_PERSISTENCE_PUBLISH_PAYLOAD));

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation, new MetricRegistry(), new HivemqId(), configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final PublishPayloadXodusLocalPersistence persistence_4_2_x = persistenceInjector.getInstance(PublishPayloadXodusLocalPersistence.class);
        for (int i = 0; i < 10; i++) {
            persistence_4_2_x.put(i, ("message" + i).getBytes());
        }

        Migrations.migrate(persistenceInjector, migrations, ImmutableSet.of());
        Migrations.afterMigration(systemInformation);

        //closing persistences
        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(PublishPayloadXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getPublishPayloadPersistenceVersion());
        assertEquals(PersistenceType.FILE, metaInformation.getPublishPayloadPersistenceType());
        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);

    }

    @NotNull
    private MetaInformation getMetaInformation() {
        final MetaInformation metaInformation = new MetaInformation();

        metaInformation.setHivemqVersion("2019.1");
        metaInformation.setPublishPayloadPersistenceType(PersistenceType.FILE_NATIVE);
        metaInformation.setRetainedMessagesPersistenceType(PersistenceType.FILE_NATIVE);
        metaInformation.setDataFolderPresent(true);
        metaInformation.setMetaFilePresent(true);
        metaInformation.setPersistenceFolderPresent(true);

        return metaInformation;
    }

}