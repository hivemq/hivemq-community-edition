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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.publish.PUBLISH;

public interface PublishPayloadPersistence {

    static long createId() {
        return PUBLISH.PUBLISH_COUNTER.getAndIncrement();
    }

    /**
     * The payload persistence has to be initialized after the other persistence bootstraps are finished.
     */
    void init();

    /**
     * Add the payload to the persistence and counts the reference count up.
     * If the payload is already existent in the persistence, the reference count is incremented.
     *
     * @param payload   The payload that will be persisted.
     * @param id The publish ID is used a the payload ID
     */
    void add(byte @NotNull [] payload, long id);

    /**
     * Get the persisted payload for an id.
     *
     * @param id The id associated with the payload.
     * @return The payload that is persisted.
     * @throws PayloadPersistenceException if {@link PublishPayloadLocalPersistence} returns null reference for id.
     */
    byte @NotNull [] get(long id);

    /**
     * Get the persisted payload for an id or null.
     *
     * @param id The id associated with the payload.
     * @return The payload that is persisted for the given id or null if the reference was deleted.
     */
    byte @Nullable [] getPayloadOrNull(long id);

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
     * close the persistence with all buckets.
     */
    void closeDB();

    /**
     * @return all reference counts for all publish payloads in a readonly map.
     */
    @VisibleForTesting
    @NotNull ImmutableMap<Long, Integer> getReferenceCountersAsMap();
}
