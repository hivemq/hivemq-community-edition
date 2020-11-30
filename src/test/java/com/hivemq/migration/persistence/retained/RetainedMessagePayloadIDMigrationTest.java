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

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.GuiceBootstrap;
import com.hivemq.configuration.ConfigurationBootstrap;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.migration.MigrationUnit;
import com.hivemq.migration.Migrations;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.migration.persistence.legacy.PublishPayloadRocksDBLocalPersistence_4_4;
import com.hivemq.migration.persistence.legacy.PublishPayloadXodusLocalPersistence_4_4;
import com.hivemq.migration.persistence.legacy.RetainedMessageRocksDBLocalPersistence_4_4;
import com.hivemq.migration.persistence.legacy.RetainedMessageXodusLocalPersistence_4_4;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.migration.meta.PersistenceType.FILE;
import static com.hivemq.migration.meta.PersistenceType.FILE_NATIVE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 */
@SuppressWarnings("NullabilityAnnotations")
public class RetainedMessagePayloadIDMigrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SystemInformation systemInformation;

    private File dataFolder;
    private FullConfigurationService configurationService;
    private File retainedFolder;
    private File payloadFolder;

    @Before
    public void before() throws IOException {
        MockitoAnnotations.initMocks(this);
        dataFolder = temporaryFolder.newFolder();
        when(systemInformation.getDataFolder()).thenReturn(dataFolder);
        when(systemInformation.getConfigFolder()).thenReturn(temporaryFolder.newFolder());
        when(systemInformation.getHiveMQVersion()).thenReturn("2020.7");
        configurationService = ConfigurationBootstrap.bootstrapConfig(systemInformation);
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(4);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(4);
        final File file = new File(dataFolder, LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME);
        file.mkdirs();
        retainedFolder = new File(file, RetainedMessageLocalPersistence.PERSISTENCE_NAME);
        retainedFolder.mkdir();
        payloadFolder = new File(file, PublishPayloadLocalPersistence.PERSISTENCE_NAME);
        payloadFolder.mkdir();

    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(64);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(64);
        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(FILE_NATIVE);
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(FILE_NATIVE);
        FileUtils.forceDelete(dataFolder);
    }

    @Test
    public void test_retained_and_payload_xodus_44_to_rocksdb_45() throws Exception {

        new File(retainedFolder, RetainedMessageXodusLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();
        new File(payloadFolder, PublishPayloadXodusLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();

        final Map<MigrationUnit, PersistenceType> typeMigrations = Migrations.checkForTypeMigration(systemInformation);
        final Set<MigrationUnit> migrations = Migrations.checkForValueMigration(systemInformation);

        assertEquals(2, typeMigrations.size());
        assertEquals(1, migrations.size());

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation, new MetricRegistry(), new HivemqId(), configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final RetainedMessageXodusLocalPersistence_4_4 retainedLegacy = persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence_4_4.class);
        final PublishPayloadXodusLocalPersistence_4_4 payloadLegacy = persistenceInjector.getInstance(PublishPayloadXodusLocalPersistence_4_4.class);

        for (int i = 0; i < 1000; i++) {
            payloadLegacy.put(i, ("message" + i).getBytes());
            retainedLegacy.put(new RetainedMessage(("message" + i).getBytes(), QoS.AT_LEAST_ONCE, i, i + 1000), "topic" + i, BucketUtils.getBucket("topic" + i, retainedLegacy.getBucketCount()));
        }

        Migrations.migrate(persistenceInjector, typeMigrations, migrations);

        final RetainedMessageRocksDBLocalPersistence persistence = persistenceInjector.getInstance(RetainedMessageRocksDBLocalPersistence.class);
        assertEquals(1000, persistence.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals("message" + i, new String(persistence.get("topic" + i, BucketUtils.getBucket("topic" + i, 4)).getMessage()));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(RetainedMessageRocksDBLocalPersistence.PERSISTENCE_VERSION, metaInformation.getRetainedMessagesPersistenceVersion());

        final PublishPayloadPersistence payloadPersistence = persistenceInjector.getInstance(PublishPayloadPersistence.class);
        assertEquals(1000, payloadPersistence.getReferenceCountersAsMap().values().stream().mapToLong(AtomicLong::get).sum());
    }


    @Test
    public void test_retained_and_payload_rocksdb_44_to_xodus_45() throws Exception {

        MetaFileService.writeMetaFile(systemInformation, getMetaInformation(FILE_NATIVE, FILE_NATIVE));

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(FILE);
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(FILE);

        new File(retainedFolder, RetainedMessageRocksDBLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();
        new File(payloadFolder, PublishPayloadRocksDBLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();

        final Map<MigrationUnit, PersistenceType> typeMigrations = Migrations.checkForTypeMigration(systemInformation);
        final Set<MigrationUnit> migrations = Migrations.checkForValueMigration(systemInformation);

        assertEquals(2, typeMigrations.size());
        assertEquals(1, migrations.size());

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation, new MetricRegistry(), new HivemqId(), configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final PublishPayloadRocksDBLocalPersistence_4_4 payloadLegacy = persistenceInjector.getInstance(PublishPayloadRocksDBLocalPersistence_4_4.class);
        final RetainedMessageRocksDBLocalPersistence_4_4 retainedLegacy = persistenceInjector.getInstance(RetainedMessageRocksDBLocalPersistence_4_4.class);

        for (int i = 0; i < 1000; i++) {
            payloadLegacy.put(i, ("message" + i).getBytes());
            retainedLegacy.put(new RetainedMessage(("message" + i).getBytes(), QoS.AT_LEAST_ONCE, i, i + 1000), "topic" + i, BucketUtils.getBucket("topic" + i, retainedLegacy.getBucketCount()));
        }

        Migrations.migrate(persistenceInjector, typeMigrations, migrations);

        final RetainedMessageXodusLocalPersistence persistence = persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence.class);
        assertEquals(1000, persistence.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals("message" + i, new String(persistence.get("topic" + i, BucketUtils.getBucket("topic" + i, 4)).getMessage()));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getRetainedMessagesPersistenceVersion());
        assertEquals(PublishPayloadXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getPublishPayloadPersistenceVersion());

        final PublishPayloadPersistence payloadPersistence = persistenceInjector.getInstance(PublishPayloadPersistence.class);
        assertEquals(1000, payloadPersistence.getReferenceCountersAsMap().values().stream().mapToLong(AtomicLong::get).sum());

    }

    @Test
    public void test_retained_rocksdb_44_to_xodus_45() throws Exception {

        MetaFileService.writeMetaFile(systemInformation, getMetaInformation(FILE, FILE_NATIVE));

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(FILE);
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(FILE);

        new File(retainedFolder, RetainedMessageRocksDBLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();
        new File(payloadFolder, PublishPayloadXodusLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();

        final Map<MigrationUnit, PersistenceType> typeMigrations = Migrations.checkForTypeMigration(systemInformation);
        final Set<MigrationUnit> migrations = Migrations.checkForValueMigration(systemInformation);

        assertEquals(1, typeMigrations.size());
        assertEquals(1, migrations.size());

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation, new MetricRegistry(), new HivemqId(), configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final PublishPayloadXodusLocalPersistence_4_4 payloadLegacy = persistenceInjector.getInstance(PublishPayloadXodusLocalPersistence_4_4.class);
        final RetainedMessageRocksDBLocalPersistence_4_4 retainedLegacy = persistenceInjector.getInstance(RetainedMessageRocksDBLocalPersistence_4_4.class);

        for (int i = 0; i < 1000; i++) {
            payloadLegacy.put(i, ("message" + i).getBytes());
            retainedLegacy.put(new RetainedMessage(("message" + i).getBytes(), QoS.AT_LEAST_ONCE, i, i + 1000), "topic" + i, BucketUtils.getBucket("topic" + i, retainedLegacy.getBucketCount()));
        }

        Migrations.migrate(persistenceInjector, typeMigrations, migrations);

        final RetainedMessageXodusLocalPersistence persistence = persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence.class);
        assertEquals(1000, persistence.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals("message" + i, new String(persistence.get("topic" + i, BucketUtils.getBucket("topic" + i, 4)).getMessage()));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getRetainedMessagesPersistenceVersion());

        final PublishPayloadPersistence payloadPersistence = persistenceInjector.getInstance(PublishPayloadPersistence.class);
        assertEquals(1000, payloadPersistence.getReferenceCountersAsMap().values().stream().mapToLong(AtomicLong::get).sum());
    }

    @Test
    public void test_payload_rocksdb_44_to_xodus_45() throws Exception {

        MetaFileService.writeMetaFile(systemInformation, getMetaInformation(FILE_NATIVE, FILE));

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(FILE);
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(FILE);

        new File(retainedFolder, RetainedMessageXodusLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();
        new File(payloadFolder, PublishPayloadRocksDBLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();

        final Map<MigrationUnit, PersistenceType> typeMigrations = Migrations.checkForTypeMigration(systemInformation);
        final Set<MigrationUnit> migrations = Migrations.checkForValueMigration(systemInformation);

        assertEquals(1, typeMigrations.size());
        assertEquals(1, migrations.size());

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation, new MetricRegistry(), new HivemqId(), configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final PublishPayloadRocksDBLocalPersistence_4_4 payloadLegacy = persistenceInjector.getInstance(PublishPayloadRocksDBLocalPersistence_4_4.class);
        final RetainedMessageXodusLocalPersistence_4_4 retainedLegacy = persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence_4_4.class);

        for (int i = 0; i < 1000; i++) {
            payloadLegacy.put(i, ("message" + i).getBytes());
            retainedLegacy.put(new RetainedMessage(("message" + i).getBytes(), QoS.AT_LEAST_ONCE, i, i + 1000), "topic" + i, BucketUtils.getBucket("topic" + i, retainedLegacy.getBucketCount()));
        }

        Migrations.migrate(persistenceInjector, typeMigrations, migrations);

        final RetainedMessageXodusLocalPersistence persistence = persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence.class);
        assertEquals(1000, persistence.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals("message" + i, new String(persistence.get("topic" + i, BucketUtils.getBucket("topic" + i, 4)).getMessage()));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getRetainedMessagesPersistenceVersion());
        assertEquals(PublishPayloadXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getPublishPayloadPersistenceVersion());

        final PublishPayloadPersistence payloadPersistence = persistenceInjector.getInstance(PublishPayloadPersistence.class);
        assertEquals(1000, payloadPersistence.getReferenceCountersAsMap().values().stream().mapToLong(AtomicLong::get).sum());
    }

    @Test
    public void test_retained_and_payload_stay_xodus() throws Exception {

        MetaFileService.writeMetaFile(systemInformation, getMetaInformation(FILE, FILE));

        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(FILE);
        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(FILE);

        new File(retainedFolder, RetainedMessageXodusLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();
        new File(payloadFolder, PublishPayloadXodusLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();

        final Map<MigrationUnit, PersistenceType> typeMigrations = Migrations.checkForTypeMigration(systemInformation);
        final Set<MigrationUnit> migrations = Migrations.checkForValueMigration(systemInformation);

        assertEquals(0, typeMigrations.size());
        assertEquals(1, migrations.size());

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation, new MetricRegistry(), new HivemqId(), configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final RetainedMessageXodusLocalPersistence_4_4 retainedLegacy = persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence_4_4.class);
        final PublishPayloadXodusLocalPersistence_4_4 payloadLegacy = persistenceInjector.getInstance(PublishPayloadXodusLocalPersistence_4_4.class);

        for (int i = 0; i < 1000; i++) {
            payloadLegacy.put(i, ("message" + i).getBytes());
            retainedLegacy.put(new RetainedMessage(("message" + i).getBytes(), QoS.AT_LEAST_ONCE, i, i + 1000), "topic" + i, BucketUtils.getBucket("topic" + i, retainedLegacy.getBucketCount()));
        }

        Migrations.migrate(persistenceInjector, typeMigrations, migrations);

        final RetainedMessageXodusLocalPersistence persistence = persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence.class);
        assertEquals(1000, persistence.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals("message" + i, new String(persistence.get("topic" + i, BucketUtils.getBucket("topic" + i, 4)).getMessage()));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getRetainedMessagesPersistenceVersion());
        assertEquals(PublishPayloadXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getPublishPayloadPersistenceVersion());

        final PublishPayloadPersistence payloadPersistence = persistenceInjector.getInstance(PublishPayloadPersistence.class);
        assertEquals(1000, payloadPersistence.getReferenceCountersAsMap().values().stream().mapToLong(AtomicLong::get).sum());

    }

    @Test
    public void test_retained_and_payload_stay_rocksdb() throws Exception {

        MetaFileService.writeMetaFile(systemInformation, getMetaInformation(FILE_NATIVE, FILE_NATIVE));

        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(FILE_NATIVE);
        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(FILE_NATIVE);

        new File(retainedFolder, RetainedMessageRocksDBLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();
        new File(payloadFolder, PublishPayloadRocksDBLocalPersistence_4_4.PERSISTENCE_VERSION).mkdir();

        final Map<MigrationUnit, PersistenceType> typeMigrations = Migrations.checkForTypeMigration(systemInformation);
        final Set<MigrationUnit> migrations = Migrations.checkForValueMigration(systemInformation);

        assertEquals(0, typeMigrations.size());
        assertEquals(1, migrations.size());

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation, new MetricRegistry(), new HivemqId(), configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final RetainedMessageRocksDBLocalPersistence_4_4 retainedLegacy = persistenceInjector.getInstance(RetainedMessageRocksDBLocalPersistence_4_4.class);
        final PublishPayloadRocksDBLocalPersistence_4_4 payloadLegacy = persistenceInjector.getInstance(PublishPayloadRocksDBLocalPersistence_4_4.class);

        for (int i = 0; i < 1000; i++) {
            payloadLegacy.put(i, ("message" + i).getBytes());
            retainedLegacy.put(new RetainedMessage(("message" + i).getBytes(), QoS.AT_LEAST_ONCE, i, i + 1000), "topic" + i, BucketUtils.getBucket("topic" + i, retainedLegacy.getBucketCount()));
        }

        Migrations.migrate(persistenceInjector, typeMigrations, migrations);

        final RetainedMessageRocksDBLocalPersistence persistence = persistenceInjector.getInstance(RetainedMessageRocksDBLocalPersistence.class);
        assertEquals(1000, persistence.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals("message" + i, new String(persistence.get("topic" + i, BucketUtils.getBucket("topic" + i, 4)).getMessage()));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(RetainedMessageRocksDBLocalPersistence.PERSISTENCE_VERSION, metaInformation.getRetainedMessagesPersistenceVersion());
        assertEquals(PublishPayloadRocksDBLocalPersistence.PERSISTENCE_VERSION, metaInformation.getPublishPayloadPersistenceVersion());

        final PublishPayloadPersistence payloadPersistence = persistenceInjector.getInstance(PublishPayloadPersistence.class);
        assertEquals(1000, payloadPersistence.getReferenceCountersAsMap().values().stream().mapToLong(AtomicLong::get).sum());

    }

    @NotNull
    private MetaInformation getMetaInformation(PersistenceType payload, PersistenceType retained) {
        final MetaInformation metaInformation = new MetaInformation();

        metaInformation.setHivemqVersion("4.3.0");
        metaInformation.setPublishPayloadPersistenceType(payload);
        metaInformation.setRetainedMessagesPersistenceType(retained);
        metaInformation.setDataFolderPresent(true);
        metaInformation.setMetaFilePresent(true);
        metaInformation.setPersistenceFolderPresent(true);

        return metaInformation;
    }
}