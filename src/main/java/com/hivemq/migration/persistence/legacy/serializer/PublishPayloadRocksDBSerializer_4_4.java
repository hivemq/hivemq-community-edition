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
package com.hivemq.migration.persistence.legacy.serializer;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.Bytes;

/**
 * @author Florian Limpöck
 * @since 4.5.0
 */
public class PublishPayloadRocksDBSerializer_4_4 {

    @NotNull
    public static byte[] serializeKey(final long id) {
        final byte[] bytes = new byte[8];
        Bytes.copyLongToByteArray(id, bytes, 0);
        return bytes;
    }

    public static long deserializeKey(final @NotNull byte[] bytes) {
        return Bytes.readLong(bytes, 0);
    }
}
