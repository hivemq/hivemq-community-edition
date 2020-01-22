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

package com.hivemq.persistence.clientsession;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.persistence.*;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.task.ClientSessionCleanUpTask;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.xodus.BucketChunkResult;
import com.hivemq.persistence.local.xodus.MultipleChunkResult;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ClientSessions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;

/**
 * @author Lukas Brandl
 */
@LazySingleton
public class ClientSessionPersistenceImpl extends AbstractPersistence implements ClientSessionPersistence {

    private static final Logger log = LoggerFactory.getLogger(ClientSessionPersistenceImpl.class);

    private final @NotNull ClientSessionLocalPersistence localPersistence;
    private final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull ProducerQueues singleWriter;
    private final @NotNull ChannelPersistence channelPersistence;
    private final @NotNull EventLog eventLog;
    private final @NotNull PublishPayloadPersistence publishPayloadPersistence;

    private final @NotNull PendingWillMessages pendingWillMessages;
    private final @NotNull Mqtt5ServerDisconnector mqtt5ServerDisconnector;
    private final @NotNull Mqtt3ServerDisconnector mqtt3ServerDisconnector;

    @Inject
    public ClientSessionPersistenceImpl(final @NotNull ClientSessionLocalPersistence localPersistence,
                                        final @NotNull ClientSessionSubscriptionPersistence sessionSubscriptionPersistence,
                                        final @NotNull ClientQueuePersistence clientQueuePersistence,
                                        final @NotNull SingleWriterService singleWriterService,
                                        final @NotNull ChannelPersistence channelPersistence,
                                        final @NotNull EventLog eventLog,
                                        final @NotNull PublishPayloadPersistence publishPayloadPersistence,
                                        final @NotNull PendingWillMessages pendingWillMessages,
                                        final @NotNull Mqtt5ServerDisconnector mqtt5ServerDisconnector,
                                        final @NotNull Mqtt3ServerDisconnector mqtt3ServerDisconnector) {


        this.localPersistence = localPersistence;
        this.subscriptionPersistence = sessionSubscriptionPersistence;
        this.clientQueuePersistence = clientQueuePersistence;
        singleWriter = singleWriterService.getClientSessionQueue();

        this.channelPersistence = channelPersistence;
        this.eventLog = eventLog;
        this.publishPayloadPersistence = publishPayloadPersistence;
        this.pendingWillMessages = pendingWillMessages;

        this.mqtt5ServerDisconnector = mqtt5ServerDisconnector;
        this.mqtt3ServerDisconnector = mqtt3ServerDisconnector;
    }

    @Override
    public boolean isExistent(@NotNull final String client) {
        checkNotNull(client, "Client id must not be null");

        return isExistent(getSession(client, false));
    }

    private boolean isExistent(@Nullable final ClientSession clientSession) {
        return (clientSession != null) && (clientSession.getSessionExpiryInterval() > 0 || clientSession.isConnected());
    }

    @NotNull
    @Override
    public Map<String, Boolean> isExistent(@NotNull final Set<String> clients) {
        final ImmutableMap.Builder<String, Boolean> builder = ImmutableMap.builder();

        for (final String client : clients) {
            builder.put(client, isExistent(client));
        }

        return builder.build();
    }

    @NotNull
    @Override
    public ListenableFuture<Void> clientDisconnected(@NotNull final String client, final boolean sendWill, final long sessionExpiry) {
        checkNotNull(client, "Client id must not be null");
        final long timestamp = System.currentTimeMillis();
        final SettableFuture<Void> resultFuture = SettableFuture.create();
        singleWriter.submit(client, (SingleWriterService.Task<Void>) (bucketIndex, queueBuckets, queueIndex) -> {
            final ClientSession disconnectSession = localPersistence.disconnect(client, timestamp, sendWill, bucketIndex, sessionExpiry);
            if (sendWill) {
                pendingWillMessages.addWill(client, disconnectSession);
            }

            final ListenableFuture<Void> removeQos0Future = clientQueuePersistence.removeAllQos0Messages(client, false);
            if (disconnectSession.getSessionExpiryInterval() == SESSION_EXPIRE_ON_DISCONNECT) {
                final ListenableFuture<Void> removeSubFuture = subscriptionPersistence.removeAll(client);
                resultFuture.setFuture(FutureUtils.mergeVoidFutures(removeQos0Future, removeSubFuture));
                return null;
            }
            resultFuture.setFuture(removeQos0Future);
            return null;
        });
        return resultFuture;
    }

