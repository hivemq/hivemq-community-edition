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
package com.hivemq.persistence.local.xodus.clientsession;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.logging.EventLog;
import com.hivemq.persistence.NoSessionException;
import com.hivemq.persistence.PersistenceEntry;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionWill;
import com.hivemq.persistence.clientsession.PendingWillMessages;
import com.hivemq.persistence.exception.InvalidSessionExpiryIntervalException;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.persistence.local.xodus.XodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.ClientSessions;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.ThreadPreConditions;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.persistence.local.xodus.XodusUtils.byteIterableToBytes;
import static com.hivemq.persistence.local.xodus.XodusUtils.bytesToByteIterable;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * An implementation of the ClientSessionLocalPersistence based on Xodus.
 * <p>
 * This implementation is thread safe and all methods block.
 *
 * @author Dominik Obermaier
 */
@ThreadSafe
@LazySingleton
public class ClientSessionXodusLocalPersistence extends XodusLocalPersistence implements ClientSessionLocalPersistence {

    private static final Logger log = LoggerFactory.getLogger(ClientSessionXodusLocalPersistence.class);

    private static final String PERSISTENCE_NAME = "client_session_store";
    public static final String PERSISTENCE_VERSION = "040000";

    private final @NotNull ClientSessionPersistenceSerializer serializer;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull EventLog eventLog;

    private final AtomicInteger sessionsCount = new AtomicInteger(0);

    @Inject
    ClientSessionXodusLocalPersistence(
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull MqttConfigurationService mqttConfigurationService,
            final @NotNull EnvironmentUtil environmentUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull EventLog eventLog,
            final @NotNull PersistenceStartup persistenceStartup) {

        super(environmentUtil, localPersistenceFileUtil, persistenceStartup, InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get(), true);

        this.payloadPersistence = payloadPersistence;
        this.eventLog = eventLog;
        this.serializer = new ClientSessionPersistenceSerializer();

    }


    @NotNull
    @Override
    protected String getName() {
        return PERSISTENCE_NAME;
    }

    @NotNull
    @Override
    protected String getVersion() {
        return PERSISTENCE_VERSION;
    }

    @NotNull
    @Override
    protected StoreConfig getStoreConfig() {
        return StoreConfig.WITHOUT_DUPLICATES;
    }

    @NotNull
    @Override
    protected Logger getLogger() {
        return log;
    }

    @PostConstruct
    protected void postConstruct() {
        super.postConstruct();
    }

