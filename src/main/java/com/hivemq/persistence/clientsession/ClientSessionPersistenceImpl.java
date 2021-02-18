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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.iteration.ChunkCursor;
import com.hivemq.extensions.iteration.Chunker;
import com.hivemq.extensions.iteration.MultipleChunkResult;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.persistence.*;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.task.ClientSessionCleanUpTask;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistenceImpl;
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
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull Chunker chunker;

    @Inject
    public ClientSessionPersistenceImpl(final @NotNull ClientSessionLocalPersistence localPersistence,
                                        final @NotNull ClientSessionSubscriptionPersistence sessionSubscriptionPersistence,
                                        final @NotNull ClientQueuePersistence clientQueuePersistence,
                                        final @NotNull SingleWriterService singleWriterService,
                                        final @NotNull ChannelPersistence channelPersistence,
                                        final @NotNull EventLog eventLog,
                                        final @NotNull PublishPayloadPersistence publishPayloadPersistence,
                                        final @NotNull PendingWillMessages pendingWillMessages,
                                        final @NotNull MqttServerDisconnector mqttServerDisconnector,
                                        final @NotNull Chunker chunker) {


        this.localPersistence = localPersistence;
        this.subscriptionPersistence = sessionSubscriptionPersistence;
        this.clientQueuePersistence = clientQueuePersistence;
        singleWriter = singleWriterService.getClientSessionQueue();

        this.channelPersistence = channelPersistence;
        this.eventLog = eventLog;
        this.publishPayloadPersistence = publishPayloadPersistence;
        this.pendingWillMessages = pendingWillMessages;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.chunker = chunker;
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
        singleWriter.submit(client, (SingleWriterServiceImpl.Task<Void>) (bucketIndex, queueBuckets, queueIndex) -> {
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
    public ListenableFuture<Void> clientConnected(@NotNull final String client,
                                                  final boolean cleanStart,
                                                  final long clientSessionExpiryInterval,
                                                  @Nullable final MqttWillPublish willPublish,
                                                  @Nullable final Long queueLimit) {
        checkNotNull(client, "Client id must not be null");
        pendingWillMessages.cancelWill(client);
        final long timestamp = System.currentTimeMillis();
        ClientSessionWill sessionWill = null;
        if (willPublish != null) {
            final long publishId = PublishPayloadPersistenceImpl.createId();
            final boolean removePayload = publishPayloadPersistence.add(willPublish.getPayload(), 1, publishId);
            if (removePayload) {
                willPublish.setPayload(null);
            }
            sessionWill = new ClientSessionWill(willPublish, publishId);
        }
        final ClientSession clientSession = new ClientSession(true, clientSessionExpiryInterval, sessionWill, queueLimit);
        final ListenableFuture<ConnectResult> submitFuture =
                singleWriter.submit(client, (bucketIndex, queueBuckets, queueIndex) -> {
                    final Long previousTimestamp = localPersistence.getTimestamp(client, bucketIndex);
                    final ClientSession previousClientSession = localPersistence.getSession(client, bucketIndex, false);
                    localPersistence.put(client, clientSession, timestamp, bucketIndex);
                    return new ConnectResult(previousTimestamp, previousClientSession);
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

        final String logMessage = String.format("Disconnecting client with clientId '%s' forcibly via extension system.", clientId);
        final String eventLogMessage = "Disconnected via extension system";

        final Mqtt5DisconnectReasonCode usedReasonCode =
                reasonCode == null ? Mqtt5DisconnectReasonCode.ADMINISTRATIVE_ACTION : Mqtt5DisconnectReasonCode.valueOf(reasonCode.name());

        mqttServerDisconnector.disconnect(channel, logMessage, eventLogMessage, usedReasonCode, reasonString);

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

        final ListenableFuture<Boolean> setTTlFuture =
                singleWriter.submit(clientId, (bucketIndex, queueBuckets, queueIndex) -> {

                    final boolean clientSessionExists = localPersistence.getSession(clientId) != null;

                    if (!clientSessionExists) {
                        return false;
                    }

                    localPersistence.setSessionExpiryInterval(clientId, sessionExpiryInterval, bucketIndex);
                    return true;
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

        final ListenableFuture<List<Map<String, PendingWillMessages.PendingWill>>> singleWriterFutures =
                singleWriter.submitToAllQueuesAsList((bucketIndex, queueBuckets, queueIndex) -> {
                    final ImmutableMap.Builder<String, PendingWillMessages.PendingWill> queueWills =
                            ImmutableMap.builder();
                    for (final Integer queueBucket : queueBuckets) {
                        final Map<String, PendingWillMessages.PendingWill> bucketMap =
                                localPersistence.getPendingWills(queueBucket);
                        queueWills.putAll(bucketMap);
                    }
                    return queueWills.build();
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
        return singleWriter.submit(clientId, (bucketIndex, queueBuckets, queueIndex) -> {
            localPersistence.removeWill(clientId, bucketIndex);
            return null;
        });
    }

    @Override
    public @NotNull ListenableFuture<MultipleChunkResult<Map<String, ClientSession>>> getAllLocalClientsChunk(@NotNull final ChunkCursor cursor) {
        return chunker.getAllLocalChunk(cursor, InternalConfigurations.PERSISTENCE_CLIENT_SESSIONS_MAX_CHUNK_SIZE,
                // Chunker.SingleWriterCall interface
                (bucket, lastKey, maxResults) -> singleWriter.submit(bucket,
                        // actual single writer call
                        (bucketIndex, ignored1, ignored2) ->
                                localPersistence.getAllClientsChunk(
                                        bucketIndex,
                                        lastKey,
                                        maxResults)));
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