    @NotNull
    @Override
    public ListenableFuture<Void> clientConnected(@NotNull final String client, final boolean cleanStart, final long clientSessionExpiryInterval, @Nullable final MqttWillPublish willPublish) {
        checkNotNull(client, "Client id must not be null");
        pendingWillMessages.cancelWill(client);
        final long timestamp = System.currentTimeMillis();
        ClientSessionWill sessionWill = null;
        if (willPublish != null) {
            final long willPayloadId = publishPayloadPersistence.add(willPublish.getPayload(), 1);
            sessionWill = new ClientSessionWill(willPublish, willPayloadId);
        }
        final ClientSession clientSession = new ClientSession(true, clientSessionExpiryInterval, sessionWill);
        final ListenableFuture<ConnectResult> submitFuture = singleWriter.submit(client, new SingleWriterService.Task<>() {
            @NotNull
            @Override
            public ConnectResult doTask(final int bucketIndex, @NotNull final ImmutableList<Integer> queueBuckets, final int queueIndex) {
                final Long previousTimestamp = localPersistence.getTimestamp(client, bucketIndex);
                final ClientSession previousClientSession = localPersistence.getSession(client, bucketIndex, false);
                localPersistence.put(client, clientSession, timestamp, bucketIndex);
                return new ConnectResult(previousTimestamp, previousClientSession);
            }
        });

        final SettableFuture<Void> resultFuture = SettableFuture.create();
        FutureUtils.addPersistenceCallback(submitFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final ConnectResult connectResult) {
                final Long previousTimestamp = connectResult.getPreviousTimestamp();
                final ClientSession previousClientSession = connectResult.getPreviousClientSession();

                final ListenableFuture<Void> cleanupFuture;

                // CleanUp the client session if the session is expired OR the client is clean start client.
                if (cleanStart) {
                    cleanupFuture = cleanClientData(client);
                } else {
                    final boolean expired = previousTimestamp != null &&
                            ClientSessions.isExpired(previousClientSession, System.currentTimeMillis() - previousTimestamp);
                    if (expired) {
                        // timestamp in milliseconds + session expiry in seconds * 1000 = milliseconds
                        eventLog.clientSessionExpired(previousTimestamp + previousClientSession.getSessionExpiryInterval() * 1000, client);
                        cleanupFuture = cleanClientData(client);
                    } else {
                        cleanupFuture = Futures.immediateFuture(null);
                    }
                }
                resultFuture.setFuture(cleanupFuture);
            }

            @Override
            public void onFailure(@NotNull final Throwable t) {
                resultFuture.setException(t);
            }
        });
        return resultFuture;
    }

    @NotNull
    @Override
    public ListenableFuture<Boolean> forceDisconnectClient(
            final @NotNull String clientId,
            final boolean preventLwtMessage,
            final @NotNull DisconnectSource source,
            final @Nullable Mqtt5DisconnectReasonCode reasonCode,
            final @Nullable String reasonString) {

        Preconditions.checkNotNull(clientId, "Parameter clientId cannot be null");
        Preconditions.checkNotNull(source, "Disconnect source cannot be null");

        final ClientSession session = getSession(clientId, false);
        if (session == null) {
            log.trace("Ignoring forced client disconnect request for client '{}', because client is not connected.", clientId);
            return Futures.immediateFuture(false);
        }
        if (preventLwtMessage) {
            pendingWillMessages.cancelWill(clientId);
        }

        log.debug("Request forced client disconnect for client {}.", clientId);
        final Channel channel = channelPersistence.get(clientId);

        if (channel == null) {
            log.trace("Ignoring forced client disconnect request for client '{}', because client is not connected.", clientId);
            return Futures.immediateFuture(false);
        }

        channel.attr(ChannelAttributes.PREVENT_LWT).set(preventLwtMessage);
        if (session.getSessionExpiryInterval() != SESSION_EXPIRY_NOT_SET) {
            channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(session.getSessionExpiryInterval());
        }

        final ProtocolVersion version = channel.attr(ChannelAttributes.MQTT_VERSION).get();
        final String logMessage = String.format("Disconnecting client with clientId '%s' forcibly via extension system.", clientId);
        final String eventLogMessage = "Disconnected via extension system";

        final Mqtt5DisconnectReasonCode usedReasonCode =
                reasonCode == null ? Mqtt5DisconnectReasonCode.ADMINISTRATIVE_ACTION : Mqtt5DisconnectReasonCode.valueOf(reasonCode.name());

        if (version == ProtocolVersion.MQTTv5) {
            mqtt5ServerDisconnector.disconnect(channel, logMessage, eventLogMessage, usedReasonCode, reasonString);
        } else {
            mqtt3ServerDisconnector.disconnect(channel, logMessage, eventLogMessage, usedReasonCode, reasonString);
        }

        final SettableFuture<Boolean> resultFuture = SettableFuture.create();
        channel.closeFuture().addListener((ChannelFutureListener) future -> {
            resultFuture.set(true);
        });
        return resultFuture;
    }

    @Override
    public @NotNull ListenableFuture<Boolean> forceDisconnectClient(
            final @NotNull String clientId, final boolean preventLwtMessage, final @NotNull DisconnectSource source) {
        return forceDisconnectClient(clientId, preventLwtMessage, source, null, null);
    }

    @NotNull
    public ListenableFuture<Void> cleanClientData(@NotNull final String clientId) {

        final ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();
        builder.add(subscriptionPersistence.removeAll(clientId));
        builder.add(clientQueuePersistence.clear(clientId, false));

        return FutureUtils.voidFutureFromList(builder.build());
    }

    @NotNull
    @Override
    public ListenableFuture<Set<String>> getAllClients() {
        final List<ListenableFuture<Set<String>>> futures = singleWriter.submitToAllQueues((bucketIndex, queueBuckets, queueIndex) -> {
            final Set<String> clientSessions = new HashSet<>();
            for (final Integer bucket : queueBuckets) {
                clientSessions.addAll(localPersistence.getAllClients(bucket));
            }
            return clientSessions;
        });
        return FutureUtils.combineSetResults(Futures.allAsList(futures));
    }

    @Nullable
    @Override
    public ClientSession getSession(@NotNull final String clientId, final boolean includeWill) {
        checkNotNull(clientId, "Client id must not be null");

        return localPersistence.getSession(clientId, true, includeWill);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public ListenableFuture<Boolean> setSessionExpiryInterval(@NotNull final String clientId, final long sessionExpiryInterval) {
        checkNotNull(clientId, "Client id must not be null");

        final ListenableFuture<Boolean> setTTlFuture = singleWriter.submit(clientId, new SingleWriterService.Task<>() {

            @Override
            public @NotNull Boolean doTask(final int bucketIndex, @NotNull final ImmutableList<Integer> queueBuckets, final int queueIndex) {

                final boolean clientSessionExists = localPersistence.getSession(clientId) != null;

                if (!clientSessionExists) {
                    return false;
                }

                localPersistence.setSessionExpiryInterval(clientId, sessionExpiryInterval, bucketIndex);
                return true;
            }
        });

        final SettableFuture<Boolean> settableFuture = SettableFuture.create();

        FutureUtils.addPersistenceCallback(setTTlFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final Boolean sessionExists) {
                if (sessionExpiryInterval == SESSION_EXPIRE_ON_DISCONNECT) {

                    final ListenableFuture<Void> removeAllFuture = subscriptionPersistence.removeAll(clientId);

                    FutureUtils.addPersistenceCallback(removeAllFuture, new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable final Void result) {
                            settableFuture.set(sessionExists);
                        }

                        @Override
                        public void onFailure(@NotNull final Throwable t) {
                            settableFuture.setException(t);
                        }
                    });
                } else {
                    settableFuture.set(sessionExists);
                }
            }

            @Override
            public void onFailure(@NotNull final Throwable t) {
                settableFuture.setException(t);
            }
        });

        return settableFuture;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Long getSessionExpiryInterval(@NotNull final String clientId) {

        final ClientSession session = getSession(clientId, false);
        if (session == null) {
            return null;
        }
        return session.getSessionExpiryInterval();
    }

    @NotNull
    @Override
    public ListenableFuture<Void> cleanUp(final int bucketIndex) {
        return singleWriter.submit(bucketIndex, new ClientSessionCleanUpTask(localPersistence, this));
    }

    @NotNull
    @Override
    public ListenableFuture<Void> closeDB() {
        return closeDB(localPersistence, singleWriter);
    }

    @NotNull
    @Override
    public ListenableFuture<Boolean> invalidateSession(final @NotNull String clientId, final @NotNull DisconnectSource disconnectSource) {

        Preconditions.checkNotNull(clientId, "ClientId cannot be null");
        Preconditions.checkNotNull(disconnectSource, "Disconnect source cannot be null");

        final ListenableFuture<Boolean> setTTLFuture = setSessionExpiryInterval(clientId, 0);
        final SettableFuture<Boolean> resultFuture = SettableFuture.create();

        FutureUtils.addPersistenceCallback(setTTLFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final Boolean sessionExists) {

                if (sessionExists) {
                    final ListenableFuture<Boolean> disconnectClientFuture = forceDisconnectClient(clientId, false, disconnectSource);
                    resultFuture.setFuture(disconnectClientFuture);
                } else {
                    resultFuture.set(null);
                }
            }

            @Override
            public void onFailure(@NotNull final Throwable throwable) {
                resultFuture.setException(throwable);
            }
        });

        return resultFuture;
    }

    @NotNull
    @Override
    public ListenableFuture<Map<String, PendingWillMessages.PendingWill>> pendingWills() {

        final ListenableFuture<List<Map<String, PendingWillMessages.PendingWill>>> singleWriterFutures = singleWriter.submitToAllQueuesAsList(new SingleWriterService.Task<Map<String, PendingWillMessages.PendingWill>>() {
            @NotNull
            @Override
            public Map<String, PendingWillMessages.PendingWill> doTask(final int bucketIndex, @NotNull final ImmutableList<Integer> queueBuckets, final int queueIndex) {
                final ImmutableMap.Builder<String, PendingWillMessages.PendingWill> queueWills = ImmutableMap.builder();
                for (final Integer queueBucket : queueBuckets) {
                    final Map<String, PendingWillMessages.PendingWill> bucketMap = localPersistence.getPendingWills(queueBucket);
                    queueWills.putAll(bucketMap);
                }
                return queueWills.build();
            }
        });

        final SettableFuture<Map<String, PendingWillMessages.PendingWill>> settableFuture = SettableFuture.create();
        FutureUtils.addPersistenceCallback(singleWriterFutures, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final List<Map<String, PendingWillMessages.PendingWill>> result) {

                if (result == null) {
                    settableFuture.set(ImmutableMap.of());
                    return;
                }

                final ImmutableMap.Builder<String, PendingWillMessages.PendingWill> resultMap = ImmutableMap.builder();
                for (final Map<String, PendingWillMessages.PendingWill> map : result) {
                    resultMap.putAll(map);
                }
                settableFuture.set(resultMap.build());
            }

            @Override
            public void onFailure(final Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeWill(@NotNull final String clientId) {
        checkNotNull(clientId, "Client id must not be null");
        return singleWriter.submit(clientId, new SingleWriterService.Task<Void>() {
            @NotNull
            @Override
            public Void doTask(final int bucketIndex, @NotNull final ImmutableList<Integer> queueBuckets, final int queueIndex) {
                localPersistence.removeWill(clientId, bucketIndex);
                return null;
            }
        });
    }

    @Override
    public @NotNull ListenableFuture<MultipleChunkResult<Map<String, ClientSession>>> getAllLocalClientsChunk(@NotNull final ChunkCursor cursor) {
        try {
            checkNotNull(cursor, "Cursor must not be null");

            final ImmutableList.Builder<ListenableFuture<@NotNull BucketChunkResult<Map<String, ClientSession>>>> builder = ImmutableList.builder();

            final int bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
            final int maxResults = InternalConfigurations.PERSISTENCE_CLIENT_SESSIONS_MAX_CHUNK_SIZE / (bucketCount - cursor.getFinishedBuckets().size());
            for (int i = 0; i < bucketCount; i++) {
                //skip already finished buckets
                if (!cursor.getFinishedBuckets().contains(i)) {
                    final String lastKey = cursor.getLastKeys().get(i);
                    builder.add(singleWriter.submit(i, (bucketIndex1, queueBuckets, queueIndex) -> {
                        return localPersistence.getAllClientsChunk(MatchAllPersistenceFilter.INSTANCE, bucketIndex1, lastKey, maxResults);
                    }));
                }
            }

            return Futures.transform(Futures.allAsList(builder.build()), allBucketsResult -> {
                Preconditions.checkNotNull(allBucketsResult, "Iteration result from all buckets cannot be null");

                final ImmutableMap.Builder<Integer, BucketChunkResult<Map<String, ClientSession>>> resultBuilder = ImmutableMap.builder();
                for (final BucketChunkResult<Map<String, ClientSession>> bucketResult : allBucketsResult) {
                    resultBuilder.put(bucketResult.getBucketIndex(), bucketResult);
                }

                for (final Integer finishedBucketId : cursor.getFinishedBuckets()) {
                    resultBuilder.put(finishedBucketId, new BucketChunkResult<>(Map.of(), true, cursor.getLastKeys().get(finishedBucketId), finishedBucketId));
                }

                return new MultipleChunkResult<>(resultBuilder.build());

            }, MoreExecutors.directExecutor());

        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }
    }

    public enum DisconnectSource {

        /**
         * Extension system disconnected the client
         */
        EXTENSION(0);

        final int number;

        DisconnectSource(final int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

        @NotNull
        public static DisconnectSource ofNumber(final int number) {

            for (final DisconnectSource value : values()) {
                if (value.number == number) {
                    return value;
                }
            }

            throw new IllegalArgumentException("No disconnect source found for number: " + number);
        }
    }
}
