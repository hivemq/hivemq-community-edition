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

package com.hivemq.extensions.services.session;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.*;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.services.exception.NoSuchClientIdException;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.session.SessionInformation;
import com.hivemq.extensions.ListenableFutureConverter;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.util.Exceptions;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.hivemq.persistence.clientsession.ClientSessionPersistenceImpl.DisconnectSource.EXTENSION;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@LazySingleton
public class ClientServiceImpl implements ClientService {


    @NotNull
    private final PluginServiceRateLimitService pluginServiceRateLimitService;

    @NotNull
    private final ClientSessionPersistence clientSessionPersistence;

    @Inject
    public ClientServiceImpl(@NotNull final PluginServiceRateLimitService pluginServiceRateLimitService,
                             @NotNull final ClientSessionPersistence clientSessionPersistence) {
        this.pluginServiceRateLimitService = pluginServiceRateLimitService;
        this.clientSessionPersistence = clientSessionPersistence;
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> isClientConnected(@NotNull final String clientId) {
        Preconditions.checkNotNull(clientId, "A client id must never be null");
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        final ClientSession session = clientSessionPersistence.getSession(clientId);
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

        final ClientSession session = clientSessionPersistence.getSession(clientId);
        if (session == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.completedFuture(Optional.of(new SessionInformationImpl(clientId, session.getSessionExpiryInterval(), session.isConnected())));
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> disconnectClient(@NotNull final String clientId) {
        return disconnectClient(clientId, false);
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> disconnectClient(@NotNull final String clientId, final boolean preventWillMessage) {
        Preconditions.checkNotNull(clientId, "A client id must never be null");
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        return ListenableFutureConverter.toCompletable(clientSessionPersistence.forceDisconnectClient(clientId, preventWillMessage, EXTENSION));
    }

    @NotNull
    @Override
    public CompletableFuture<Boolean> invalidateSession(@NotNull final String clientId) {
        Preconditions.checkNotNull(clientId, "A client id must never be null");
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        final SettableFuture<Boolean> setSessionSettableFuture = SettableFuture.create();
        final ListenableFuture<Boolean> setSessionFuture = clientSessionPersistence.invalidateSession(clientId, EXTENSION);
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
        }, MoreExecutors.directExecutor());

        return ListenableFutureConverter.toCompletable(setSessionSettableFuture);
    }
}
