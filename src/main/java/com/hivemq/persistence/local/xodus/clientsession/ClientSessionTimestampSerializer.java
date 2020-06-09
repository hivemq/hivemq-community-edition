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
package com.hivemq.persistence.local.xodus.clientsession;

import com.hivemq.extension.sdk.api.annotations.NotNull;

class ClientSessionTimestampSerializer {

    /**
     * Reads a long value from the first six bytes of the given byte array
     *
     * @param b a byte array with size of at least 6
     * @return a timestamp long
     */
    public long byteArrayToTimestampLong(@NotNull final byte[] b) {
        return ((((long) b[5] & 0xff) << 40) |
                (((long) b[4] & 0xff) << 32) |
                (((long) b[3] & 0xff) << 24) |
                (((long) b[2] & 0xff) << 16) |
                (((long) b[1] & 0xff) << 8) |
                (((long) b[0] & 0xff))
        );
    }

    /**
     * Reads a timestamp and converts it to a six byte long
     * byte array.
     *
     * @param timestamp the timestamp to convert
     * @return a six byte long byte array
     */
    @NotNull
    public byte[] timestampLongToByteArray(final long timestamp) {
        return new byte[]{
                (byte) timestamp,
                (byte) (timestamp >> 8),
                (byte) (timestamp >> 16),
                (byte) (timestamp >> 24),
                (byte) (timestamp >> 32),
                (byte) (timestamp >> 40)
        };
    }
}