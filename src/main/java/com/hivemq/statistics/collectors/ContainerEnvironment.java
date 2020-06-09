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

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.sun.jna.Platform;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author Christoph Schäbel
 */
public class ContainerEnvironment {

    private static final Logger log = LoggerFactory.getLogger(ContainerEnvironment.class);

    public String getContainerEnvironment() {

        try {
            if (!Platform.isLinux()) {
                return null;
            }

            //a docker container always has the content "docker" in the cgroup file
            //see https://stackoverflow.com/questions/23513045/
            final File cgroupFile = getCgroupFile();
            if (cgroupFile.exists() && cgroupFile.canRead()) {
                final String content = FileUtils.readFileToString(cgroupFile, StandardCharsets.UTF_8);
                if (content.contains("docker")) {
                    return "Docker";
                }
            }
        } catch (final Exception ex) {
            log.trace("not able to determine if running in container", ex);
        }

        return null;
    }

    @NotNull
    @VisibleForTesting
    File getCgroupFile() {
        final long pid = ProcessHandle.current().pid();
        return new File("/proc/" + pid + "/cgroup");
    }


}
