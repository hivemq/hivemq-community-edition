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
package com.hivemq.bootstrap;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.netty.ChannelInitializerFactory;
import com.hivemq.bootstrap.netty.NettyConfiguration;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.util.Validators;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HiveMQNettyBootstrap {

    private static final Logger log = LoggerFactory.getLogger(HiveMQNettyBootstrap.class);

    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull ListenerConfigurationService listenerConfigurationService;
    private final @NotNull ChannelInitializerFactory channelInitializerFactory;
    private final @NotNull ConnectionPersistence connectionPersistence;
    private final @NotNull NettyConfiguration nettyConfiguration;

    @Inject
    HiveMQNettyBootstrap(
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull ListenerConfigurationService listenerConfigurationService,
            final @NotNull ChannelInitializerFactory channelInitializerFactory,
            final @NotNull ConnectionPersistence connectionPersistence,
            final @NotNull NettyConfiguration nettyConfiguration) {

        this.shutdownHooks = shutdownHooks;
        this.listenerConfigurationService = listenerConfigurationService;
        this.channelInitializerFactory = channelInitializerFactory;
        this.connectionPersistence = connectionPersistence;
        this.nettyConfiguration = nettyConfiguration;
    }

    public @NotNull ListenableFuture<List<ListenerStartupInformation>> bootstrapServer() {

        //Adding shutdown hook for graceful shutdown
        final int shutdownTimeout = InternalConfigurations.EVENT_LOOP_GROUP_SHUTDOWN_TIMEOUT_SEC;
        final int channelsShutdownTimeout = InternalConfigurations.CONNECTION_PERSISTENCE_SHUTDOWN_TIMEOUT_SEC;
        shutdownHooks.add(new NettyShutdownHook(
                nettyConfiguration.getChildEventLoopGroup(),
                nettyConfiguration.getParentEventLoopGroup(),
                shutdownTimeout,
                channelsShutdownTimeout,
                connectionPersistence));

        final List<BindInformation> futures = new ArrayList<>();

        addDefaultListeners();

        futures.addAll(bindTcpListeners(listenerConfigurationService.getTcpListeners()));
        futures.addAll(tlsTcpListeners(listenerConfigurationService.getTlsTcpListeners()));
        futures.addAll(websocketListeners(listenerConfigurationService.getWebsocketListeners()));
        futures.addAll(tlsWebsocketListeners(listenerConfigurationService.getTlsWebsocketListeners()));

        return aggregatedFuture(futures);
    }

    private void addDefaultListeners() {
        if (listenerConfigurationService.getListeners().isEmpty()) {
            listenerConfigurationService.addListener(new TcpListener(1883, "0.0.0.0"));
        }
    }

    private @NotNull List<BindInformation> bindTcpListeners(final @NotNull List<TcpListener> tcpListeners) {
        log.trace("Checking TCP listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();

        for (final TcpListener listener : tcpListeners) {

            final ServerBootstrap b = createServerBootstrap(nettyConfiguration.getParentEventLoopGroup(), nettyConfiguration.getChildEventLoopGroup(), listener);
            log.info("Starting TCP listener on address {} and port {}", listener.getBindAddress(), listener.getPort());
            final ChannelFuture bind = b.bind(listener.getBindAddress(), listener.getPort());
            connectionPersistence.addServerChannel(listener.getName(), bind.channel());
            futures.add(new BindInformation(listener, bind));
        }
        return futures.build();
    }

    private @NotNull List<BindInformation> tlsTcpListeners(final @NotNull List<TlsTcpListener> tlsTcpListeners) {
        log.trace("Checking TLS TCP listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();

        for (final TlsTcpListener listener : tlsTcpListeners) {

            final ServerBootstrap b = createServerBootstrap(nettyConfiguration.getParentEventLoopGroup(), nettyConfiguration.getChildEventLoopGroup(), listener);
            log.info("Starting TLS TCP listener on address {} and port {}", listener.getBindAddress(), listener.getPort());
            final ChannelFuture bind = b.bind(listener.getBindAddress(), listener.getPort());
            connectionPersistence.addServerChannel(listener.getName(), bind.channel());
            futures.add(new BindInformation(listener, bind));
        }
        return futures.build();
    }

    private @NotNull List<BindInformation> websocketListeners(final @NotNull List<WebsocketListener> websocketListeners) {
        log.trace("Checking Websocket listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();

        for (final WebsocketListener listener : websocketListeners) {

            final ServerBootstrap b = createServerBootstrap(nettyConfiguration.getParentEventLoopGroup(), nettyConfiguration.getChildEventLoopGroup(), listener);
            log.info("Starting Websocket listener on address {} and port {}", listener.getBindAddress(), listener.getPort());
            final ChannelFuture bind = b.bind(listener.getBindAddress(), listener.getPort());
            connectionPersistence.addServerChannel(listener.getName(), bind.channel());
            futures.add(new BindInformation(listener, bind));
        }
        return futures.build();
    }

    private @NotNull List<BindInformation> tlsWebsocketListeners(
            final @NotNull List<TlsWebsocketListener> tlsWebsocketListeners) {

        log.trace("Checking Websocket TLS listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();
        for (final TlsWebsocketListener listener : tlsWebsocketListeners) {

            final ServerBootstrap b = createServerBootstrap(nettyConfiguration.getParentEventLoopGroup(), nettyConfiguration.getChildEventLoopGroup(), listener);
            log.info("Starting Websocket TLS listener on address {} and port {}", listener.getBindAddress(), listener.getPort());
            final ChannelFuture bind = b.bind(listener.getBindAddress(), listener.getPort());
            connectionPersistence.addServerChannel(listener.getName(), bind.channel());
            futures.add(new BindInformation(listener, bind));
        }
        return futures.build();
    }

    /**
     * Creates an aggregated future which allows to wait for all futures at once
     *
     * @param bindInformation a list of futures to aggregate
     * @return a {@link com.google.common.util.concurrent.ListenableFuture} which aggregates
     * all given {@link io.netty.channel.ChannelFuture}s
     */
    private @NotNull ListenableFuture<List<ListenerStartupInformation>> aggregatedFuture(
            final @NotNull List<BindInformation> bindInformation) {

        final List<ListenableFuture<ListenerStartupInformation>> listenableFutures = bindInformation.stream()
                .map(input -> {
                    final SettableFuture<ListenerStartupInformation> objectSettableFuture = SettableFuture.create();
                    input.getBindFuture().addListener(new UpdateGivenFutureListener(input, objectSettableFuture));
                    return objectSettableFuture;
                }).collect(Collectors.toList());
        return Futures.allAsList(listenableFutures);
    }

    private @NotNull ServerBootstrap createServerBootstrap(
            final @NotNull EventLoopGroup bossGroup,
            final @NotNull EventLoopGroup workerGroup,
            final @NotNull Listener listener) {

        final ServerBootstrap b = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(nettyConfiguration.getServerSocketChannelClass())
                .childHandler(channelInitializerFactory.getChannelInitializer(listener))
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        setAdvancedOptions(b);
        return b;
    }

    /**
     * Sets all advanced properties
     *
     * @param b the server bootstrap
     */
    private void setAdvancedOptions(final @NotNull ServerBootstrap b) {

        final int sendBufferSize = InternalConfigurations.LISTENER_SOCKET_SEND_BUFFER_SIZE_BYTES;
        final int receiveBufferSize = InternalConfigurations.LISTENER_SOCKET_RECEIVE_BUFFER_SIZE_BYTES;

        if (sendBufferSize > -1) {
            b.childOption(ChannelOption.SO_SNDBUF, sendBufferSize);
        }
        if (receiveBufferSize > -1) {
            b.childOption(ChannelOption.SO_RCVBUF, receiveBufferSize);
        }

        final int writeBufferHigh = InternalConfigurations.LISTENER_CLIENT_WRITE_BUFFER_HIGH_THRESHOLD_BYTES;
        final int writeBufferLow = InternalConfigurations.LISTENER_CLIENT_WRITE_BUFFER_LOW_THRESHOLD_BYTES;

        final ClientWriteBufferProperties properties =
                Validators.validateWriteBufferProperties(new ClientWriteBufferProperties(writeBufferHigh,
                        writeBufferLow));

        //it is assumed that the ClientWriteBufferProperties that the listener returns was validated by Validators.validateWriteBufferProperties()
        b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                new WriteBufferWaterMark(properties.getLowThreshold(), properties.getHighThreshold()));
    }

    @Immutable
    private static class UpdateGivenFutureListener implements ChannelFutureListener {

        private final @NotNull BindInformation bindInformation;
        private final @NotNull SettableFuture<ListenerStartupInformation> settableFuture;

        UpdateGivenFutureListener(
                final @NotNull BindInformation bindInformation,
                final @NotNull SettableFuture<ListenerStartupInformation> settableFuture) {
            this.bindInformation = bindInformation;
            this.settableFuture = settableFuture;
        }

        @Override
        public void operationComplete(final @NotNull ChannelFuture future) throws Exception {
            final Listener listener = bindInformation.getListener();
            final int bindPort = ((InetSocketAddress) future.channel().localAddress()).getPort();
            listener.setPort(bindPort);
            if (future.isSuccess()) {
                settableFuture.set(ListenerStartupInformation.successfulListenerStartup(bindPort, listener));
            } else {
                settableFuture.set(ListenerStartupInformation.failedListenerStartup(bindPort, listener, future.cause()));
            }
        }
    }
}
