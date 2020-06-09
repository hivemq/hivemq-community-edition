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
package com.hivemq.persistence.qos;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.MessageWithID;

/**
 * @author Dominik Obermaier
 */
public interface IncomingMessageFlowPersistence {

    /**
     * get a {@link MessageWithID} for specific client id and message id from the persistence.
     *
     * @param client    The client which belongs this message.
     * @param messageId The identifier of the message.
     * @return the message with id.
     */
    @Nullable
    MessageWithID get(final @NotNull String client, final int messageId);

    /**
     * Add or replace a {@link MessageWithID} for specific client id and message id.
     *
     * @param client    The client which belongs this message.
     * @param messageId The identifier of the message.
     * @param message   The message to add.
     */
    void addOrReplace(final @NotNull String client, final int messageId, final @NotNull MessageWithID message);


    /**
     * Remove a {@link MessageWithID} for specific client id and message id.
     *
     * @param client    The client which belongs this message.
     * @param messageId The identifier of the message.
     */
    void remove(final @NotNull String client, final int messageId);

    /**
     * Delete all {@link MessageWithID}s for specific client id.
     *
     * @param client The client which belongs this message.
     */
    void delete(final @NotNull String client);

    /**
     * close the persistence with all buckets.
     */
    void closeDB();

}
