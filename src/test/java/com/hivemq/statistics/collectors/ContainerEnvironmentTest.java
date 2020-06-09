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
public class ContainerEnvironmentTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test_getContainerEnvironment_file_not_present() {

        assumeTrue(Platform.isLinux());

        final ContainerEnvironment containerEnvironment = new ContainerEnvironment() {
            @NotNull
            @Override
            File getCgroupFile() {
                return new File("/this/file/does/not/exist");
            }
        };

        assertEquals(null, containerEnvironment.getContainerEnvironment());
    }

    @Test
    public void test_getContainerEnvironment_docker_file_present() throws Exception {

        assumeTrue(Platform.isLinux());

        final File cgroupFile = new File(temporaryFolder.getRoot(), "cgroup");

        final String exampleContent = "11:name=systemd:/\n" +
                "9:perf_event:/\n" +
                "6:devices:/docker/435454325452353780436aa50e472bb46231663bb99a6bb8927649\n" +
                "5:memory:/\n" +
                "4:cpuacct:/\n" +
                "3:cpu:/docker/234a23a4e23a4e234243e2a4e23a4e23a42ea42e34a2e3a423bb\n" +
                "2:cpuset:/";

        FileUtils.writeStringToFile(cgroupFile, exampleContent, StandardCharsets.UTF_8);

        final ContainerEnvironment containerEnvironment = new ContainerEnvironment() {
            @NotNull
            @Override
            File getCgroupFile() {
                return cgroupFile;
            }
        };

        assertEquals("Docker", containerEnvironment.getContainerEnvironment());
    }


    @Test
    public void test_getContainerEnvironment_not_docker_file_present() throws Exception {

        assumeTrue(Platform.isLinux());

        final File cgroupFile = new File(temporaryFolder.getRoot(), "cgroup");

        final String exampleContent = "11:name=systemd:/\n" +
                "9:perf_event:/\n" +
                "6:devices:/other/435454325452353780436aa50e472bb46231663bb99a6bb8927649\n" +
                "5:memory:/\n" +
                "4:cpuacct:/\n" +
                "3:cpu:/other/234a23a4e23a4e234243e2a4e23a4e23a42ea42e34a2e3a423bb\n" +
                "2:cpuset:/";

        FileUtils.writeStringToFile(cgroupFile, exampleContent, StandardCharsets.UTF_8);

        final ContainerEnvironment containerEnvironment = new ContainerEnvironment() {
            @NotNull
            @Override
            File getCgroupFile() {
                return cgroupFile;
            }
        };

        assertEquals(null, containerEnvironment.getContainerEnvironment());
    }

}