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
package com.hivemq.persistence.connection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.*;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class ConnectionPersistenceImpl implements ConnectionPersistence {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPersistenceImpl.class);

    private final @NotNull Map<String, ClientConnection> clientConnectionMap;
    private final @NotNull Map<String, Channel> serverChannelMap;
    private final @NotNull AtomicBoolean interrupted;
    private final boolean shutdownLegacy;
    private final int shutdownPartitionSize;

    @Inject
    public ConnectionPersistenceImpl() {
        shutdownLegacy = InternalConfigurations.NETTY_SHUTDOWN_LEGACY;
        shutdownPartitionSize = InternalConfigurations.NETTY_COUNT_OF_CONNECTIONS_IN_SHUTDOWN_PARTITION;
        interrupted = new AtomicBoolean(false);
        clientConnectionMap = new ConcurrentHashMap<>();
        serverChannelMap = new ConcurrentHashMap<>();
    }

    @Override
    public @Nullable ClientConnection get(final @NotNull String clientId) {
        return clientConnectionMap.get(clientId);
    }

    @Override
    public @NotNull ClientConnection persistIfAbsent(final @NotNull ClientConnection clientConnection) {
        return clientConnectionMap.computeIfAbsent(clientConnection.getClientId(), id -> clientConnection);
    }

    @Override
    public void remove(final @NotNull ClientConnection clientConnection) {
        clientConnectionMap.remove(clientConnection.getClientId(), clientConnection);
    }

    @Override
    public void addServerChannel(final @NotNull String listenerName, final @NotNull Channel channel) {
        serverChannelMap.put(listenerName, channel);
    }

    @Override
    public void interruptShutdown() {
        interrupted.set(true);
    }

    @Override
    public @NotNull ListenableFuture<Void> shutDown() {
        if (shutdownLegacy) {
            return Futures.immediateFuture(null);
        }

        final ListenableFuture<Void> allServersClosedFuture = shutDownListeners();
        final SettableFuture<Void> allClientsClosedFuture = SettableFuture.create();
        Futures.addCallback(allServersClosedFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final @Nullable Void result) {
                shutDownClients(allClientsClosedFuture);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                Exceptions.rethrowError(t);
                log.warn("Shutdown of listeners failed");
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception: ", t);
                }
                shutDownClients(allClientsClosedFuture);
            }

        }, MoreExecutors.directExecutor());
        return allClientsClosedFuture;
    }

    private @NotNull ListenableFuture<Void> shutDownListeners() {
        try {
            final Map<String, Channel> allServerChannels = ImmutableMap.copyOf(serverChannelMap);
            final ImmutableList.Builder<ListenableFuture<Void>> futureBuilder = ImmutableList.builder();
            for (final Map.Entry<String, Channel> channelEntry : allServerChannels.entrySet()) {
                final SettableFuture<Void> closeFuture = SettableFuture.create();
                futureBuilder.add(closeFuture);
                channelEntry.getValue().close().addListener((ChannelFutureListener) future -> {
                    log.debug("Closed channel of listener with name '{}'", channelEntry.getKey());
                    closeFuture.set(null);
                });
            }

            final ListenableFuture<List<Void>> future = Futures.allAsList(futureBuilder.build());
            final SettableFuture<Void> resultFuture = SettableFuture.create();
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(final @Nullable List<Void> result) {
                    resultFuture.set(null);
                }

                @Override
                public void onFailure(final @NotNull Throwable t) {
                    resultFuture.setException(t);
                }
            }, MoreExecutors.directExecutor());
            return resultFuture;
        } catch (final Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private void shutDownClients(final @NotNull SettableFuture<Void> allClientsClosedFuture) {
        final List<ClientConnection> allConnections = new ArrayList<>(clientConnectionMap.values());
        if (allConnections.isEmpty()) {
            allClientsClosedFuture.set(null);
            return;
        }
        final List<List<ClientConnection>> connectionPartitions = Lists.partition(allConnections, shutdownPartitionSize);
        shutDownPartition(connectionPartitions, 0, allClientsClosedFuture);
    }

    private void shutDownPartition(
            final @NotNull List<List<ClientConnection>> connectionPartitions,
            final int index,
            final @NotNull SettableFuture<Void> closeFuture) {

        if (interrupted.get() || index >= connectionPartitions.size()) {
            closeFuture.set(null);
            return;
        }
        final List<ClientConnection> partition = connectionPartitions.get(index);
        final List<ListenableFuture<Void>> closeFutures = new ArrayList<>(partition.size());
        for (final ClientConnection clientConnection : partition) {
            final SettableFuture<Void> disconnectFuture = clientConnection.getDisconnectFuture();
            final ChannelFuture channelFuture = clientConnection.getChannel().close();
            if (disconnectFuture != null) {
                closeFutures.add(disconnectFuture);
            } else {
                final SettableFuture<Void> channelCloseFuture = SettableFuture.create();
                closeFutures.add(channelCloseFuture);
                channelFuture.addListener((ChannelFutureListener) future -> channelCloseFuture.set(null));
            }
        }

        Futures.whenAllComplete(closeFutures).run(() -> {
            shutDownPartition(connectionPartitions, index + 1, closeFuture);
        }, MoreExecutors.directExecutor());
    }

    @VisibleForTesting
    public @NotNull Set<Map.Entry<String, ClientConnection>> entries() {
        return clientConnectionMap.entrySet();
    }
}
