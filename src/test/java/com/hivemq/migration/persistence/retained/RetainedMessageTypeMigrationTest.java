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
import com.google.common.collect.ImmutableSet;
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
import com.hivemq.migration.logging.PayloadExceptionLogging;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpoeck
 */
public class RetainedMessageTypeMigrationTest {

    @Mock
    private PublishPayloadLocalPersistence payloadLocalPersistence;

    @Mock
    private RetainedMessageRocksDBLocalPersistence rocksDBLocalPersistence;

    @Mock
    private PayloadExceptionLogging payloadExceptionLogging;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SystemInformation systemInformation;

    private File dataFolder;
    private FullConfigurationService configurationService;

    private RetainedMessageTypeMigration.RetainedMessagePersistenceTypeSwitchCallback callback;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        dataFolder = temporaryFolder.newFolder();
        when(systemInformation.getDataFolder()).thenReturn(dataFolder);
        when(systemInformation.getConfigFolder()).thenReturn(temporaryFolder.newFolder());
        when(systemInformation.getHiveMQVersion()).thenReturn("2019.2");
        configurationService = ConfigurationBootstrap.bootstrapConfig(systemInformation);

        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(4);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(4);

        final File file = new File(dataFolder, LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME);
        file.mkdirs();
        new File(file, RetainedMessageLocalPersistence.PERSISTENCE_NAME).mkdir();
        new File(file, PublishPayloadLocalPersistence.PERSISTENCE_NAME).mkdir();

