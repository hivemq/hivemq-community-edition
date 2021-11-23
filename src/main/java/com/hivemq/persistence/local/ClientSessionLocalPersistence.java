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
package com.hivemq.persistence.local;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.annotations.ExecuteInSingleWriter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.persistence.LocalPersistence;
import com.hivemq.persistence.PersistenceEntry;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.PendingWillMessages;
import com.hivemq.persistence.exception.InvalidSessionExpiryIntervalException;

import java.util.Map;
import java.util.Set;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
public interface ClientSessionLocalPersistence extends LocalPersistence {

    /**
     * Get a {@link ClientSession} for a specific client id with an expired check.
     *
     * @param clientId The id associated with the session
     * @return A {@link ClientSession} object or {@code null} if there is no
     * session stored for the given id
     */
    @Nullable ClientSession getSession(@NotNull String clientId);

    /**
     * Get a {@link ClientSession} for a specific client id and a bucket index with an expired check.
     *
     * @param clientId    The id associated with the session
     * @param bucketIndex The index of the bucket in which the session is stored
     * @return A {@link ClientSession} object or {@code null} if there is no
     * session stored for the given id
     */
    @Nullable ClientSession getSession(@NotNull String clientId, int bucketIndex);

    /**
     * Get a {@link ClientSession} for a specific client id with an optional expired check.
     *
     * @param clientId     The id associated with the session
     * @param checkExpired true => return null for expired session, false return tombstones and expired sessions
     * @return A {@link ClientSession} object or {@code null} if there is no
     * session stored for the given id
     */
    @Nullable ClientSession getSession(@NotNull String clientId, boolean checkExpired);

    /**
     * Get a {@link ClientSession} for a specific client id and a bucket index with an expired check.
     *
     * @param clientId     The id associated with the session
     * @param bucketIndex  The index of the bucket in which the session is stored
     * @param checkExpired true => return null for expired session, false return tombstones and expired sessions
     * @return A {@link ClientSession} object or {@code null} if there is no
     * session stored for the given id
     */
    @Nullable ClientSession getSession(@NotNull String clientId, int bucketIndex, boolean checkExpired);

    /**
     * Get a {@link ClientSession} for a specific client id with an optional expired check.
     *
     * @param clientId     The id associated with the session
     * @param checkExpired true => return null for expired session, false return tombstones and expired sessions
     * @param includeWill  if the will message should be included, the will is set to {@code null}
     *                     if this parameter is {@code false}.
     * @return A {@link ClientSession} object or {@code null} if there is no session stored for the given id
     */
    @Nullable ClientSession getSession(@NotNull String clientId, boolean checkExpired, boolean includeWill);

    /**
     * @param clientId The id associated with the session
     * @return The timestamp of the client session (last connected/disconnected) or {@code null} if there is no session
     * stored for the given id
     */
    @Nullable Long getTimestamp(@NotNull String clientId);

    /**
     * @param clientId    The id associated with the session
     * @param bucketIndex The index of the bucket in which the session is stored
     * @return The timestamp of the client session (last connected/disconnected) or null if there is no session stored
     * for the given id
     */
    @Nullable Long getTimestamp(@NotNull String clientId, int bucketIndex);

    /**
     * Put a {@link ClientSession} for specific client id, timestamp and persistence bucket index into the persistence.
     *
     * @param clientId      The id associated with the session.
     * @param clientSession The {@link ClientSession} to put.
     * @param timestamp     The timestamp of the latest connect/disconnect.
     * @param bucketIndex   The index of the bucket in which the session is stored
     */
    @ExecuteInSingleWriter
    void put(@NotNull String clientId, @NotNull ClientSession clientSession, long timestamp, int bucketIndex);

    /**
     * Set a {@link ClientSession} to disconnected.
     *
     * @param clientId              The id associated with the session.
     * @param sendWill              The flag to send Will or not.
     * @param timestamp             The timestamp of the disconnect.
     * @param bucketIndex           The index of the bucket in which the session is stored.
     * @param sessionExpiryInterval The session expiry interval.
     * @return the disconnected {@link ClientSession}.
     */
    @ExecuteInSingleWriter
    @NotNull ClientSession disconnect(
            @NotNull String clientId, long timestamp, boolean sendWill, int bucketIndex, long sessionExpiryInterval);

    /**
     * get all client identifiers of all stored clients form a specific persistence bucket.
     *
     * @param bucketIndex The index of the bucket in which the client sessions are stored.
     * @return A set of strings containing all client identifiers found.
     */
    @NotNull Set<@NotNull String> getAllClients(int bucketIndex);

    /**
     * Completely remove the session for the client. This will not create a tombstone.
     */
    @VisibleForTesting
    @ExecuteInSingleWriter
    void removeWithTimestamp(@NotNull String client, int bucketIndex);

    /**
     * Trigger a cleanup for a specific persistence bucket.
     *
     * @param bucketIndex The index of the bucket in which the client sessions are stored.
     * @return A set of strings containing all client identifiers which were cleaned up.
     */
    @ExecuteInSingleWriter
    @NotNull Set<@NotNull String> cleanUp(int bucketIndex);

    /**
     * get all client identifiers of all stored disconnected clients form a specific persistence bucket.
     *
     * @param bucketIndex The index of the bucket in which the client sessions are stored.
     * @return A set of strings containing all client identifiers of the found disconnected client sessions.
     */
    @VisibleForTesting
    @NotNull Set<@NotNull String> getDisconnectedClients(int bucketIndex);

    /**
     * @return the amount of not expired client sessions.
     */
    int getSessionsCount();

    /**
     * Sets the session expiry interval for a client in seconds.
     *
     * @param clientId              the client identifier of the client
     * @param sessionExpiryInterval session expiry interval for a client in seconds
     * @throws InvalidSessionExpiryIntervalException when interval < 0
     */
    @ExecuteInSingleWriter
    void setSessionExpiryInterval(@NotNull String clientId, long sessionExpiryInterval, int bucketIndex);

    /**
     * @return the delay of all wills of disconnected clients that have not been sent yet. The key is the client id
     */
    @NotNull Map<String, PendingWillMessages.PendingWill> getPendingWills(int bucketIndex);

    /**
     * Remove Will message of a client session.
     */
    @ExecuteInSingleWriter
    @Nullable PersistenceEntry<@NotNull ClientSession> deleteWill(@NotNull String clientId, int bucketIndex);

    /**
     * Gets a chunk of client sessions from the persistence.
     * <p>
     * The session do not include the Will message. It is always set to {@code null} in the returned sessions.
     *
     * @param bucketIndex  the bucket index
     * @param lastClientId the last client identifier for this chunk. Pass {@code null} to start at the beginning.
     * @param maxResults   the max amount of results contained in the chunk.
     * @return a {@link BucketChunkResult} with the entries and the information if more chunks are available
     */
    @NotNull BucketChunkResult<Map<String, ClientSession>> getAllClientsChunk(
            int bucketIndex, @Nullable String lastClientId, int maxResults);
}
