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
 * The ConnectionPersistence contains the connections of all active clients. The connection however can be closed in
 * the meantime when you try to get the connection. It is recommended to check the
 * {@link com.hivemq.bootstrap.ClientState} of the connection to make sure if the connection is still open.
 * <p>
 * It guarantees even under heavy take over scenarios that only one client for one unique identifier is persisted.
 * <p>
 * The connections in here need to be shutdown gracefully with {@link ConnectionPersistence#shutDown} when HiveMQ is
 * shutting down.
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
     * Try to put a ClientConnection for the client id. Return the old ClientConnection when there is already one.
     *
     * @param clientId         The client id of which the channel should be persisted.
     * @param clientConnection The ClientConnection to persist.
     * @return The currently persisted ClientConnection.
     */
    @NotNull ClientConnection persistIfAbsent(@NotNull String clientId, @NotNull ClientConnection clientConnection);

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
