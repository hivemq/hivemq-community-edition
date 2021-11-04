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
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Map;

/**
 * @author Daniel Krüger
 * <p>
 * This class stores the counter for payload references in in-memory data structures.
 * To avoid object overheads, the references are stored in a map that supports primitive data types.
 */
public interface PayloadReferenceCounterRegistry {

    /**
     * Getter for the reference count that is associated to the given payloadId
     *
     * @param payloadId the key for the payload reference counter
     * @return null: there was no entry for the given payloadId, otherwise the associated reference counter
     */
    int get(final @NotNull long payloadId);

    /**
     * Increments the reference count by one
     *
     * @param payloadId the payloadId for which the count is incremented
     * @return the new value that is associated to the payloadId
     */
    int incrementAndGet(final @NotNull long payloadId);

    /**
     * Gets the current count of the reference
     *
     * @param payloadId the payloadId for which the count is incremented
     * @param delta     the value by which the reference count is incremented
     * @return the existing value that is associated or 0 if none was associated
     */
    int getAndIncrementBy(final @NotNull long payloadId, final int delta);

    /**
     * Decrements the reference count by one
     *
     * @param payloadId the payloadId for which the count is decremented
     * @return the new value that is associated to the payloadId
     */
    int decrementAndGet(final @NotNull long payloadId);

    /**
     * Returns all reference counter entries for all buckets and nodes
     *
     * @return all reference counter entries for all buckets and nodes
     */
    @NotNull Map<Long, Integer> getAll();

    /**
     * Returns the amount of entries for all buckets and node
     *
     * @return the amount of entries for all buckets and node
     */
    int size();


}
