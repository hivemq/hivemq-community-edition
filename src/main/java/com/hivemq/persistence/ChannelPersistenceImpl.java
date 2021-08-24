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
package com.hivemq.persistence;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.*;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.ChannelAttributes;
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

/**
 * @author Dominik Obermaier
 */
@Singleton
public class ChannelPersistenceImpl implements ChannelPersistence {

    private static final Logger log = LoggerFactory.getLogger(ChannelPersistence.class);

    private final @NotNull Map<String, Channel> channelMap;
    private final @NotNull Map<String, Channel> serverChannelMap;
    private final boolean shutdownLegacy;
    private final @NotNull AtomicBoolean interrupted;
    private final int shutdownPartitionSize;

    @Inject
    public ChannelPersistenceImpl() {
        this.shutdownLegacy = InternalConfigurations.NETTY_SHUTDOWN_LEGACY;
        this.shutdownPartitionSize = InternalConfigurations.NETTY_SHUTDOWN_PARTITION_SIZE;
        this.interrupted = new AtomicBoolean(false);
        this.channelMap = new ConcurrentHashMap<>();
        this.serverChannelMap = new ConcurrentHashMap<>();
    }


        @Nullable
    @Override
    public Channel get(final @NotNull String clientId) {
        return channelMap.get(clientId);
    }

    @Override
    public void persist(final @NotNull String clientId, final @NotNull Channel value) {
        channelMap.put(clientId, value);
    }

    @Nullable
    @Override
    public Channel remove(final @NotNull String clientId) {
        return channelMap.remove(clientId);
    }

    @Override
    public long size() {
        return channelMap.size();
    }

    @NotNull
    @Override
    public Set<Map.Entry<String, Channel>> entries() {
        return channelMap.entrySet();
    }


    @Override
    public void addServerChannel(final @NotNull String listenerName, final @NotNull Channel channel) {
        serverChannelMap.put(listenerName, channel);
    }

    @Override
    public @NotNull Set<Map.Entry<String, Channel>> getServerChannels() {
        return serverChannelMap.entrySet();
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
                if(log.isDebugEnabled()){
                    log.debug("Original Exception: ", t);
                }
                shutDownClients(allClientsClosedFuture);
            }

        }, MoreExecutors.directExecutor());
        return allClientsClosedFuture;
    }

    @NotNull
    private ListenableFuture<Void> shutDownListeners() {
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
//            return FutureUtils.voidFutureFromList(futureBuilder.build());
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
        final List<Channel> allChannels = new ArrayList<>(channelMap.values());
        if (allChannels.isEmpty()) {
            allClientsClosedFuture.set(null);
            return;
        }
        final List<List<Channel>> channelPartitions = Lists.partition(allChannels, shutdownPartitionSize);
        shutDownPartition(channelPartitions, 0, allClientsClosedFuture);
    }

    private void shutDownPartition(final @NotNull List<List<Channel>> channelPartitions, final int index, final @NotNull SettableFuture<Void> closeFuture) {
        if (interrupted.get() || index >= channelPartitions.size()) {
            closeFuture.set(null);
            return;
        }
        final List<Channel> partition = channelPartitions.get(index);
        final List<ListenableFuture<Void>> closeFutures = new ArrayList<>(partition.size());
        for (final Channel channel : partition) {
            final SettableFuture<Void> disconnectFuture = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getDisconnectFuture();
            final ChannelFuture channelFuture = channel.close();
            if(disconnectFuture != null){
                closeFutures.add(disconnectFuture);
            } else {
                final SettableFuture<Void> channelCloseFuture = SettableFuture.create();
                closeFutures.add(channelCloseFuture);
                channelFuture.addListener((ChannelFutureListener) future -> channelCloseFuture.set(null));
            }
        }

        Futures.whenAllComplete(closeFutures).run(() -> {
            shutDownPartition(channelPartitions, index + 1, closeFuture);
        }, MoreExecutors.directExecutor());
    }
}
