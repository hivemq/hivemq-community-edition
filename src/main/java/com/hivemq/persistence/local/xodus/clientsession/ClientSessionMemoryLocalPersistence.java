/*
 * Copyright 2020 dc-square GmbH
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

package com.hivemq.persistence.local.xodus.clientsession;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.logging.EventLog;
import com.hivemq.persistence.PersistenceEntry;
import com.hivemq.persistence.PersistenceFilter;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionWill;
import com.hivemq.persistence.clientsession.PendingWillMessages;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.xodus.BucketChunkResult;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.ClientSessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;

/**
 * @author Georg Held
 */
public class ClientSessionMemoryLocalPersistence implements ClientSessionLocalPersistence {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientSessionMemoryLocalPersistence.class);

    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull EventLog eventLog;

    private final long configuredSessionExpiryInterval;
    private final @NotNull AtomicInteger sessionsCount = new AtomicInteger(0);

    private final int bucketCount;
    private final @NotNull Map<String, StoredSession>[] buckets;

    @Inject
    ClientSessionMemoryLocalPersistence(
            final @NotNull MqttConfigurationService mqttConfigurationService,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull EventLog eventLog) {

        this.payloadPersistence = payloadPersistence;
        this.eventLog = eventLog;
        this.configuredSessionExpiryInterval = mqttConfigurationService.maxSessionExpiryInterval();

        bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
        buckets = new ConcurrentHashMap[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new ConcurrentHashMap<>();
        }
    }

    private @NotNull Map<String, StoredSession> getBucket(final int bucketIndex) {
        checkArgument(bucketIndex <= bucketCount, "Bucket must be less or equal than bucketCount");
        return buckets[bucketIndex];
    }

    @Override
    public @Nullable ClientSession getSession(
            final @NotNull String clientId, final int bucketIndex, final boolean checkExpired) {
        return getSession(clientId, bucketIndex, checkExpired, true);
    }

    @Override
    public @Nullable ClientSession getSession(final @NotNull String clientId, final int bucketIndex) {
        return getSession(clientId, bucketIndex, true, true);
    }

    @Override
    public @Nullable ClientSession getSession(final @NotNull String clientId, final boolean checkExpired) {
        final int bucketIndex = BucketUtils.getBucket(clientId, bucketCount);
        return getSession(clientId, bucketIndex, checkExpired, true);
    }

    @Override
    public @Nullable ClientSession getSession(
            final @NotNull String clientId, final boolean checkExpired, final boolean includeWill) {
        final int bucketIndex = BucketUtils.getBucket(clientId, bucketCount);
        return getSession(clientId, bucketIndex, checkExpired, includeWill);
    }

    @Override
    public @Nullable ClientSession getSession(final @NotNull String clientId) {
        final int bucketIndex = BucketUtils.getBucket(clientId, bucketCount);
        return getSession(clientId, bucketIndex, true, true);
    }

    private @Nullable ClientSession getSession(
            final @NotNull String clientId,
            final int bucketIndex,
            final boolean checkExpired,
            final boolean includeWill) {

        final Map<String, StoredSession> bucket = getBucket(bucketIndex);

        final StoredSession storedSession = bucket.get(clientId);
        if (storedSession == null) {
            return null;
        }

        final ClientSession clientSession = storedSession.getClientSession();

        if (checkExpired &&
                ClientSessions.isExpired(clientSession, System.currentTimeMillis() - storedSession.getTimestamp())) {
            return null;
        }
        if (includeWill) {
            dereferenceWillPayload(clientSession);
        }
        return clientSession;
    }

    @Override
    public @Nullable Long getTimestamp(final @NotNull String clientId) {
        final int bucketIndex = BucketUtils.getBucket(clientId, bucketCount);
        return getTimestamp(clientId, bucketIndex);
    }

    @Override
    public @Nullable Long getTimestamp(final @NotNull String clientId, final int bucketIndex) {

        final Map<String, StoredSession> bucket = getBucket(bucketIndex);

        final StoredSession storedSession = bucket.get(clientId);
        if (storedSession != null) {
            return storedSession.getTimestamp();
        }
        return null;
    }

    @Override
    public void put(
            final @NotNull String clientId,
            final @NotNull ClientSession clientSession,
            final long timestamp,
            final int bucketIndex) {
        checkNotNull(clientId, "Client id must not be null");
        checkNotNull(clientSession, "Client session must not be null");
        checkArgument(timestamp > 0, "Timestamp must be greater than 0");

        final Map<String, StoredSession> sessions = getBucket(bucketIndex);
        final boolean isPersistent = persistent(clientSession);

        sessions.compute(clientId, (ignored, storedSession) -> {

            if (storedSession != null) {
                final ClientSession oldSession = storedSession.getClientSession();
                if (oldSession.getWillPublish() != null) {
                    removeWillReference(oldSession);
                }

                final boolean prevIsPersistent = persistent(oldSession);

                if ((isPersistent || clientSession.isConnected()) && (!prevIsPersistent && !oldSession.isConnected())) {
                    sessionsCount.incrementAndGet();
                } else if ((prevIsPersistent || oldSession.isConnected()) &&
                        (!isPersistent && !clientSession.isConnected())) {
                    sessionsCount.decrementAndGet();
                }

            } else if (isPersistent || clientSession.isConnected()) {
                sessionsCount.incrementAndGet();
            }

            // We remove the payload of the MqttWillPublish for storage.
            // It was already put into the PayloadPersistence in
            // ClientSessionPersistence.clientConnected().
            final ClientSessionWill willPublish = clientSession.getWillPublish();
            if (willPublish != null) {
                willPublish.getMqttWillPublish().setPayload(null);
            }

            return new StoredSession(clientSession, timestamp);
        });
    }

    @Override
    public @NotNull ClientSession disconnect(
            final @NotNull String clientId,
            final long timestamp,
            final boolean sendWill,
            final int bucketIndex,
            final long expiry) {

        final Map<String, StoredSession> bucket = getBucket(bucketIndex);

        final StoredSession storedSession = bucket.compute(clientId, (ignored, oldSession) -> {

            if (oldSession == null) {
                // we create a tombstone here
                final ClientSession clientSession = new ClientSession(false, configuredSessionExpiryInterval);
                return new StoredSession(clientSession, timestamp);
            }

            final ClientSession clientSession = oldSession.getClientSession();

            if (expiry != SESSION_EXPIRY_NOT_SET) {
                clientSession.setSessionExpiryInterval(expiry);
            }

            if (clientSession.isConnected() && !persistent(clientSession)) {
                sessionsCount.decrementAndGet();
            }

            clientSession.setConnected(false);

            if (!sendWill) {
                removeWillReference(clientSession);
                clientSession.setWillPublish(null);
            }

            dereferenceWillPayload(clientSession);
            return oldSession;
        });

        return storedSession.getClientSession();
    }

    @Override
    public @NotNull Set<String> getAllClients(int bucketIndex) {
        return null;
    }

    @Override
    public void removeWithTimestamp(@NotNull String client, int bucketIdx) {

    }

    @Override
    public @NotNull Set<String> cleanUp(int bucketIndex) {
        return null;
    }

    @Override
    public @NotNull Set<String> getDisconnectedClients(int bucketIndex) {
        return null;
    }

    @Override
    public int getSessionsCount() {
        return 0;
    }

    @Override
    public void setSessionExpiryInterval(@NotNull String clientId, long sessionExpiryInterval, int bucketIndex) {

    }

    @Override
    public @NotNull Long getSessionExpiryInterval(@NotNull String clientId) {
        return null;
    }

    @Override
    public @NotNull Map<String, PendingWillMessages.PendingWill> getPendingWills(
            int bucketIndex) {
        return null;
    }

    @Override
    public @Nullable PersistenceEntry<ClientSession> removeWill(
            @NotNull String clientId, int bucketIndex) {
        return null;
    }

    @Override
    public @NotNull BucketChunkResult<Map<String, ClientSession>> getAllClientsChunk(
            @NotNull PersistenceFilter filter, int bucketIndex, @Nullable String lastClientId, int maxResults) {
        return null;
    }

    @Override
    public void closeDB(int bucketIndex) {

    }

    private void removeWillReference(final @NotNull ClientSession clientSession) {
        final ClientSessionWill willPublish = clientSession.getWillPublish();
        if (willPublish == null) {
            return;
        }
        payloadPersistence.decrementReferenceCounter(willPublish.getPayloadId());
    }

    private void dereferenceWillPayload(final @NotNull ClientSession clientSession) {
        final ClientSessionWill willPublish = clientSession.getWillPublish();
        if (willPublish == null) {
            return;
        }
        if (willPublish.getPayload() != null) {
            return;
        }
        final Long payloadId = willPublish.getPayloadId();
        final byte[] payload = payloadPersistence.getPayloadOrNull(payloadId);
        if (payload == null) {
            clientSession.setWillPublish(null);
            log.warn("Will Payload for payloadid {} not found", payloadId);
            return;
        }
        willPublish.getMqttWillPublish().setPayload(payload);
    }

    private boolean persistent(final @NotNull ClientSession clientSession) {
        return clientSession.getSessionExpiryInterval() > SESSION_EXPIRE_ON_DISCONNECT;
    }

    @Immutable
    private static class StoredSession {

        private final @NotNull ClientSession clientSession;
        private final @NotNull long timestamp;

        private StoredSession(final @NotNull ClientSession clientSession, final @NotNull long timestamp) {
            this.clientSession = clientSession;
            this.timestamp = timestamp;
        }

        public @NotNull ClientSession getClientSession() {
            return clientSession;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
