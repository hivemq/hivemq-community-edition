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

import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.persistence.clientqueue.ClientQueueXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.clientsession.ClientSessionSubscriptionXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.clientsession.ClientSessionXodusLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadRocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadXodusLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 */
public class MigrationFinisherTest {

    private MigrationFinisher migrationFinisher;

    @Test
    public void test_finish() {

        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("2019.1");
        new File(systemInformation.getDataFolder(), LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME).mkdir();
        migrationFinisher = new MigrationFinisher(systemInformation);

        migrationFinisher.finishMigration();

        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);

        assertEquals("2019.1", metaInformation.getHivemqVersion());
        assertEquals(PublishPayloadRocksDBLocalPersistence.PERSISTENCE_VERSION, metaInformation.getPublishPayloadPersistenceVersion());
        assertEquals(RetainedMessageRocksDBLocalPersistence.PERSISTENCE_VERSION, metaInformation.getRetainedMessagesPersistenceVersion());
        assertEquals(ClientSessionXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getClientSessionPersistenceVersion());
        assertEquals(ClientQueueXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getQueuedMessagesPersistenceVersion());
        assertEquals(ClientSessionSubscriptionXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getSubscriptionPersistenceVersion());
        assertEquals(PersistenceType.FILE_NATIVE, metaInformation.getPublishPayloadPersistenceType());
        assertEquals(PersistenceType.FILE_NATIVE, metaInformation.getRetainedMessagesPersistenceType());

    }

    @Test
    public void test_finish_xodus() {

        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("2019.1");
        new File(systemInformation.getDataFolder(), LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME).mkdir();

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE);
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE);

        migrationFinisher = new MigrationFinisher(systemInformation);

        migrationFinisher.finishMigration();

        final MetaInformation metaInformation = MetaFileService.readMetaFile(systemInformation);

        assertEquals("2019.1", metaInformation.getHivemqVersion());
        assertEquals(PublishPayloadXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getPublishPayloadPersistenceVersion());
        assertEquals(RetainedMessageXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getRetainedMessagesPersistenceVersion());
        assertEquals(ClientSessionXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getClientSessionPersistenceVersion());
        assertEquals(ClientQueueXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getQueuedMessagesPersistenceVersion());
        assertEquals(ClientSessionSubscriptionXodusLocalPersistence.PERSISTENCE_VERSION, metaInformation.getSubscriptionPersistenceVersion());
        assertEquals(PersistenceType.FILE, metaInformation.getPublishPayloadPersistenceType());
        assertEquals(PersistenceType.FILE, metaInformation.getRetainedMessagesPersistenceType());

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE_NATIVE);

    }
}