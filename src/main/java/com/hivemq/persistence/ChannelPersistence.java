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
package com.hivemq.persistence;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.Set;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
public interface ChannelPersistence {

    /**
     * Receive a {@link Channel} from the persistence, for a specific client id.
     *
     * @param clientId The client identifier.
     * @return The Channel of the client or {@code null} if not found.
     */
    @Nullable Channel get(@NotNull String clientId);

    /**
     * Receive a {@link ClientConnection} from the persistence, for a specific client id.
     *
     * @param clientId         The client identifier.
     * @return The ClientConnection of the client or {@code null} if not found.
     */
    @Nullable ClientConnection getClientConnection(@NotNull String clientId);

    /**
     * Try to put a ClientConnection for the client id. Return the old ClientConnection when there is already one.
     *
     * @param clientId         The client id of which the channel should be persisted.
     * @param clientConnection The ClientConnection to persist.
     * @return The currently persisted ClientConnection.
     */
    @NotNull ClientConnection tryPersist(@NotNull String clientId, @NotNull ClientConnection clientConnection);

    /**
     * Remove a {@link ClientConnection} from the persistence, for a specific client id.
     *
     * @param clientConnection The ClientConnection to remove.
     */
    void remove(@NotNull ClientConnection clientConnection);

    /**
     * @return the amount of stored connections.
     */
    long size();

    /**
     * Receive all ClientConnections with their corresponding client identifier as a set of map entries.
     *
     * @return all ClientConnections currently stored.
     */
    @NotNull Set<Map.Entry<String, ClientConnection>> entries();

    void addServerChannel(@NotNull String listenerName, @NotNull Channel channel);

    @NotNull Set<Map.Entry<String, Channel>> getServerChannels();

    @NotNull ListenableFuture<Void> shutDown();

    void interruptShutdown();
}
