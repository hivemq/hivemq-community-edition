/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.persistence.payload;

import com.google.common.primitives.Longs;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.Bytes;

/**
 * @author Florian Limpöck
 */
public class PublishPayloadRocksDBSerializer {

    @NotNull
    public static byte[] serializeKey(final long id) {
        return Longs.toByteArray(id);
    }

    public static long deserializeKey(final @NotNull byte[] bytes) {
        return Longs.fromByteArray(bytes);
    }
}
