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

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * This class stores the counter for payload references in in-memory data structures.
 * To avoid object overheads, the references are stored in a map that supports primitive data types.
 */
public interface PayloadReferenceCounterRegistry {

    /**
     * This return value represents a state where no entry is found for the given key. This makes difference to the
     * return of 0.
     * The latter indicates that we have a payload and its reference count is 0, so it could be removed.
     * The UNKNOWN_PAYLOAD constant represents, that we have no information on the reference count and that the payload
     * is assumed to be not present in the persistence.
     */
    int UNKNOWN_PAYLOAD = -1;

    /**
     * This constant serves as the return value of the {@link #decrementAndGet} in case a decrement gets called on a
     * reference counter
     * that is already 0
     */
    int REF_COUNT_ALREADY_ZERO = -2;

    /**
     * Getter for the reference count that is associated to the given payloadId
     *
     * @param payloadId the key for the payload reference counter
     * @return {@link #UNKNOWN_PAYLOAD}: there was no entry for the given payloadId
     *         otherwise the associated reference counter
     */
    int get(long payloadId);

    /**
     * Decrements the reference count by one
     *
     * @param payloadId the payloadId for which the count is decremented
     * @return {@link #UNKNOWN_PAYLOAD}: there was no entry for the given payloadId,
     *         {@link #REF_COUNT_ALREADY_ZERO}: the counter, that should get decremented, was already zero
     *         otherwise: the decremented reference count
     */
    int decrementAndGet(long payloadId);

    /**
     * Gets the current count of the reference and increases it afterward by the given amount
     *
     * @param payloadId the payloadId for which the count is incremented
     * @return {@link #UNKNOWN_PAYLOAD}: there was no entry for the given payloadId
     *         otherwise: the existing value that is associated before the increment
     */
    int getAndIncrement(long payloadId);

    /**
     * Deletes the entry for the given payloadId from the registry
     *
     * @param payloadId the payloadId for which the entry is removed
     */
    void delete(long payloadId);

    /**
     * @return all reference counter entries for all buckets and nodes
     */
    @NotNull ImmutableMap<Long, Integer> getAll();

    /**
     * @return the amount of entries for all buckets and nodes
     */
    int size();
}
