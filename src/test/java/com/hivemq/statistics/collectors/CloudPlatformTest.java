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
package com.hivemq.statistics.collectors;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.sun.jna.Platform;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * @author Christoph Schäbel
 */
public class CloudPlatformTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test_getCloudPlatform_file_not_present() {

        assumeTrue(Platform.isLinux());

        final CloudPlatform cloudPlatform = new CloudPlatform() {

            @NotNull
            @Override
            File getUuidFile() {
                return new File("/this/file/does/not/exist");
            }
        };

        assertEquals(null, cloudPlatform.getCloudPlatform());
    }

    @Test
    public void test_getCloudPlatform_file_present_ec2() throws Exception {

        assumeTrue(Platform.isLinux());

        final File uuidFile = new File(temporaryFolder.getRoot(), "uuid");

        final String exampleContent = "ec2e1916-9099-7caf-fd21-012345abcdef";

        FileUtils.writeStringToFile(uuidFile, exampleContent, StandardCharsets.UTF_8);

        final CloudPlatform cloudPlatform = new CloudPlatform() {

            @NotNull
            @Override
            File getUuidFile() {
                return uuidFile;
            }
        };

        assertEquals("AWS EC2", cloudPlatform.getCloudPlatform());
    }

    @Test
    public void test_getCloudPlatform_file_present_ec2_upper() throws Exception {

        assumeTrue(Platform.isLinux());

        final File uuidFile = new File(temporaryFolder.getRoot(), "uuid");

        final String exampleContent = "EC2E1916-9099-7CAF-FD21-012345ABCDEF";

        FileUtils.writeStringToFile(uuidFile, exampleContent, StandardCharsets.UTF_8);

        final CloudPlatform cloudPlatform = new CloudPlatform() {

            @NotNull
            @Override
            File getUuidFile() {
                return uuidFile;
            }
        };

        assertEquals("AWS EC2", cloudPlatform.getCloudPlatform());
    }


    @Test
    public void test_getCloudPlatform_file_present_not_ec2() throws Exception {

        assumeTrue(Platform.isLinux());

        final File uuidFile = new File(temporaryFolder.getRoot(), "uuid");

        final String exampleContent = "AB2E1916-9099-7CAF-FD21-012345ABCDEF";

        FileUtils.writeStringToFile(uuidFile, exampleContent, StandardCharsets.UTF_8);

        final CloudPlatform cloudPlatform = new CloudPlatform() {

            @NotNull
            @Override
            File getUuidFile() {
                return uuidFile;
            }
        };

        assertEquals(null, cloudPlatform.getCloudPlatform());
    }

}