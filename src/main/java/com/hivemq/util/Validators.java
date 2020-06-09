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
package com.hivemq.util;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.entity.ClientWriteBufferProperties;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Georg Held
 */
public class Validators {
    private static final Logger log = getLogger(Validators.class);

    // the default low threshold in byte (32kB)
    public static final int DEFAULT_LOW_THRESHOLD = 32 * 1024;
    //the default high threshold in byte (64kB)
    public static final int DEFAULT_HIGH_THRESHOLD = 64 * 1024;
    public static final ClientWriteBufferProperties DEFAULT_WRITE_BUFFER_PROPERTIES =
            new ClientWriteBufferProperties(DEFAULT_HIGH_THRESHOLD, DEFAULT_LOW_THRESHOLD);

    public static @NotNull ClientWriteBufferProperties validateWriteBufferProperties(
            @NotNull final ClientWriteBufferProperties writeBufferProperties) {

        checkNotNull(writeBufferProperties, "writeBufferProperties must not be null");

        if (validateWriteBufferThresholds(writeBufferProperties.getHighThreshold(), writeBufferProperties.getLowThreshold())) {
            return writeBufferProperties;
        }
        return DEFAULT_WRITE_BUFFER_PROPERTIES;
    }

    public static boolean validateWriteBufferThresholds(final int high, final int low) {
        if (low <= 0) {
            log.warn("write-buffer low-threshold must be greater than zero");
            return false;
        }
        if (high < low) {
            log.warn("write-buffer high-threshold must be greater than write-buffer low-threshold");
            return false;
        }
        return true;
    }
}
