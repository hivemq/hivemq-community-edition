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
package com.hivemq.extension.sdk.api.services.session;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.services.ManagedExtensionExecutorService;
import com.hivemq.extension.sdk.api.services.exception.IncompatibleHiveMQVersionException;
import com.hivemq.extension.sdk.api.services.exception.IterationFailedException;
import com.hivemq.extension.sdk.api.services.exception.NoSuchClientIdException;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extension.sdk.api.services.general.IterationContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Through this client service an extension can query details about connected or disconnected clients (with a persistent
 * session) from the HiveMQ core.
 *
 * @author Lukas Brandl
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @author Robin Atherton
 * @since 4.0.0
 */
@DoNotImplement
public interface ClientService {

    /**
     * Check if a client with a given identifier is currently connected to this HiveMQ broker instance or any other
     * instance in the cluster.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param clientId The client identifier of the client.
     * @return A {@link CompletableFuture} which contains <b>true</b>, if a certain client is currently connected and
     *         <b>false</b> otherwise.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Boolean> isClientConnected(@NotNull String clientId);

    /**
     * Returns additional client information about a given client with a given client identifier.
     * <p>
     * This method will also get client information from other cluster nodes if needed.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param clientId The client identifier of the client.
     * @return A {@link CompletableFuture} which contains the {@link SessionInformation} for the client, if the session
     *         exist.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Optional<SessionInformation>> getSession(@NotNull String clientId);

    /**
     * Forcefully disconnect a client with the specified clientId.
     * <p>
     * If the client specified a Will message, it will be sent. To prevent the sending of Will messages, use the {@link
     * #disconnectClient(String, boolean)} method.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param clientId The client identifier of the client to disconnect.
     * @return A {@link CompletableFuture} which contains a {@link Boolean} that is <b>true</b> when the client has been
     *         disconnected and <b>false</b> if no client with that id was found.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Boolean> disconnectClient(@NotNull String clientId);

    /**
     * Forcefully disconnect a client with the specified clientId.
     * <p>
     * If a specific reason code and/or reason string should be sent with the DISCONNECT packet use {@link
     * ClientService#disconnectClient(String, boolean, DisconnectReasonCode, String)}.
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
     * @return A {@link CompletableFuture} which contains a {@link Boolean} that is <b>true</b> when the client has been
     *         disconnected and <b>false</b> if no client with that id was found.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Boolean> disconnectClient(@NotNull String clientId, boolean preventWillMessage);

    /**
     * Forcefully disconnect a client with the specified clientId.
     * <p>
     * A specific reason code and/or reason string can be set when wanted.
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
     * @param reasonCode         The reason code for disconnecting this client.
     * @param reasonString       The user defined reason string for disconnecting this client.
     * @return A {@link CompletableFuture} which contains a {@link Boolean} that is <b>true</b> when the client has been
     *         disconnected and <b>false</b> if no client with that id was found.
     * @throws IllegalArgumentException If the disconnect reason code must not be used for outbound disconnect packets
     *                                  from the server to a client.
     * @see DisconnectReasonCode What reason codes exist for outbound disconnect packets from the server to a
     *         client.
     */
    @NotNull CompletableFuture<Boolean> disconnectClient(
            @NotNull String clientId, boolean preventWillMessage,
            @Nullable DisconnectReasonCode reasonCode, @Nullable String reasonString);

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
     * @return A {@link CompletableFuture} succeeding with a {@link Boolean} that is <b>true</b> when the client has
     *         been actively disconnected by the broker otherwise <b>false</b>.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Boolean> invalidateSession(@NotNull String clientId);

    /**
     * Iterate over all clients and their session information.
     * <p>
     * The callback is called once for each client. Passed to each execution of the callback are the client identifier
     * and its session information. Clients that have exceeded their session expiry interval are not included.
     * <p>
     * The callback is executed in the {@link ManagedExtensionExecutorService} per default. Use the overloaded methods
     * to pass a custom executor for the callback. If you want to collect the results of each execution of the callback
     * in a collection please make sure to use a concurrent collection (thread-safe), as the callback might be executed
     * in another thread as the calling thread of this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation. Do not call
     * this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all HiveMQ nodes in the
     * cluster have at least version 4.2.0. {@link CompletableFuture} fails with a {@link RateLimitExceededException} if
     * the extension service rate limit was exceeded. {@link CompletableFuture} fails with a {@link
     * IterationFailedException} if the cluster topology changed during the iteration (e.g. a network-split, node leave
     * or node join)
     *
     * @param callback An {@link IterationCallback} that is called for every returned result.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found or the
     *         iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException If the passed callback or callbackExecutor are null.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllClients(@NotNull IterationCallback<SessionInformation> callback);

    /**
     * Iterate over all clients and their session information.
     * <p>
     * The callback is called once for each client. Passed to each execution of the callback are the client identifier
     * and its session information. Clients that have exceeded their session expiry interval are not included.
     * <p>
     * The callback is executed in the passed {@link Executor}. If you want to collect the results of each execution of
     * the callback in a collection please make sure to use a concurrent collection, as the callback might be executed
     * in another thread as the calling thread of this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation. Do not call
     * this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all HiveMQ nodes in the
     * cluster have at least version 4.2.0. {@link CompletableFuture} fails with a {@link RateLimitExceededException} if
     * the extension service rate limit was exceeded. {@link CompletableFuture} fails with a {@link
     * IterationFailedException} if the cluster topology changed during the iteration (e.g. a network-split, node leave
     * or node join)
     *
     * @param callback         An {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor An {@link Executor} in which the callback for each iteration is executed.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found or the
     *         iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException If the passed callback or callbackExecutor are null.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllClients(
            @NotNull IterationCallback<SessionInformation> callback, @NotNull Executor callbackExecutor);

}
