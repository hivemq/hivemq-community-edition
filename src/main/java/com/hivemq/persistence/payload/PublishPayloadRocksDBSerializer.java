package com.hivemq.persistence.payload;

import com.google.common.primitives.Longs;
import com.hivemq.annotations.NotNull;
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
