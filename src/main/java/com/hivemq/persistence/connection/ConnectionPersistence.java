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
package com.hivemq.persistence.connection;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.netty.channel.Channel;

/**
 * The ConnectionPersistence contains the connections of all the active clients. However, there is no guarantee that
 * the connection is open at the time of access. Hence, the accessing code must check
 * {@link com.hivemq.bootstrap.ClientState} of the connection to make sure that the connection is still open.
 * <p>
 * ConnectionPersistence guarantees that even under takeover intensive scenarios only one client per identifier is
 * persisted.
 * <p>
 * The graceful shutdown of all the persisted connections can be done via {@link ConnectionPersistence#shutDown}.
 * This call is necessary when HiveMQ is shutting down.
 */
public interface ConnectionPersistence {

    /**
     * Receive a {@link ClientConnection} from the persistence, for a specific client id.
     *
     * @param clientId The client identifier.
     * @return The ClientConnection of the client or {@code null} if not found.
     */
    @Nullable ClientConnection get(@NotNull String clientId);

    /**
     * Try to persist a ClientConnection. This method stores one ClientConnection per unique client ID. Returns the
     * existing ClientConnection if there was already one persisted with the same client ID.
     *
     * @param clientConnection The ClientConnection to persist.
     * @return ClientConnection persisted in ConnectionPersistence after the operation completes.
     */
    @NotNull ClientConnection persistIfAbsent(@NotNull ClientConnection clientConnection);

    /**
     * Remove a {@link ClientConnection} from the persistence, for a specific client id.
     *
     * @param clientConnection The ClientConnection to remove.
     */
    void remove(@NotNull ClientConnection clientConnection);

    void addServerChannel(@NotNull String listenerName, @NotNull Channel channel);

    @NotNull ListenableFuture<Void> shutDown();

    void interruptShutdown();
}
