package com.hivemq.persistence.payload;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Map;

/**
 * @author Daniel Krüger
 */
public interface PayloadReferenceCounterRegistry {


    static String toPayloadId(final long nodeID, final long counter) {
        return nodeID + "_pub_" + counter;
    }


    @Nullable Integer get(final long payloadId);


    int add(final long payloadId, final int referenceCount);

    int put(long payloadId, int referenceCount);

    int increment(final long payloadId);

    int decrement(final long payloadId);

    @NotNull Map<Long, Integer> getAll();

    int size();

    void remove(@NotNull long payloadId);


}
