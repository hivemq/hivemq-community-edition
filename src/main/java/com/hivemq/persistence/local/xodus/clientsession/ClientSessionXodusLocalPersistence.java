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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.persistence.NoSessionException;
import com.hivemq.persistence.PersistenceEntry;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionWill;
import com.hivemq.persistence.clientsession.PendingWillMessages;
import com.hivemq.persistence.exception.InvalidSessionExpiryIntervalException;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.persistence.local.xodus.TransactionCommitActions;
import com.hivemq.persistence.local.xodus.XodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
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
import static com.hivemq.mqtt.message.disconnect.DISCONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.persistence.local.xodus.XodusUtils.byteIterableToBytes;
import static com.hivemq.persistence.local.xodus.XodusUtils.bytesToByteIterable;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * An implementation of the ClientSessionLocalPersistence based on Xodus.
 * <p>
 * This implementation is thread safe and all methods block.
 */
@ThreadSafe
@LazySingleton
public class ClientSessionXodusLocalPersistence extends XodusLocalPersistence implements ClientSessionLocalPersistence {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientSessionXodusLocalPersistence.class);

    private static final @NotNull String PERSISTENCE_NAME = "client_session_store";
    public static final @NotNull String PERSISTENCE_VERSION = "040000";

    private final @NotNull ClientSessionPersistenceSerializer serializer;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull EventLog eventLog;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull AtomicInteger sessionsCount = new AtomicInteger(0);

    @Inject
    ClientSessionXodusLocalPersistence(
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull EnvironmentUtil environmentUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull EventLog eventLog,
            final @NotNull PersistenceStartup persistenceStartup,
            final @NotNull MetricsHolder metricsHolder) {

        super(environmentUtil,
                localPersistenceFileUtil,
                persistenceStartup,
                InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get(),
                true);

        this.payloadPersistence = payloadPersistence;
        this.eventLog = eventLog;
        this.metricsHolder = metricsHolder;
        serializer = new ClientSessionPersistenceSerializer();
    }

    @Override
    protected @NotNull String getName() {
        return PERSISTENCE_NAME;
    }

    @Override
    protected @NotNull String getVersion() {
        return PERSISTENCE_VERSION;
    }

    @Override
    protected @NotNull StoreConfig getStoreConfig() {
        return StoreConfig.WITHOUT_DUPLICATES;
    }

    @Override
    protected @NotNull Logger getLogger() {
        return log;
    }

    @PostConstruct
    protected void postConstruct() {
        super.postConstruct();
    }

    protected void init() {
        for (int i = 0; i < bucketCount; i++) {
            final Bucket bucket = buckets[i];
            final SessionCounterDelta sessionCounterDelta = new SessionCounterDelta();
            bucket.getEnvironment().executeInExclusiveTransaction(txn -> {

                final Store store = bucket.getStore();

                try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                    final TransactionCommitActions commitActions = TransactionCommitActions.asCommitHookFor(txn);
                    while (cursor.getNext()) {
                        final byte[] bytes = byteIterableToBytes(cursor.getValue());
                        final ClientSession clientSession = serializer.deserializeValue(bytes);
                        if (persistent(clientSession)) {
                            sessionCounterDelta.increment();
                        }
                        final ClientSessionWill will = clientSession.getWillPublish();
                        if (will != null) {
                            commitActions.add(() -> {
                                // Workaround?
                                // Since we are starting HiveMQ stateful the PublishPayloadPersistence has no references
                                // to any stored payloads. In order to delete the payload we need to create a reference
                                // and remove it again.
                                payloadPersistence.incrementReferenceCounterOnBootstrap(will.getPublishId());
                                payloadPersistence.decrementReferenceCounter(will.getPublishId());
                            });
                            clientSession.setWillPublish(null);
                            final long timestamp = serializer.deserializeTimestamp(bytes);
                            final byte[] sessionsWithoutWill = serializer.serializeValue(clientSession, timestamp);
                            store.put(txn, cursor.getKey(), bytesToByteIterable(sessionsWithoutWill));
                        }
                    }
                }
            });
            // Ideally, this should be executed by Xodus as soon as the transaction is done.
            // But transaction hooks are only executed if there actually was an update,
            // which only happens if at least one session has a will.
            // Therefore, we could either
            // - increment continuously during the iteration in the transaction, or
            // - increment once after the transaction is done.
            // Let's go with the latter to better signal what's happening:
            // All session updates are also committed at once, if there are any.
            sessionCounterDelta.run();
        }
    }

    @Override
    public @Nullable ClientSession getSession(final @NotNull String clientId) {
        checkNotNull(clientId, "Client id must not be null");

        return getSession(clientId, getBucket(clientId), true, true);
    }

    @Override
    public @Nullable ClientSession getSession(final @NotNull String clientId, final int bucketIndex) {
        checkNotNull(clientId, "Client id must not be null");
        checkBucketIndex(bucketIndex);

        return getSession(clientId, buckets[bucketIndex], true, true);
    }

    @Override
    public @Nullable ClientSession getSession(final @NotNull String clientId, final boolean checkExpired) {
        checkNotNull(clientId, "Client id must not be null");

        return getSession(clientId, getBucket(clientId), checkExpired, true);
    }

    @Override
    public @Nullable ClientSession getSession(
            final @NotNull String clientId, final int bucketIndex, final boolean checkExpired) {

        checkNotNull(clientId, "Client id must not be null");
        checkBucketIndex(bucketIndex);

        return getSession(clientId, buckets[bucketIndex], checkExpired, true);
    }

    @Override
    public @Nullable ClientSession getSession(
            final @NotNull String clientId, final boolean checkExpired, final boolean includeWill) {

        checkNotNull(clientId, "Client id must not be null");

        return getSession(clientId, getBucket(clientId), checkExpired, includeWill);
    }

    private @Nullable ClientSession getSession(
            final @NotNull String clientId,
            final Bucket bucket,
            final boolean checkExpired,
            final boolean includeWill) {

        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {

            final ByteIterable byteIterable =
                    bucket.getStore().get(txn, bytesToByteIterable(serializer.serializeKey(clientId)));
            if (byteIterable == null) {
                return null;
            }
            final ClientSession clientSession;
            final byte @NotNull [] bytes = byteIterableToBytes(byteIterable);

            if (includeWill) {
                clientSession = serializer.deserializeValue(bytes);
            } else {
                clientSession = serializer.deserializeValueWithoutWill(bytes);
            }

            if (checkExpired &&
                    clientSession.isExpired(System.currentTimeMillis() - serializer.deserializeTimestamp(bytes))) {
                return null;
            }

            if (includeWill) {
                loadWillPayload(clientSession);
            }
            return clientSession;
        });
    }

    @Override
    public @Nullable Long getTimestamp(final @NotNull String clientId) {
        return getTimestamp(clientId, BucketUtils.getBucket(clientId, bucketCount));
    }

    @Override
    public @Nullable Long getTimestamp(final @NotNull String clientId, final int bucketIndex) {
        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {

            final ByteIterable byteIterable =
                    bucket.getStore().get(txn, bytesToByteIterable(serializer.serializeKey(clientId)));
            if (byteIterable == null) {
                return null;
            }

            return serializer.deserializeTimestamp(byteIterableToBytes(byteIterable));
        });
    }

    @Override
    public void put(
            final @NotNull String clientId,
            final @NotNull ClientSession newClientSession,
            final long timestamp,
            final int bucketIndex) {
        checkNotNull(clientId, "Client id must not be null");
        checkNotNull(newClientSession, "Client session must not be null");
        checkArgument(timestamp > 0, "Timestamp must be greater than 0");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            final ByteIterable key = bytesToByteIterable(serializer.serializeKey(clientId));

            final boolean isPersistent = persistent(newClientSession);

            final ByteIterable value = bucket.getStore().get(txn, key);
            txn.setCommitHook(() -> {
                if (value == null) {
                    if (isPersistent || newClientSession.isConnected()) {
                        sessionsCount.incrementAndGet();
                    }

                    final ClientSessionWill newWill = newClientSession.getWillPublish();
                    if (newWill != null) {
                        addWillReference(newWill);
                    }
                } else {
                    final ClientSession prevClientSession = serializer.deserializeValue(byteIterableToBytes(value));

                    handleWillPayloads(prevClientSession.getWillPublish(), newClientSession.getWillPublish());

                    final boolean prevIsPersistent = persistent(prevClientSession);

                    if ((isPersistent || newClientSession.isConnected()) &&
                            (!prevIsPersistent && !prevClientSession.isConnected())) {
                        sessionsCount.incrementAndGet();
                    } else if ((prevIsPersistent || prevClientSession.isConnected()) &&
                            (!isPersistent && !newClientSession.isConnected())) {
                        sessionsCount.decrementAndGet();
                    }
                }
            });

            bucket.getStore()
                    .put(txn, key, bytesToByteIterable(serializer.serializeValue(newClientSession, timestamp)));
        });
    }

    @Override
    public @NotNull ClientSession disconnect(
            final @NotNull String clientId,
            final long timestamp,
            final boolean sendWill,
            final int bucketIndex,
            final long sessionExpiryInterval) {

        checkNotNull(clientId, "Client id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInExclusiveTransaction(txn -> {
            final ByteIterable key = bytesToByteIterable(serializer.serializeKey(clientId));
            final ByteIterable byteIterable = bucket.getStore().get(txn, key);

            if (byteIterable == null) {
                // we create a tombstone here which will be removed at next cleanup
                final ClientSession clientSession = new ClientSession(false, SESSION_EXPIRE_ON_DISCONNECT);
                bucket.getStore()
                        .put(txn, key, bytesToByteIterable(serializer.serializeValue(clientSession, timestamp)));
                return clientSession;
            }

            final ClientSession clientSession = serializer.deserializeValue(byteIterableToBytes(byteIterable));

            if (sessionExpiryInterval != SESSION_EXPIRY_NOT_SET) {
                clientSession.setSessionExpiryIntervalSec(sessionExpiryInterval);
            }

            final boolean isConnected = clientSession.isConnected();
            final ClientSessionWill will = clientSession.getWillPublish();
            txn.setCommitHook(() -> {
                if (isConnected && !persistent(clientSession)) {
                    sessionsCount.decrementAndGet();
                }
                if (!sendWill && will != null) {
                    removeWillReference(will);
                }
            });
            clientSession.setConnected(false);
            if (!sendWill && will != null) {
                clientSession.setWillPublish(null);
            }
            bucket.getStore().put(txn, key, bytesToByteIterable(serializer.serializeValue(clientSession, timestamp)));
            loadWillPayload(clientSession);
            return clientSession;
        });
    }

    @Override
    public @Nullable PersistenceEntry<ClientSession> deleteWill(final @NotNull String clientId, final int bucketIndex) {
        checkNotNull(clientId, "Client id must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInExclusiveTransaction(txn -> {
            final ByteIterable key = bytesToByteIterable(serializer.serializeKey(clientId));
            final ByteIterable byteIterable = bucket.getStore().get(txn, key);

            if (byteIterable == null) {
                return null;
            }

            final ClientSession clientSession = serializer.deserializeValue(byteIterableToBytes(byteIterable));
            // Just to be safe.
            if (clientSession.isConnected()) {
                return null;
            }
            final long timestamp = serializer.deserializeTimestamp(byteIterableToBytes(byteIterable));
            final ClientSessionWill will = clientSession.getWillPublish();
            if (will != null) {
                txn.setCommitHook(() -> removeWillReference(will));
                clientSession.setWillPublish(null);
                bucket.getStore()
                        .put(txn, key, bytesToByteIterable(serializer.serializeValue(clientSession, timestamp)));
            }
            return new PersistenceEntry<>(clientSession, timestamp);
        });
    }

    @Override
    public @NotNull BucketChunkResult<Map<String, ClientSession>> getAllClientsChunk(
            final int bucketIndex, final @Nullable String lastClientId, final int maxResults) {
        checkBucketIndex(bucketIndex);

        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
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

                    final boolean expired = clientSession.isExpired(System.currentTimeMillis() - timestamp);
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

    @Override
    public @NotNull Set<String> getAllClients(final int bucketIndex) {
        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            final ImmutableSet.Builder<String> clientSessions = ImmutableSet.builder();
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                while (cursor.getNext()) {
                    final String clientId = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));
                    clientSessions.add(clientId);
                }
            }
            return clientSessions.build();
        });
    }

    @VisibleForTesting
    void removeWithTimestamp(final @NotNull String client, final int bucketIndex) {
        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            final ByteIterable value = bucket.getStore().get(txn, bytesToByteIterable(serializer.serializeKey(client)));
            if (value != null) {
                final ClientSession clientSession = serializer.deserializeValue(byteIterableToBytes(value));
                txn.setCommitHook(() -> {
                    if (persistent(clientSession) || clientSession.isConnected()) {
                        sessionsCount.decrementAndGet();
                    }
                    if (clientSession.getWillPublish() != null) {
                        removeWillReference(clientSession.getWillPublish());
                    }
                });
                bucket.getStore().delete(txn, bytesToByteIterable(serializer.serializeKey(client)));
            }
        });
    }

    @Override
    public void setSessionExpiryInterval(
            final @NotNull String clientId, final long sessionExpiryInterval, final int bucketIndex) {
        checkNotNull(clientId, "Client Id must not be null");

        if (sessionExpiryInterval < 0) {
            throw new InvalidSessionExpiryIntervalException("Invalid session expiry interval " + sessionExpiryInterval);
        }

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {

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

            clientSession.setSessionExpiryIntervalSec(sessionExpiryInterval);

            bucket.getStore()
                    .put(txn,
                            key,
                            bytesToByteIterable(serializer.serializeValue(clientSession, System.currentTimeMillis())));
        });
    }

    @Override
    public @NotNull Set<String> cleanUp(final int bucketIndex) {
        if (stopped.get()) {
            return ImmutableSet.of();
        }
        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInExclusiveTransaction(txn -> {
            final ImmutableSet.Builder<String> expiredSessionsBuilder = ImmutableSet.builder();
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                final TransactionCommitActions commitActions = TransactionCommitActions.asCommitHookFor(txn);
                final SessionCounterDelta sessionCounterDelta = new SessionCounterDelta();
                commitActions.add(sessionCounterDelta);
                while (cursor.getNext()) {
                    final String clientId = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));

                    final byte[] valueBytes = byteIterableToBytes(cursor.getValue());
                    final ClientSession clientSession = serializer.deserializeValue(valueBytes);
                    final long timestamp = serializer.deserializeTimestamp(valueBytes);

                    final long sessionExpiryInterval = clientSession.getSessionExpiryIntervalSec();
                    final long timeSinceDisconnect = System.currentTimeMillis() - timestamp;

                    // Expired is true if the persistent data for the client has to be removed
                    if (clientSession.isExpired(timeSinceDisconnect)) {
                        if (sessionExpiryInterval > SESSION_EXPIRE_ON_DISCONNECT) {
                            sessionCounterDelta.decrement();
                        }
                        commitActions.add(() -> eventLog.clientSessionExpired(timestamp + sessionExpiryInterval * 1000,
                                clientId));
                        cursor.deleteCurrent();
                        expiredSessionsBuilder.add(clientId);
                    }
                }
            }
            return expiredSessionsBuilder.build();
        });
    }

    @Override
    public @NotNull Set<String> getDisconnectedClients(final int bucketIndex) {
        checkBucketIndex(bucketIndex);

        final Bucket bucket = buckets[bucketIndex];

        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {

            final Set<String> collectSet = new HashSet<>();
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                while (cursor.getNext()) {
                    final byte[] valueBytes = byteIterableToBytes(cursor.getValue());
                    final ClientSession clientSession = serializer.deserializeValue(valueBytes);
                    final String clientId = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));
                    if (!clientSession.isConnected() && clientSession.getSessionExpiryIntervalSec() > 0) {
                        final long timestamp = serializer.deserializeTimestamp(valueBytes);
                        final long timeSinceDisconnect = System.currentTimeMillis() - timestamp;
                        final long sessionExpiryIntervalInMillis = clientSession.getSessionExpiryIntervalSec() * 1000L;
                        // We don't remove expired client sessions here, since this method is often called for all buckets at once.
                        // Handling the TTL in the cleanup job will result in a more evenly distributed CPU usage.
                        if (timeSinceDisconnect < sessionExpiryIntervalInMillis) {
                            collectSet.add(clientId);
                        }
                    }
                }
            }
            return collectSet;
        });
    }

    @Override
    public int getSessionsCount() {
        return sessionsCount.get();
    }

    @Override
    public @NotNull Map<String, PendingWillMessages.PendingWill> getPendingWills(final int bucketIndex) {
        final Bucket bucket = buckets[bucketIndex];

        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {
            final Map<String, PendingWillMessages.PendingWill> resultMap = new HashMap<>();
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
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
                    resultMap.put(clientId,
                            new PendingWillMessages.PendingWill(Math.min(willPublish.getDelayInterval(),
                                    clientSession.getSessionExpiryIntervalSec()), timestamp));
                }
            }
            return resultMap;
        });
    }

    private void loadWillPayload(final @NotNull ClientSession clientSession) {
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
            log.warn("Will Payload for payloadId {} not found", willPublish.getPublishId());
            return;
        }
        willPublish.getMqttWillPublish().setPayload(payload);
    }

    private void handleWillPayloads(
            final @Nullable ClientSessionWill previousWill, final @Nullable ClientSessionWill currentWill) {
        if (previousWill != null && currentWill != null) {
            // When equal we have the payload already.
            if (previousWill.getPublishId() != currentWill.getPublishId()) {
                payloadPersistence.decrementReferenceCounter(previousWill.getPublishId());
                payloadPersistence.add(currentWill.getPayload(), currentWill.getPublishId());
            }
        } else {
            if (previousWill != null) {
                removeWillReference(previousWill);
            }
            if (currentWill != null) {
                addWillReference(currentWill);
            }
        }
    }

    private static boolean persistent(final @NotNull ClientSession clientSession) {
        return clientSession.getSessionExpiryIntervalSec() > SESSION_EXPIRE_ON_DISCONNECT;
    }

    private void addWillReference(final @NotNull ClientSessionWill will) {
        metricsHolder.getStoredWillMessagesCount().inc();
        payloadPersistence.add(will.getPayload(), will.getPublishId());
    }

    private void removeWillReference(final @NotNull ClientSessionWill will) {
        metricsHolder.getStoredWillMessagesCount().dec();
        payloadPersistence.decrementReferenceCounter(will.getPublishId());
    }

    private class SessionCounterDelta implements Runnable {

        private int delta;

        private void increment() {
            delta++;
        }

        private void decrement() {
            delta--;
        }

        @Override
        public void run() {
            sessionsCount.addAndGet(delta);
        }
    }
}
