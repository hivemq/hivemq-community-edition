package com.hivemq.migration.persistence.legacy.serializer;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.migration.persistence.legacy.PublishPayloadXodusLocalPersistence_4_4.KeyPair;
import com.hivemq.util.Bytes;

/**
 * @author Florian Limpöck
 * @since 4.5.0
 */
public class PublishPayloadXodusSerializer_4_4 {

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
