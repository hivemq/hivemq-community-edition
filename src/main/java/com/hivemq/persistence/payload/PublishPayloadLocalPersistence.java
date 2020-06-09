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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.annotations.ReadOnly;

/**
 * @author Lukas Brandl
 */
public interface PublishPayloadLocalPersistence {

    String PERSISTENCE_NAME = "publish_payload_store";

    /**
     * initialize the publish payload local persistence to set the next payload id.
     */
    void init();

    /**
     * Put a payload for a specific id.
     *
     * @param id      The payload id.
     * @param payload The payload to put.
     */
    void put(long id, @NotNull byte[] payload);

    /**
     * Get a payload for a specific id.
     *
     * @param id The payload id.
     * @return the payload for the id.
     */
    @Nullable
    byte[] get(long id);

    /**
     * Remove a payload for a specific id.
     *
     * @param id The payload id.
     */
    void remove(long id);

    /**
     * @return the highest identifier for which payloads are currently stored in the persistence.
     */
    long getMaxId();

    /**
     * @return all payload ids as a readonly list.
     */
    @ReadOnly
    @NotNull
    ImmutableList<Long> getAllIds();

    /**
     * close the persistence with all buckets.
     */
    void closeDB();

    /**
     * iterate over all entries.
     * @param callback the callback called at every iteration.
     */
    void iterate(final @NotNull Callback callback);

    @FunctionalInterface
    interface Callback {
        void call(long id, @Nullable byte[] payload);
    }
}
