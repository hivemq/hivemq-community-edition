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
import com.hivemq.migration.meta.MetaFileService;
import com.hivemq.migration.meta.MetaInformation;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 */
public class MigrationsTest {

    @Test
    public void test_check_development_snapshot() {
        assertEquals(0, Migrations.checkForTypeMigration(new SystemInformationImpl()).size());
    }

    @Test
    public void test_check_no_data_folder_present() {
        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("2019.1");
        systemInformation.getDataFolder().delete();
        assertEquals(0, Migrations.checkForTypeMigration(systemInformation).size());
    }

    @Test
    public void test_check_no_meta_and_no_persistence_folder() {
        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("2019.1");
        assertEquals(0, Migrations.checkForTypeMigration(systemInformation).size());
    }

    @Test
    public void test_check_no_meta_but_persistence_folder_but_no_previous_persistences_found() {
        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("2019.1");
        new File(systemInformation.getDataFolder(), LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME).mkdir();
        assertEquals(0, Migrations.checkForTypeMigration(systemInformation).size());
    }

    @Test
    public void test_check_no_meta_but_persistence_folder_and_previous_payload_persistence_found() {
        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("4.3.0");
        final File persistenceFolder = new File(systemInformation.getDataFolder(), LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME);
        persistenceFolder.mkdir();
        new File(persistenceFolder, PublishPayloadLocalPersistence.PERSISTENCE_NAME).mkdir();
        assertEquals(1, Migrations.checkForTypeMigration(systemInformation).size());
    }

    @Test
    public void test_check_no_meta_but_persistence_folder_and_previous_retained_persistence_found() {
        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("4.3.0");
        final File persistenceFolder = new File(systemInformation.getDataFolder(), LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME);
        persistenceFolder.mkdir();
        new File(persistenceFolder, RetainedMessageLocalPersistence.PERSISTENCE_NAME).mkdir();
        assertEquals(1, Migrations.checkForTypeMigration(systemInformation).size());
    }

    @Test
    public void test_check_no_meta_but_persistence_folder_and_previous_retained_persistence_and_payload_found() {
        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("4.3.0");
        final File persistenceFolder = new File(systemInformation.getDataFolder(), LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME);
        persistenceFolder.mkdir();
        new File(persistenceFolder, RetainedMessageLocalPersistence.PERSISTENCE_NAME).mkdir();
        new File(persistenceFolder, PublishPayloadLocalPersistence.PERSISTENCE_NAME).mkdir();
        assertEquals(2, Migrations.checkForTypeMigration(systemInformation).size());
    }

    @Test
    public void test_check_with_meta_was_file() {
        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("4.3.0");

        final MetaInformation metaInformation = new MetaInformation();
        metaInformation.setHivemqVersion("4.3.0");
        metaInformation.setPublishPayloadPersistenceType(PersistenceType.FILE);
        metaInformation.setRetainedMessagesPersistenceType(PersistenceType.FILE);

        MetaFileService.writeMetaFile(systemInformation, metaInformation);
        final File persistenceFolder = new File(systemInformation.getDataFolder(), LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME);
        persistenceFolder.mkdir();
        new File(persistenceFolder, RetainedMessageLocalPersistence.PERSISTENCE_NAME).mkdir();
        new File(persistenceFolder, PublishPayloadLocalPersistence.PERSISTENCE_NAME).mkdir();
        assertEquals(2, Migrations.checkForTypeMigration(systemInformation).size());
    }

    @Test
    public void test_check_with_meta_was_file_native() {
        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.setHivemqVersion("4.3.0");

        final MetaInformation metaInformation = new MetaInformation();
        metaInformation.setHivemqVersion("4.3.0");
        metaInformation.setPublishPayloadPersistenceType(PersistenceType.FILE_NATIVE);
        metaInformation.setRetainedMessagesPersistenceType(PersistenceType.FILE_NATIVE);

        MetaFileService.writeMetaFile(systemInformation, metaInformation);
        final File persistenceFolder = new File(systemInformation.getDataFolder(), LocalPersistenceFileUtil.PERSISTENCE_SUBFOLDER_NAME);
        persistenceFolder.mkdir();
        new File(persistenceFolder, RetainedMessageLocalPersistence.PERSISTENCE_NAME).mkdir();
        new File(persistenceFolder, PublishPayloadLocalPersistence.PERSISTENCE_NAME).mkdir();
        assertEquals(0, Migrations.checkForTypeMigration(systemInformation).size());
    }
}