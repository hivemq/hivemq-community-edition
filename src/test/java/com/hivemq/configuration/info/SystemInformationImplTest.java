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
package com.hivemq.configuration.info;

import com.hivemq.HiveMQServer;
import com.hivemq.configuration.EnvironmentVariables;
import com.hivemq.configuration.SystemProperties;
import com.hivemq.util.ManifestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.org.webcompere.systemstubs.SystemStubs.restoreSystemProperties;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

/**
 * @author Christoph Schäbel
 */
public class SystemInformationImplTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private SystemInformation systemInformation;
    private String tempFolderPath;

    @Before
    public void before() {
        tempFolderPath = tempFolder.getRoot().getAbsolutePath();
    }

    @Test
    public void test_getHiveMQVersion() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            //check if there is a manifest file present (happens on jenkins) and use the value from the manifest file
            final String valueFromManifest = ManifestUtils.getValueFromManifest(HiveMQServer.class, "HiveMQ-Version");

            assertEquals(Objects.requireNonNullElse(valueFromManifest, "Development Snapshot"),
                    systemInformation.getHiveMQVersion());
        });
    }

    @Test
    public void test_getHiveMQVersion_from_system_information_with_path() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl(true);
            systemInformation.init();

            //check if there is a manifest file present (happens on jenkins) and use the value from the manifest file
            final String valueFromManifest = ManifestUtils.getValueFromManifest(HiveMQServer.class, "HiveMQ-Version");

            assertEquals(Objects.requireNonNullElse(valueFromManifest, "Development Snapshot"),
                    systemInformation.getHiveMQVersion());
        });
    }

    @Test
    public void test_getHiveMQHomeFolder() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertEquals(tempFolderPath, systemInformation.getHiveMQHomeFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getHiveMQHomeFolder_from_system_information_with_path() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertEquals(tempFolderPath, systemInformation.getHiveMQHomeFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getHiveMQHomeFolder_environmentVariable() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            final File testfolder = tempFolder.newFolder("home");

            System.getProperties().remove(SystemProperties.HIVEMQ_HOME);
            withEnvironmentVariable(EnvironmentVariables.HIVEMQ_HOME, testfolder.getAbsolutePath()) //
                    .execute(() -> {
                        systemInformation = new SystemInformationImpl();
                        systemInformation.init();
                    });

            assertEquals(testfolder.getAbsolutePath(), systemInformation.getHiveMQHomeFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getConfigFolder_default() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertEquals(tempFolderPath + File.separator + "conf",
                    systemInformation.getConfigFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getConfigFolder_property() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            final File testfolder = tempFolder.newFolder("testconfig");

            System.setProperty(SystemProperties.CONFIG_FOLDER, testfolder.getAbsolutePath());

            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertEquals(testfolder.getAbsolutePath(), systemInformation.getConfigFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getConfigFolder_environmentVariable() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            final File testfolder = tempFolder.newFolder("testconfig");

            withEnvironmentVariable(EnvironmentVariables.CONFIG_FOLDER, testfolder.getAbsolutePath()) //
                    .execute(() -> {
                        systemInformation = new SystemInformationImpl();
                        systemInformation.init();
                    });

            assertEquals(testfolder.getAbsolutePath(), systemInformation.getConfigFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getLogFolder_default() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertEquals(tempFolderPath + File.separator + "log", systemInformation.getLogFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getLogFolder_property() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            final File testfolder = tempFolder.newFolder("testlogs");

            System.setProperty(SystemProperties.LOG_FOLDER, testfolder.getAbsolutePath());

            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertEquals(testfolder.getAbsolutePath(), systemInformation.getLogFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getLogFolder_environmentVariable() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            final File testfolder = tempFolder.newFolder("testlogs");

            withEnvironmentVariable(EnvironmentVariables.LOG_FOLDER, testfolder.getAbsolutePath()) //
                    .execute(() -> {
                        systemInformation = new SystemInformationImpl();
                        systemInformation.init();
                    });

            assertEquals(testfolder.getAbsolutePath(), systemInformation.getLogFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getDataFolder_default() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertEquals(tempFolderPath + File.separator + "data", systemInformation.getDataFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getDataFolder_property() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            final File testfolder = tempFolder.newFolder("testdatas");

            System.setProperty(SystemProperties.DATA_FOLDER, testfolder.getAbsolutePath());

            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertEquals(testfolder.getAbsolutePath(), systemInformation.getDataFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_getDataFolder_environmentVariable() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            final File testfolder = tempFolder.newFolder("testdatas");

            withEnvironmentVariable(EnvironmentVariables.DATA_FOLDER, testfolder.getAbsolutePath()) //
                    .execute(() -> {
                        systemInformation = new SystemInformationImpl();
                        systemInformation.init();
                    });

            assertEquals(testfolder.getAbsolutePath(), systemInformation.getDataFolder().getAbsolutePath());
        });
    }

    @Test
    public void test_create_plugin_folder_if_not_exists() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertTrue(systemInformation.getExtensionsFolder().exists());
        });
    }

    @Test
    public void test_create_data_folder_if_not_exists() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertTrue(systemInformation.getDataFolder().exists());
        });
    }

    @Test
    public void test_create_log_folder_if_not_exists() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertTrue(systemInformation.getLogFolder().exists());
        });
    }

    @Test
    public void test_get_core_count() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(SystemProperties.HIVEMQ_HOME, tempFolderPath);
            systemInformation = new SystemInformationImpl();
            systemInformation.init();

            assertTrue(systemInformation.getProcessorCount() > 0);
        });
    }
}
