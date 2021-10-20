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
package com.hivemq.mqtt.message.pool;

import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;

/**
 * A message ID pool for MQTT QoS message identifiers.
 * <p>
 * There are no guarantees or predictions if the message ids are sequential or random,
 * the concrete implementation can decide how message ids are generated. The only guarantee
 * the message ID pool provides is that each taken message id will not be reused until it is returned.
 * <p>
 * <b>Each taken message id must be returned explicitly to the pool</b>
 * <p>
 * All message ID pool implementations are Thread safe
 *
 * @author Dominik Obermaier
 */
@ThreadSafe
public interface MessageIDPool {

    /**
     * Returns the next available message id.
     * <p>
     * <b>Important: It's the responsibility of the caller to return the
     * message id again!</b>
     *
     * @return the next available message id
     * @throws NoMessageIdAvailableException if no message id is available
     */
    @ThreadSafe
    int takeNextId() throws NoMessageIdAvailableException;

    /**
     * Acquires a given message id, if it is available. The next available message id is returned otherwise.
     * <p>
     * <b>Important: It's the responsibility of the caller to return the
     * message id again!</b>
     *
     * @return An available message id
     * @throws NoMessageIdAvailableException if no message id is available
     */
    @ThreadSafe
    int takeIfAvailable(final int id) throws NoMessageIdAvailableException;

    /**
     * Returns a message id if it is not needed anymore
     *
     * @param id the id to return.
     */
    @ThreadSafe
    void returnId(int id);

    /**
     * Prepopulates the message pools with unavailable message ids. These message ids
     * are not available until someone returns them
     *
     * @param ids the message ids which are unavailable
     */
    @ThreadSafe
    void prepopulateWithUnavailableIds(int... ids);
}
