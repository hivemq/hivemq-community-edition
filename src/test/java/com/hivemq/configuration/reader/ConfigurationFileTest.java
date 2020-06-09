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
package com.hivemq.configuration.reader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ConfigurationFileTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test_null_configuration_file() throws Exception {
        final ConfigurationFile configurationFile = new ConfigurationFile(null);

        assertEquals(false, configurationFile.file().isPresent());
    }

    @Test
    public void test_configuration_file_exists() throws Exception {
        final File file = temporaryFolder.newFile();
        final ConfigurationFile configurationFile = new ConfigurationFile(file);

        assertEquals(true, configurationFile.file().isPresent());
        assertEquals(file, configurationFile.file().get());
    }
}