    protected void init() {
        for (int i = 0; i < bucketCount; i++) {
            final Bucket bucket = buckets[i];
            bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                final Store store = bucket.getStore();
                final Cursor cursor = bucket.getStore().openCursor(txn);
                while (cursor.getNext()) {
                    final byte[] bytes = byteIterableToBytes(cursor.getValue());
                    final ClientSession clientSession = serializer.deserializeValue(bytes);
                    if (persistent(clientSession)) {
                        sessionsCount.incrementAndGet();
                    }
                    if (clientSession.getWillPublish() != null) {
                        clientSession.setWillPublish(null);
                        final long timestamp = serializer.deserializeTimestamp(bytes);
                        final byte[] sessionsWithoutWill = serializer.serializeValue(clientSession, timestamp);
                        store.put(txn, cursor.getKey(), bytesToByteIterable(sessionsWithoutWill));
                    }
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClientSession getSession(@NotNull final String clientId, final int bucketIndex, final boolean checkExpired) {
        checkNotNull(clientId, "Client id must not be null");
        checkBucketIndex(bucketIndex);

        return getSession(clientId, buckets[bucketIndex], checkExpired, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClientSession getSession(@NotNull final String clientId, final int bucketIndex) {
        checkNotNull(clientId, "Client id must not be null");
        checkBucketIndex(bucketIndex);

        return getSession(clientId, buckets[bucketIndex], true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public ClientSession getSession(@NotNull final String clientId, final boolean checkExpired) {
        checkNotNull(clientId, "Client id must not be null");

        return getSession(clientId, getBucket(clientId), checkExpired, true);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public ClientSession getSession(@NotNull final String clientId, final boolean checkExpired, final boolean includeWill) {
        checkNotNull(clientId, "Client id must not be null");

        return getSession(clientId, getBucket(clientId), checkExpired, includeWill);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public ClientSession getSession(@NotNull final String clientId) {
        checkNotNull(clientId, "Client id must not be null");

        return getSession(clientId, getBucket(clientId), true, true);
    }

    @Nullable
    private ClientSession getSession(@NotNull final String clientId, final Bucket bucket, final boolean checkExpired, final boolean includeWill) {
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {

            final ByteIterable byteIterable = bucket.getStore().get(txn, bytesToByteIterable(serializer.serializeKey(clientId)));
            if (byteIterable == null) {
                return null;
            }
            final ClientSession clientSession;
            final @NotNull byte[] bytes = byteIterableToBytes(byteIterable);

            if (includeWill) {
                clientSession = serializer.deserializeValue(bytes);
            } else {
                clientSession = serializer.deserializeValueWithoutWill(bytes);
            }

            if (checkExpired && ClientSessions.isExpired(clientSession, System.currentTimeMillis() - serializer.deserializeTimestamp(bytes))) {
                return null;
            }

            if (includeWill) {
                dereferenceWillPayload(clientSession);
            }
            return clientSession;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Long getTimestamp(@NotNull final String clientId) {
        return getTimestamp(clientId, BucketUtils.getBucket(clientId, bucketCount));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Long getTimestamp(@NotNull final String clientId, final int bucketIndex) {
        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            final ByteIterable byteIterable = bucket.getStore().get(txn, bytesToByteIterable(serializer.serializeKey(clientId)));
            if (byteIterable == null) {
                return null;
            }

            return serializer.deserializeTimestamp(byteIterableToBytes(byteIterable));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@NotNull final String clientId, @NotNull final ClientSession clientSession, final long timestamp, final int bucketIndex) {
        checkNotNull(clientId, "Client id must not be null");
        checkNotNull(clientSession, "Client session must not be null");
        checkArgument(timestamp > 0, "Timestamp must be greater than 0");

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInTransaction(txn -> {

            final ByteIterable key = bytesToByteIterable(serializer.serializeKey(clientId));

            final boolean isPersistent = persistent(clientSession);

            final ByteIterable value = bucket.getStore().get(txn, key);
            if (value != null) {
                final ClientSession prevClientSession = serializer.deserializeValue(byteIterableToBytes(value));
                if (prevClientSession.getWillPublish() != null) {
                    removeWillReference(prevClientSession);
                }

                final boolean prevIsPersistent = persistent(prevClientSession);

                if ((isPersistent || clientSession.isConnected()) && (!prevIsPersistent && !prevClientSession.isConnected())) {
                    sessionsCount.incrementAndGet();
                } else if ((prevIsPersistent || prevClientSession.isConnected()) && (!isPersistent && !clientSession.isConnected())) {
                    sessionsCount.decrementAndGet();
                }

            } else if (isPersistent || clientSession.isConnected()) {
                sessionsCount.incrementAndGet();
            }

            bucket.getStore().put(txn, key,
                    bytesToByteIterable(serializer.serializeValue(clientSession, timestamp)));

        });
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public ClientSession disconnect(@NotNull final String clientId, final long timestamp, final boolean sendWill, final int bucketIndex, final long sessionExpiry) {
        checkNotNull(clientId, "Client id must not be null");

        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInTransaction(txn -> {

            final ByteIterable key = bytesToByteIterable(serializer.serializeKey(clientId));
            final ByteIterable byteIterable = bucket.getStore().get(txn, key);

            if (byteIterable == null) {
                // we create a tombstone here which will be removed at next cleanup
                final ClientSession clientSession = new ClientSession(false, SESSION_EXPIRE_ON_DISCONNECT);
                bucket.getStore().put(txn, key, bytesToByteIterable(serializer.serializeValue(clientSession, timestamp)));
                return clientSession;
            }

            final ClientSession clientSession = serializer.deserializeValue(byteIterableToBytes(byteIterable));

            if (sessionExpiry != SESSION_EXPIRY_NOT_SET) {
                clientSession.setSessionExpiryInterval(sessionExpiry);
            }

            if (clientSession.isConnected() && !persistent(clientSession)) {
                sessionsCount.decrementAndGet();
            }
            clientSession.setConnected(false);
            if (!sendWill) {
                removeWillReference(clientSession);
                clientSession.setWillPublish(null);
            }
            bucket.getStore().put(txn, key, bytesToByteIterable(serializer.serializeValue(clientSession, timestamp)));
            dereferenceWillPayload(clientSession);
            return clientSession;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public PersistenceEntry<ClientSession> removeWill(@NotNull final String clientId, final int bucketIndex) {
        checkNotNull(clientId, "Client id must not be null");

        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInTransaction(txn -> {
            final ByteIterable key = bytesToByteIterable(serializer.serializeKey(clientId));
            final ByteIterable byteIterable = bucket.getStore().get(txn, key);

            if (byteIterable == null) {
                return null;
            }

            final ClientSession clientSession = serializer.deserializeValue(byteIterableToBytes(byteIterable));
            // Just to be save
            if (clientSession.isConnected()) {
                return null;
            }
            final long timestamp = serializer.deserializeTimestamp(byteIterableToBytes(byteIterable));
            removeWillReference(clientSession);
            clientSession.setWillPublish(null);
            bucket.getStore().put(txn, key, bytesToByteIterable(serializer.serializeValue(clientSession, timestamp)));
            return new PersistenceEntry<>(clientSession, timestamp);
        });
    }

    @Override
    public @NotNull BucketChunkResult<Map<String, ClientSession>> getAllClientsChunk(final int bucketIndex,
                                                                                     @Nullable final String lastClientId,
                                                                                     final int maxResults) {

        checkBucketIndex(bucketIndex);

        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInTransaction(txn -> {
            final Map<String, ClientSession> resultMap = Maps.newHashMap();

            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                int counter = 0;

                //determine starting point
                if (lastClientId != null) {
                    final ByteIterable lastClientKey = bytesToByteIterable(serializer.serializeKey(lastClientId));
                    final ByteIterable foundKey = cursor.getSearchKeyRange(lastClientKey);
                    if (foundKey == null) {
                        //this key is not in the persistence and no key larger than this key is there anymore
                        return new BucketChunkResult<>(resultMap, true, lastClientId, bucketIndex);
                    } else {
                        if (cursor.getKey().equals(lastClientKey)) {
                            //jump to the next key
                            cursor.getNext();
                        }
                    }
                } else {
                    cursor.getNext();
                }

                String lastKey = lastClientId;

                do {

                    if (cursor.getKey() == ByteIterable.EMPTY) {
                        continue;
                    }

                    final String key = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));
                    lastKey = key;

                    final byte[] valueBytes = byteIterableToBytes(cursor.getValue());

                    final ClientSession clientSession = serializer.deserializeValueWithoutWill(valueBytes);
                    final long timestamp = serializer.deserializeTimestamp(valueBytes);

                    final boolean expired = ClientSessions.isExpired(clientSession, System.currentTimeMillis() - timestamp);
                    if (expired) {
                        continue;
                    }

                    resultMap.put(key, clientSession);
                    counter++;

                    if (counter >= maxResults) {
                        return new BucketChunkResult<>(resultMap, !cursor.getNext(), lastKey, bucketIndex);
                    }

                } while (cursor.getNext());

                return new BucketChunkResult<>(resultMap, true, lastKey, bucketIndex);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Set<String> getAllClients(final int bucketIndex) {
        final ImmutableSet.Builder<String> clientSessions = ImmutableSet.builder();
        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                while (cursor.getNext()) {
                    final String clientId = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));

                    clientSessions.add(clientId);
                }
            }
        });
        return clientSessions.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeWithTimestamp(final @NotNull String client, final int bucketIndex) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInTransaction(txn -> {
            final ByteIterable value = bucket.getStore().get(txn, bytesToByteIterable(serializer.serializeKey(client)));
            if (value != null) {
                final ClientSession clientSession = serializer.deserializeValue(byteIterableToBytes(value));
                if (persistent(clientSession) || clientSession.isConnected()) {
                    sessionsCount.decrementAndGet();
                }
                removeWillReference(clientSession);
            }
            bucket.getStore().delete(txn, bytesToByteIterable(serializer.serializeKey(client)));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSessionExpiryInterval(@NotNull final String clientId, final long sessionExpiryInterval, final int bucketIndex) {
        checkNotNull(clientId, "Client Id must not be null");

        if (sessionExpiryInterval < 0) {
            throw new InvalidSessionExpiryIntervalException("Invalid session expiry interval " + sessionExpiryInterval);
        }

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInTransaction(txn -> {

            final ByteIterable key = bytesToByteIterable(serializer.serializeKey(clientId));

            final ByteIterable valueFromStore = bucket.getStore().get(txn, key);

            if (valueFromStore == null) {
                throw NoSessionException.INSTANCE;
            }

            final ClientSession clientSession = serializer.deserializeValue(byteIterableToBytes(valueFromStore));

            // is tombstone?
            if (!clientSession.isConnected() && !persistent(clientSession)) {
                throw NoSessionException.INSTANCE;
            }

            clientSession.setSessionExpiryInterval(sessionExpiryInterval);

            final ByteIterable value = bytesToByteIterable(serializer.serializeValue(clientSession, System.currentTimeMillis()));

            bucket.getStore().put(txn, key, value);

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Set<String> cleanUp(final int bucketIndex) {

        final ImmutableSet.Builder<String> expiredSessionsBuilder = ImmutableSet.builder();

        if (stopped.get()) {
            return expiredSessionsBuilder.build();
        }
        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInTransaction(txn -> {
            final Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                final String clientId = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));

                final byte[] valueBytes = byteIterableToBytes(cursor.getValue());
                final ClientSession clientSession = serializer.deserializeValue(valueBytes);
                final long timestamp = serializer.deserializeTimestamp(valueBytes);

                final long sessionExpiryInterval = clientSession.getSessionExpiryInterval();
                final long timeSinceDisconnect = System.currentTimeMillis() - timestamp;

                // Expired is true if the persistent date for the client has to be removed
                final boolean expired = ClientSessions.isExpired(clientSession, timeSinceDisconnect);
                if (expired) {
                    if (sessionExpiryInterval > SESSION_EXPIRE_ON_DISCONNECT) {
                        sessionsCount.decrementAndGet();
                    }

                    eventLog.clientSessionExpired(timestamp + sessionExpiryInterval * 1000, clientId);
                    cursor.deleteCurrent();
                    expiredSessionsBuilder.add(clientId);
                }
            }
        });
        return expiredSessionsBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Set<String> getDisconnectedClients(final int bucketIndex) {

        checkBucketIndex(bucketIndex);

        final Bucket bucket = buckets[bucketIndex];

        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {

            final Set<String> collectSet = new HashSet<>();
            final Cursor cursor = bucket.getStore().openCursor(txn);

            while (cursor.getNext()) {
                final byte[] valueBytes = byteIterableToBytes(cursor.getValue());
                final ClientSession clientSession = serializer.deserializeValue(valueBytes);
                final String clientId = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));
                if (!clientSession.isConnected() && clientSession.getSessionExpiryInterval() > 0) {
                    final long timestamp = serializer.deserializeTimestamp(valueBytes);
                    final long timeSinceDisconnect = System.currentTimeMillis() - timestamp;
                    final long sessionExpiryIntervalInMillis = clientSession.getSessionExpiryInterval() * 1000L;
                    // We don't remove expired client sessions here, since this method is often called for all buckets at once.
                    // Handling the TTL in the cleanup job will result in a more evenly distributed CPU usage.
                    if (timeSinceDisconnect < sessionExpiryIntervalInMillis) {
                        collectSet.add(clientId);
                    }
                }
            }
            return collectSet;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSessionsCount() {
        return sessionsCount.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Map<String, PendingWillMessages.PendingWill> getPendingWills(final int bucketIndex) {
        final Bucket bucket = buckets[bucketIndex];

        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            final Map<String, PendingWillMessages.PendingWill> resultMap = new HashMap<>();
            final Cursor cursor = bucket.getStore().openCursor(txn);
            while (cursor.getNext()) {
                final byte[] valueBytes = byteIterableToBytes(cursor.getValue());
                final ClientSession clientSession = serializer.deserializeValue(valueBytes);
                final String clientId = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));
                final long timestamp = serializer.deserializeTimestamp(valueBytes);
                if (clientSession.isConnected() || clientSession.getWillPublish() == null) {
                    // Will must not be sent
                    continue;
                }
                final ClientSessionWill willPublish = clientSession.getWillPublish();
                resultMap.put(clientId, new PendingWillMessages.PendingWill(Math.min(willPublish.getDelayInterval(), clientSession.getSessionExpiryInterval()), timestamp));
            }
            return resultMap;
        });
    }

    private void removeWillReference(final ClientSession clientSession) {
        final ClientSessionWill willPublish = clientSession.getWillPublish();
        if (willPublish == null) {
            return;
        }
        payloadPersistence.decrementReferenceCounter(willPublish.getPublishId());
    }

    private void dereferenceWillPayload(final ClientSession clientSession) {
        final ClientSessionWill willPublish = clientSession.getWillPublish();
        if (willPublish == null) {
            return;
        }
        if (willPublish.getPayload() != null) {
            return;
        }
        final byte[] payload = payloadPersistence.getPayloadOrNull(willPublish.getPublishId());
        if (payload == null) {
            clientSession.setWillPublish(null);
            log.warn("Will Payload for payloadid {} not found", willPublish.getPublishId());
            return;
        }
        willPublish.getMqttWillPublish().setPayload(payload);
    }

    private boolean persistent(final ClientSession clientSession) {
        return clientSession.getSessionExpiryInterval() > SESSION_EXPIRE_ON_DISCONNECT;
    }
}
