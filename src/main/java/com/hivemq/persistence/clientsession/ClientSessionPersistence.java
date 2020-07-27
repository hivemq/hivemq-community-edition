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
package com.hivemq.persistence.clientsession;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.extensions.iteration.ChunkCursor;
import com.hivemq.extensions.iteration.MultipleChunkResult;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;

import java.util.Map;
import java.util.Set;

import static com.hivemq.persistence.clientsession.ClientSessionPersistenceImpl.DisconnectSource;

/**
 * @author Lukas Brandl
 */
public interface ClientSessionPersistence {

    boolean isExistent(@NotNull final String client);

    @NotNull
    ListenableFuture<Void> clientDisconnected(final @NotNull String client, final boolean sendWil, long sessionExpiry);

    /**
     * Mark the client session as connected
     *
     * @param client                The client ID
     * @param cleanStart            clean start or use previous session
     * @param sessionExpiryInterval The session expiry interval for this client
     * @param queueLimit for this session specifically
     * @return The state of incomplete outgoing message transmissions for this client
     */
    @NotNull
    ListenableFuture<Void> clientConnected(final @NotNull String client, final boolean cleanStart, final long sessionExpiryInterval,
                                           @Nullable MqttWillPublish willPublish, @Nullable Long queueLimit);

    /**
     * Close the persistence.
     *
     * @return a future which completes as soon as the persistence is closed.
     */
    @NotNull
    ListenableFuture<Void> closeDB();

    /**
     * Get a client session for a specific identifier.
     *
     * @param clientId    the client id.
     * @param includeWill if the will message should be included, the will is set to <code>null</code> if this parameter is <code>false</code>.
     * @return the client session for this identifier or <null>.
     */
    @Nullable
    ClientSession getSession(@NotNull String clientId, boolean includeWill);

    /**
     * Trigger a cleanup for a specific bucket
     *
     * @param bucketIndex the index of the bucket
     * @return a future which completes as soon as the clean up is done.
     */
    @NotNull
    ListenableFuture<Void> cleanUp(int bucketIndex);

    /**
     * @return a future of all client ids in the persistence.
     */
    @NotNull
    ListenableFuture<Set<String>> getAllClients();

    /**
     * Enforce a client disconnect from a specific source, preventing or delivering the will message.
     *
     * @param clientId          The client id of the client to disconnect.
     * @param preventLwtMessage The flag if the will message should be prevented or delivered.
     * @param source            The source of the enforce call.
     * @return a future of a boolean which gives the information that a client was disconnected (true) or wasn't
     * connected (false).
     */
    @NotNull
    ListenableFuture<Boolean> forceDisconnectClient(@NotNull String clientId, boolean preventLwtMessage, @NotNull DisconnectSource source);

    /**
     * Enforce a client disconnect from a specific source, preventing or delivering the will message.
     *
     * @param clientId          The client id of the client to disconnect.
     * @param preventLwtMessage The flag if the will message should be prevented or delivered.
     * @param source            The source of the enforce call.
     * @param reasonCode        The reason code for the enforced disconnect.
     * @param reasonString      The reason string for the enforced disconnect.
     * @return a future of a boolean which gives the information that a client was disconnected (true) or wasn't
     * connected (false).
     */
    @NotNull
    ListenableFuture<Boolean> forceDisconnectClient(@NotNull String clientId, boolean preventLwtMessage, @NotNull DisconnectSource source,
                                                    @Nullable Mqtt5DisconnectReasonCode reasonCode, @Nullable String reasonString);

    /**
     * Sets the session expiry interval for a client in seconds.
     *
     * @param clientId              the client identifier of the client
     * @param sessionExpiryInterval session expiry interval for the client in seconds
     * @return a {@link com.google.common.util.concurrent.ListenableFuture} which is returned when the Session Expiry
     * Interval is set
     * successfully or a Exception was caught.
     */
    @NotNull
    ListenableFuture<Boolean> setSessionExpiryInterval(@NotNull final String clientId, long sessionExpiryInterval);

    /**
     * Returns the session expiry interval for a client in seconds.
     * <br>
     * {@link UnsignedDataTypes#UNSIGNED_INT_MAX_VALUE} = max value
     * <p>
     * 0 = Session expires on disconnect
     *
     * @param clientId the client identifier of the client
     * @return the {@link Integer} Session expiry interval in seconds.
     */
    @Nullable
    Long getSessionExpiryInterval(@NotNull final String clientId);

    /**
     * Checks if there is a session for a given amount of clients.
     *
     * @param clients to check
     * @return The client id mapped to a boolean that is true if the session exists
     */
    @NotNull
    Map<String, Boolean> isExistent(@NotNull final Set<String> clients);

    /**
     * Invalidates the client session for a client with the given client identifier. If the client is currently
     * connected, it will be disconnected as well.
     * <p>
     * Sets session expiry interval to 0. Attention: The given Session expiry interval of 0 may be overwritten at client
     * reconnection.
     *
     * @param clientId         The client identifier of the client which session should be invalidated
     * @param disconnectSource The source of the invalidate session call.
     * @return a {@link com.google.common.util.concurrent.ListenableFuture} succeeding with a {@link Boolean} which is
     * true when the client has been actively disconnected by the broker otherwise false,
     * @since 3.4
     */
    @NotNull
    ListenableFuture<Boolean> invalidateSession(@NotNull String clientId, @NotNull DisconnectSource disconnectSource);

    /**
     * @return The delay of all will messages that have not been sent yet, mapped to the client id.
     */
    @NotNull
    ListenableFuture<Map<String, PendingWillMessages.PendingWill>> pendingWills();

    /**
     * Remove a will message for a specific client.
     *
     * @param clientId The identifier of the client.
     * @return A future which completes as soon as the will is removed.
     */
    @NotNull
    ListenableFuture<Void> removeWill(@NotNull String clientId);


    /**
     * Process a request for a chunk of all the client sessions from this node
     *
     * @param cursor the cursor returned from the last chunk or a new (empty) cursor to start iterating the persistence
     * @return a result containing the new cursor and a map of clientIds to their session
     */
    @NotNull
    ListenableFuture<MultipleChunkResult<Map<String, ClientSession>>> getAllLocalClientsChunk(@NotNull ChunkCursor cursor);

}
