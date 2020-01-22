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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Lukas Brandl
 */
public interface PublishPayloadPersistence {

    /**
     * The payload persistence has to be initialized after the other persistence bootstraps are finished.
     */
    void init();

    /**
     * Add the payload to the persistence and set the initial reference count.
     * If the payload is already existent in the persistence, the reference count is added to the current value.
     *
     * @param payload        The payload that will be persisted.
     * @param referenceCount The initial amount of references for the payload.
     * @return The id associated with the payload.
     */
    long add(@NotNull byte[] payload, long referenceCount);

    /**
     * Get the persisted payload for an id.
     *
     * @param id The id associated with the payload.
     * @return The payload that is persisted.
     * @throws PayloadPersistenceException if {@link PublishPayloadLocalPersistence} returns null reference for id.
     */
    @NotNull
    byte[] get(long id);

    /**
     * Get the persisted payload for an id or null.
     *
     * @param id The id associated with the payload.
     * @return The payload that is persisted for the given id or null if the reference was deleted.
     */
    @Nullable("There is a race condition case with retained messages where retained messages are overwritten. " +
            "In this case this method may return null")
    byte[] getPayloadOrNull(long id);

    /**
     * Increments the current reference count for an id.
     * <p>
     * <b>Don't call this method after the persistence bootstrap is finished! </b>
     * Otherwise us the reference count of the "add" method.
     *
     * @param id The id associated with the payload.
     */
    void incrementReferenceCounterOnBootstrap(long id);

    /**
     * Decrements the current reference count for an id.
     *
     * @param id The id associated with the payload.
     */
    void decrementReferenceCounter(long id);

    /**
     * Suspect that the reference of a payload is not used.
     *
     * @param id The id associated with the payload.
     */
    void suspect(final long id);

    /**
     * Returns the IDs of all payloads that currently exist in the persistence.
     *
     * @return A List off all IDs.
     */
    @NotNull
    List<Long> getAllIds();

    /**
     * close the persistence with all buckets.
     */
    void closeDB();

    /**
     * @return all reference counts for all publish payloads in a readonly map.
     */
    @NotNull
    @VisibleForTesting
    ImmutableMap<Long, AtomicLong> getReferenceCountersAsMap();
}
