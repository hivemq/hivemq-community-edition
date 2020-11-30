package com.hivemq.migration.persistence.legacy;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * legacy pre 4.5 interface just for put and get
 *
 * @author Florian Limpöck
 * @author Lukas Brandl
 * @since 4.5.0
 */
public interface PublishPayloadLocalPersistence_4_4 {

    @Nullable
    byte[] get(@NotNull long id);

    void put(final long id, @NotNull final byte[] payload);

}
