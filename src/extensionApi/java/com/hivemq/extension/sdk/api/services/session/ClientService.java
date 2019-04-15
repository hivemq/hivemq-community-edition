/*
 * Copyright 2018 dc-square GmbH
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

package com.hivemq.extension.sdk.api.services.session;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.exception.NoSuchClientIdException;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Through this client service an extension can query details about connected or disconnected clients (with a persistent
 * session) from the HiveMQ core.
 *
 * @author Lukas Brandl
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface ClientService {

    /**
     * Check if a client with a given identifier is currently connected
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param clientId The client identifier of the client.
     * @return A {@link CompletableFuture} which contains <b>true</b>, if a certain client is currently connected and
     * <b>false</b> otherwise.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Boolean> isClientConnected(@NotNull String clientId);

    /**
     * Returns additional client information about a given client with a given client identifier.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param clientId The client identifier of the client.
     * @return A {@link CompletableFuture} which contains the {@link SessionInformation} for the client, if the session exist.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Optional<SessionInformation>> getSession(@NotNull String clientId);

    /**
     * Forcefully disconnect a client with the specified clientId.
     * <p>
     * If the client specified a Will message, it will be sent.
     * To prevent the sending of Will messages, use the {@link #disconnectClient(String, boolean)} method.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param clientId The client identifier of the client to disconnect.
     * @return A {@link CompletableFuture} which contains a {@link Boolean} that is <b>true</b> when the client has been
     * disconnected and <b>false</b> if no client with that id was found.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Boolean> disconnectClient(@NotNull String clientId);

    /**
     * Forcefully disconnect a client with the specified clientId.
     * <p>
     * Setting the boolean parameter to true will prevent the sending of potential Will messages the client may have
     * specified in its CONNECT packet.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param clientId           The client identifier of the client to disconnect.
     * @param preventWillMessage If <b>true</b> the Will message for this client is not published when the client gets
     *                           disconnected.
     * @return A {@link CompletableFuture} which contains a {@link Boolean} that is true when the client has been
     * disconnected and false if no client with that id was found.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Boolean> disconnectClient(@NotNull String clientId, boolean preventWillMessage);

    /**
     * Invalidates the client session for a client with the given client identifier. If the client is currently
     * connected, it will be disconnected as well.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     * <p>
     * {@link CompletableFuture} fails with a {@link NoSuchClientIdException} if no session for the given clientId
     * exists.
     *
     * @param clientId The client identifier of the client which session should be invalidated.
     * @return A {@link CompletableFuture} succeeding with a {@link Boolean} that is <b>true</b> when the client has been
     * actively disconnected by the broker otherwise <b>false</b>.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Boolean> invalidateSession(@NotNull String clientId);
}