        callback = new RetainedMessageTypeMigration.RetainedMessagePersistenceTypeSwitchCallback(4,
                payloadLocalPersistence,
                rocksDBLocalPersistence,
                payloadExceptionLogging);

    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);
        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(64);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(64);
        FileUtils.forceDelete(dataFolder);
    }

    @Test(timeout = 5000)
    public void test_missing_payload_gets_logged() throws Exception {

        callback.onItem("topic", new RetainedMessage("message".getBytes(),
                QoS.AT_LEAST_ONCE,
                1L,
                System.currentTimeMillis(),
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                null,
                null,
                null,
                1234L));
        verify(payloadExceptionLogging, times(1)).addLogging(1L, true, "topic");

    }

    @Test(timeout = 5000)
    public void test_nothing_gets_logged_if_payload_exists() throws Exception {
        when(payloadLocalPersistence.get(1L)).thenReturn(new byte[0]);
        callback.onItem("topic", new RetainedMessage("message".getBytes(),
                QoS.AT_LEAST_ONCE,
                1L,
                System.currentTimeMillis(),
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                null,
                null,
                null,
                1234L));
        verify(payloadExceptionLogging, never()).addLogging(anyLong(), any(), any());
    }

    @Test
    public void test_retained_xodus_to_rocksdb() throws Exception {

        final Map<MigrationUnit, PersistenceType> migrations = Migrations.checkForTypeMigration(systemInformation);

        assertEquals(2, migrations.size());
        assertEquals(PersistenceType.FILE_NATIVE, migrations.get(MigrationUnit.FILE_PERSISTENCE_RETAINED_MESSAGES));

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation,
                new MetricRegistry(),
                new HivemqId(),
                configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final RetainedMessageXodusLocalPersistence xodus =
                persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence.class);
        final PublishPayloadLocalPersistence payload =
                persistenceInjector.getInstance(PublishPayloadLocalPersistence.class);

        for (int i = 0; i < 1000; i++) {
            payload.put(i, ("message" + i).getBytes());
            xodus.put(new RetainedMessage(("message" + i).getBytes(), QoS.AT_LEAST_ONCE, (long) i, i + 1000),
                    "topic" + i,
                    BucketUtils.getBucket("topic" + i, xodus.getBucketCount()));
        }

        Migrations.migrate(persistenceInjector, migrations, ImmutableSet.of());

        final RetainedMessageRocksDBLocalPersistence persistence =
                persistenceInjector.getInstance(RetainedMessageRocksDBLocalPersistence.class);
        assertEquals(1000, persistence.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals("message" + i, new String(persistence.get("topic" + i,
                    BucketUtils.getBucket("topic" + i, InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get()))
                    .getMessage()));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(RetainedMessageRocksDBLocalPersistence.PERSISTENCE_VERSION,
                metaInformation.getRetainedMessagesPersistenceVersion());
    }

    @Test
    public void test_retained_rocksdb_to_xodus() throws Exception {
        MetaFileService.writeMetaFile(systemInformation, getMetaInformation());

        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE);

        final Map<MigrationUnit, PersistenceType> migrations = Migrations.checkForTypeMigration(systemInformation);

        assertEquals(1, migrations.size());
        assertEquals(PersistenceType.FILE, migrations.get(MigrationUnit.FILE_PERSISTENCE_RETAINED_MESSAGES));

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation,
                new MetricRegistry(),
                new HivemqId(),
                configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final RetainedMessageRocksDBLocalPersistence rocks =
                persistenceInjector.getInstance(RetainedMessageRocksDBLocalPersistence.class);
        final PublishPayloadLocalPersistence payload =
                persistenceInjector.getInstance(PublishPayloadLocalPersistence.class);

        for (int i = 0; i < 1000; i++) {
            payload.put(i, ("message" + i).getBytes());
            rocks.put(new RetainedMessage(("message" + i).getBytes(), QoS.AT_LEAST_ONCE, (long) i, i + 1000),
                    "topic" + i,
                    BucketUtils.getBucket("topic" + i, rocks.getBucketCount()));
        }

        Migrations.migrate(persistenceInjector, migrations, ImmutableSet.of());

        final RetainedMessageXodusLocalPersistence xodus =
                persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence.class);
        assertEquals(1000, xodus.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals(
                    "message" + i,
                    new String(xodus.get(
                            "topic" + i,
                            BucketUtils.getBucket("topic" + i, InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get()))
                            .getMessage()));
        }

        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION,
                metaInformation.getRetainedMessagesPersistenceVersion());
        assertEquals(PersistenceType.FILE, metaInformation.getRetainedMessagesPersistenceType());
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);

    }

    @Test
    public void test_retained_stays_xodus() throws Exception {

        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE);

        final Map<MigrationUnit, PersistenceType> migrations = Migrations.checkForTypeMigration(systemInformation);

        assertEquals(1, migrations.size());
        assertFalse(migrations.containsKey(MigrationUnit.FILE_PERSISTENCE_RETAINED_MESSAGES));

        final Injector persistenceInjector = GuiceBootstrap.persistenceInjector(systemInformation,
                new MetricRegistry(),
                new HivemqId(),
                configurationService);
        final PersistenceStartup persistenceStartup = persistenceInjector.getInstance(PersistenceStartup.class);
        persistenceStartup.finish();

        final RetainedMessageXodusLocalPersistence xodus =
                persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence.class);
        final PublishPayloadLocalPersistence payload =
                persistenceInjector.getInstance(PublishPayloadLocalPersistence.class);

        for (int i = 0; i < 10; i++) {
            payload.put(i, ("message" + i).getBytes());
            xodus.put(new RetainedMessage(("message" + i).getBytes(), QoS.AT_LEAST_ONCE, (long) i, i + 1000),
                    "topic" + i,
                    BucketUtils.getBucket("topic" + i, xodus.getBucketCount()));
        }

        Migrations.migrate(persistenceInjector, migrations, ImmutableSet.of());
        Migrations.afterMigration(systemInformation);

        persistenceStartup.run();

        //check meta after migration
        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);
        assertEquals(RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION,
                metaInformation.getRetainedMessagesPersistenceVersion());
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);

    }

    @NotNull
    private MetaInformation getMetaInformation() {
        final MetaInformation metaInformation = new MetaInformation();

        metaInformation.setHivemqVersion("2019.2");
        metaInformation.setPublishPayloadPersistenceType(PersistenceType.FILE_NATIVE);
        metaInformation.setRetainedMessagesPersistenceType(PersistenceType.FILE_NATIVE);
        metaInformation.setDataFolderPresent(true);
        metaInformation.setMetaFilePresent(true);
        metaInformation.setPersistenceFolderPresent(true);

        return metaInformation;
    }
}