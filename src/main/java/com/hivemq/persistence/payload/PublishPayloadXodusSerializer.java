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
package com.hivemq.persistence.payload;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.Bytes;

import static com.hivemq.persistence.payload.PublishPayloadXodusLocalPersistence.*;

/**
 * @author Lukas Brandl
 */
public class PublishPayloadXodusSerializer {

    @NotNull
    public byte[] serializeKey(final long id, final long chunkIndex) {

        final byte[] bytes = new byte[16];

        Bytes.copyLongToByteArray(id, bytes, 0);
        Bytes.copyLongToByteArray(chunkIndex, bytes, 8);

        return bytes;
    }

    @NotNull
    public KeyPair deserializeKey(final @NotNull byte[] bytes) {

        final long id = Bytes.readLong(bytes, 0);
        final long chunkIndex = Bytes.readLong(bytes, 8);

        return new KeyPair(id, chunkIndex);
    }
}
