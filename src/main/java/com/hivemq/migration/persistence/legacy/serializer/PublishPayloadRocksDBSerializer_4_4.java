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
