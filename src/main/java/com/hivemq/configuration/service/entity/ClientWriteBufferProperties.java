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
package com.hivemq.configuration.service.entity;

import com.hivemq.extension.sdk.api.annotations.Immutable;

/**
 * Allows the customization of write buffer behaviour.
 *
 * @since 3.3
 */
@Immutable
public class ClientWriteBufferProperties {

    private final int highThresholdBytes;
    private final int lowThresholdBytes;

    /**
     * @param highThresholdBytes If the write buffer for a client reaches a size in bytes that is greater than {@code highThreshold} no more data will be written to the write buffer.
     * @param lowThresholdBytes  If the write buffer for a client exceeded the highThreshold in the past, writing to the buffer will be resumed once the fill state of the buffer drops below the {@code lowThreshold}.
     */
    public ClientWriteBufferProperties(final int highThresholdBytes, final int lowThresholdBytes) {
        this.highThresholdBytes = highThresholdBytes;
        this.lowThresholdBytes = lowThresholdBytes;
    }

    public int getHighThresholdBytes() {
        return highThresholdBytes;
    }

    public int getLowThresholdBytes() {
        return lowThresholdBytes;
    }
}
