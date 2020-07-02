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
package com.hivemq.extensions.services.session;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.services.exception.NoSuchClientIdException;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.session.SessionInformation;
import com.hivemq.extensions.ListenableFutureConverter;
import com.hivemq.extensions.iteration.*;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.util.Exceptions;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.hivemq.persistence.clientsession.ClientSessionPersistenceImpl.DisconnectSource.EXTENSION;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@LazySingleton
public class ClientServiceImpl implements ClientService {

    private final @NotNull PluginServiceRateLimitService pluginServiceRateLimitService;
    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull GlobalManagedExtensionExecutorService managedExtensionExecutorService;
    private final @NotNull AsyncIteratorFactory asyncIteratorFactory;

    @Inject
    public ClientServiceImpl(
            @NotNull final PluginServiceRateLimitService pluginServiceRateLimitService,
            @NotNull final ClientSessionPersistence clientSessionPersistence,
            @NotNull final GlobalManagedExtensionExecutorService managedExtensionExecutorService,
            @NotNull final AsyncIteratorFactory asyncIteratorFactory) {
        this.pluginServiceRateLimitService = pluginServiceRateLimitService;
        this.clientSessionPersistence = clientSessionPersistence;
        this.managedExtensionExecutorService = managedExtensionExecutorService;
        this.asyncIteratorFactory = asyncIteratorFactory;
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> isClientConnected(@NotNull final String clientId) {
        Preconditions.checkNotNull(clientId, "A client id must never be null");
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        final ClientSession session = clientSessionPersistence.getSession(clientId, false);
        if (session == null) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(session.isConnected());
    }

    @NotNull
    @Override
    public CompletableFuture<Optional<SessionInformation>> getSession(@NotNull final String clientId) {
        Preconditions.checkNotNull(clientId, "A client id must never be null");
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }

        final ClientSession session = clientSessionPersistence.getSession(clientId, false);
        if (session == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.completedFuture(Optional.of(
                new SessionInformationImpl(clientId, session.getSessionExpiryInterval(), session.isConnected())));
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> disconnectClient(@NotNull final String clientId) {
        return disconnectClient(clientId, false);
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> disconnectClient(
            @NotNull final String clientId, final boolean preventWillMessage) {
        return disconnectClient(clientId, preventWillMessage, null, null);
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> disconnectClient(
            final @NotNull String clientId,
            final boolean preventWillMessage,
            final @Nullable DisconnectReasonCode reasonCode,
            final @Nullable String reasonString) {

        Preconditions.checkNotNull(clientId, "A client id must never be null");
        if (reasonCode != null) {
            Preconditions.checkArgument(
                    reasonCode != DisconnectReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                    "Reason code %s must not be used for disconnect packets.", reasonCode);
            Preconditions.checkArgument(
                    Mqtt5DisconnectReasonCode.from(reasonCode).canBeSentByServer(),
                    "Reason code %s must not be used for outbound disconnect packets from the server to a client.",
                    reasonCode);
        }

        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }

        final Mqtt5DisconnectReasonCode disconnectReasonCode =
                reasonCode != null ? Mqtt5DisconnectReasonCode.valueOf(reasonCode.name()) : null;

        final ListenableFuture<Boolean> disconnectFuture =
                clientSessionPersistence.forceDisconnectClient(
                        clientId, preventWillMessage, EXTENSION, disconnectReasonCode, reasonString);

        return ListenableFutureConverter.toCompletable(disconnectFuture, managedExtensionExecutorService);
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> invalidateSession(@NotNull final String clientId) {
        Preconditions.checkNotNull(clientId, "A client id must never be null");
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        final SettableFuture<Boolean> setSessionSettableFuture = SettableFuture.create();
        final ListenableFuture<Boolean> setSessionFuture =
                clientSessionPersistence.invalidateSession(clientId, EXTENSION);
        Futures.addCallback(setSessionFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final Boolean disconnected) {
                if (disconnected == null) {
                    setSessionSettableFuture.setException(new NoSuchClientIdException(clientId));
                } else {
                    setSessionSettableFuture.set(disconnected);
                }
            }

            @Override
            public void onFailure(@NotNull final Throwable t) {
                Exceptions.rethrowError(t);
                setSessionSettableFuture.setException(t);
            }
        }, managedExtensionExecutorService);

        return ListenableFutureConverter.toCompletable(setSessionSettableFuture, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllClients(
            @NotNull final IterationCallback<SessionInformation> callback) {
        return iterateAllClients(callback, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllClients(
            @NotNull final IterationCallback<SessionInformation> callback, @NotNull final Executor callbackExecutor) {
        Preconditions.checkNotNull(callback, "Callback cannot be null");
        Preconditions.checkNotNull(callbackExecutor, "Callback executor cannot be null");

        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }

        final FetchCallback<SessionInformation> fetchCallback = new AllClientsFetchCallback(clientSessionPersistence);
        final AsyncIterator<SessionInformation> asyncIterator =
                asyncIteratorFactory.createIterator(
                        fetchCallback,
                        new AllItemsItemCallback<>(callbackExecutor, callback));

        asyncIterator.fetchAndIterate();

        final SettableFuture<Void> settableFuture = SettableFuture.create();
        asyncIterator.getFinishedFuture().whenComplete((aVoid, throwable) -> {
            if (throwable != null) {
                settableFuture.setException(throwable);
            } else {
                settableFuture.set(null);
            }
        });

        return ListenableFutureConverter.toCompletable(settableFuture, managedExtensionExecutorService);
    }

    static class AllClientsFetchCallback extends AllItemsFetchCallback<SessionInformation, Map<String, ClientSession>> {

        @NotNull
        private final ClientSessionPersistence clientSessionPersistence;

        AllClientsFetchCallback(@NotNull final ClientSessionPersistence clientSessionPersistence) {
            this.clientSessionPersistence = clientSessionPersistence;
        }

        @Override
        protected @NotNull ListenableFuture<MultipleChunkResult<Map<String, ClientSession>>> persistenceCall(final @NotNull ChunkCursor chunkCursor) {
            return clientSessionPersistence.getAllLocalClientsChunk(chunkCursor);
        }

        @Override
        protected @NotNull Collection<SessionInformation> transform(final @NotNull Map<String, ClientSession> stringClientSessionMap) {
            return stringClientSessionMap.entrySet().stream().map(entry -> new SessionInformationImpl(entry.getKey(), entry.getValue().getSessionExpiryInterval(), entry.getValue().isConnected())).collect(Collectors.toUnmodifiableList());
        }
    }
}
