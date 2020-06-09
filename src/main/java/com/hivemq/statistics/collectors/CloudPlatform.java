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
public class CloudPlatform {

    private static final Logger log = LoggerFactory.getLogger(CloudPlatform.class);

    public String getCloudPlatform() {
        try {
            if (!Platform.isLinux()) {
                return null;
            }

            //can generate false positives, but seems to be the simplest and least intrusive solution
            //see https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/identify_ec2_instances.html
            final File uuidFile = getUuidFile();
            if (uuidFile.exists() && uuidFile.canRead()) {
                final String content = FileUtils.readFileToString(uuidFile, StandardCharsets.UTF_8);
                if (content.startsWith("ec2") || content.startsWith("EC2")) {
                    return "AWS EC2";
                }
            }
        } catch (final Exception ex) {
            log.trace("not able to determine if running on cloud platform", ex);
        }

        return null;
    }

    @NotNull
    @VisibleForTesting
    File getUuidFile() {
        return new File("/sys/hypervisor/uuid");
    }
}
