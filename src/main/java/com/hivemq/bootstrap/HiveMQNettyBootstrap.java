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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.netty.ChannelInitializerFactory;
import com.hivemq.bootstrap.netty.NettyConfiguration;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.util.Validators;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dominik Obermaier
 */
public class HiveMQNettyBootstrap {

    private static final Logger log = LoggerFactory.getLogger(HiveMQNettyBootstrap.class);

    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull ListenerConfigurationService listenerConfigurationService;
    private final @NotNull ChannelInitializerFactory channelInitializerFactory;
    private final @NotNull NettyConfiguration nettyConfiguration;

    @Inject
    HiveMQNettyBootstrap(final @NotNull ShutdownHooks shutdownHooks,
                         final @NotNull ListenerConfigurationService listenerConfigurationService,
                         final @NotNull ChannelInitializerFactory channelInitializerFactory,
                         final @NotNull NettyConfiguration nettyConfiguration) {

        this.shutdownHooks = shutdownHooks;
        this.listenerConfigurationService = listenerConfigurationService;
        this.channelInitializerFactory = channelInitializerFactory;
        this.nettyConfiguration = nettyConfiguration;
    }

    @NotNull
    public ListenableFuture<List<ListenerStartupInformation>> bootstrapServer() {

        //Adding shutdown hook for graceful shutdown
        final int shutdownTimeout = InternalConfigurations.EVENT_LOOP_GROUP_SHUTDOWN_TIMEOUT;
        shutdownHooks.add(new NettyShutdownHook(
                nettyConfiguration.getChildEventLoopGroup(), nettyConfiguration.getParentEventLoopGroup(), shutdownTimeout));

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


    @NotNull
    private List<BindInformation> bindTcpListeners(final @NotNull List<TcpListener> tcpListeners) {
        log.trace("Checking TCP listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();
        for (final TcpListener tcpListener : tcpListeners) {

            final ServerBootstrap b = createServerBootstrap(nettyConfiguration.getParentEventLoopGroup(), nettyConfiguration.getChildEventLoopGroup(), tcpListener);
            final String bindAddress = tcpListener.getBindAddress();

            final Integer port = tcpListener.getPort();


            log.info("Starting TCP listener on address {} and port {}", bindAddress, port);
            final ChannelFuture bind = b.bind(createInetSocketAddress(bindAddress, port));
            futures.add(new BindInformation(tcpListener, bind));
        }
        return futures.build();
    }

    @NotNull
    private List<BindInformation> tlsTcpListeners(final @NotNull List<TlsTcpListener> tlsTcpListeners) {
        log.trace("Checking TLS TCP listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();

        for (final TlsTcpListener tlsTcpListener : tlsTcpListeners) {

            final ServerBootstrap b = createServerBootstrap(nettyConfiguration.getParentEventLoopGroup(), nettyConfiguration.getChildEventLoopGroup(), tlsTcpListener);
            final String bindAddress = tlsTcpListener.getBindAddress();
            final Integer port = tlsTcpListener.getPort();
            log.info("Starting TLS TCP listener on address {} and port {}", bindAddress, port);
            final ChannelFuture bind = b.bind(bindAddress, port);
            futures.add(new BindInformation(tlsTcpListener, bind));
        }
        return futures.build();
    }

    @NotNull
    private List<BindInformation> websocketListeners(final @NotNull List<WebsocketListener> websocketListeners) {
        log.trace("Checking Websocket listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();
        for (final WebsocketListener websocketListener : websocketListeners) {

            final ServerBootstrap b = createServerBootstrap(nettyConfiguration.getParentEventLoopGroup(), nettyConfiguration.getChildEventLoopGroup(), websocketListener);
            final String bindAddress = websocketListener.getBindAddress();
            final Integer port = websocketListener.getPort();
            log.info("Starting Websocket listener on address {} and port {}", bindAddress, port);
            final ChannelFuture bind = b.bind(websocketListener.getBindAddress(), websocketListener.getPort());
            futures.add(new BindInformation(websocketListener, bind));
        }
        return futures.build();
    }

    @NotNull
    private List<BindInformation> tlsWebsocketListeners(final @NotNull List<TlsWebsocketListener> tlsWebsocketListeners) {
        log.trace("Checking Websocket TLS listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();
        for (final TlsWebsocketListener tlsWebsocketListener : tlsWebsocketListeners) {

            final ServerBootstrap b = createServerBootstrap(nettyConfiguration.getParentEventLoopGroup(), nettyConfiguration.getChildEventLoopGroup(), tlsWebsocketListener);
            final String bindAddress = tlsWebsocketListener.getBindAddress();
            final Integer port = tlsWebsocketListener.getPort();
            log.info("Starting Websocket TLS listener on address {} and port {}", bindAddress, port);
            final ChannelFuture bind = b.bind(tlsWebsocketListener.getBindAddress(), tlsWebsocketListener.getPort());
            futures.add(new BindInformation(tlsWebsocketListener, bind));
        }
        return futures.build();
    }

    @NotNull
    private InetSocketAddress createInetSocketAddress(final @NotNull String ip, final int port) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddresses.forString(ip);
        } catch (final IllegalArgumentException e) {
            log.debug("Trying to find out the IP for {} via DNS lookup", ip);
            try {
                inetAddress = InetAddress.getByName(ip);
            } catch (final UnknownHostException e1) {
                log.error("Could not determine IP for hostname {}, unable to bind to it", ip);
                throw new UnrecoverableException(false);
            }
        }

        return new InetSocketAddress(inetAddress, port);
    }

    /**
     * Creates an aggregated future which allows to wait for all futures at once
     *
     * @param bindInformation a list of futures to aggregate
     * @return a {@link com.google.common.util.concurrent.ListenableFuture} which aggregates
     * all given {@link io.netty.channel.ChannelFuture}s
     */
    @NotNull
    private ListenableFuture<List<ListenerStartupInformation>> aggregatedFuture(final @NotNull List<BindInformation> bindInformation) {

        final List<ListenableFuture<ListenerStartupInformation>> listenableFutures = Lists.transform(bindInformation, new Function<BindInformation, ListenableFuture<ListenerStartupInformation>>() {
            @Override
            public ListenableFuture<ListenerStartupInformation> apply(final BindInformation input) {
                final SettableFuture<ListenerStartupInformation> objectSettableFuture = SettableFuture.create();
                input.getBindFuture().addListener(updateGivenFuture(objectSettableFuture, input));
                return objectSettableFuture;
            }
        });
        return Futures.allAsList(listenableFutures);
    }

    /**
     * Updates a given future when the {@link io.netty.channel.ChannelFuture} is finished
     *
     * @param settableFuture a {@link com.google.common.util.concurrent.SettableFuture} to update when the
     *                       ChannelFuture is finished
     * @return a {@link io.netty.channel.ChannelFutureListener} which updates the passed future object when finished
     */
    @NotNull
    private ChannelFutureListener updateGivenFuture(final @NotNull SettableFuture<ListenerStartupInformation> settableFuture, final @NotNull BindInformation bindInformation) {
        return new UpdateGivenFutureListener(bindInformation, settableFuture);
    }

    @NotNull
    private ServerBootstrap createServerBootstrap(final @NotNull EventLoopGroup bossGroup, final @NotNull EventLoopGroup workerGroup, final @NotNull Listener listener) {
        final ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
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
     * @param b        the server bootstrap
     */
    private void setAdvancedOptions(final @NotNull ServerBootstrap b) {

        final int sendBufferSize = InternalConfigurations.LISTENER_SOCKET_SEND_BUFFER_SIZE;
        final int receiveBufferSize = InternalConfigurations.LISTENER_SOCKET_RECEIVE_BUFFER_SIZE;

        if (sendBufferSize > -1) {
            b.childOption(ChannelOption.SO_SNDBUF, sendBufferSize);
        }
        if (receiveBufferSize > -1) {
            b.childOption(ChannelOption.SO_RCVBUF, receiveBufferSize);
        }

        final int writeBufferHigh = InternalConfigurations.LISTENER_CLIENT_WRITE_BUFFER_HIGH_THRESHOLD;
        final int writeBufferLow = InternalConfigurations.LISTENER_CLIENT_WRITE_BUFFER_LOW_THRESHOLD;

        final ClientWriteBufferProperties properties = Validators.validateWriteBufferProperties(new ClientWriteBufferProperties(writeBufferHigh, writeBufferLow));

        //it is assumed that the ClientWriteBufferProperties that the listener returns was validated by Validators.validateWriteBufferProperties()
        b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(properties.getLowThreshold(), properties.getHighThreshold()));

    }

    @Immutable
    private static class UpdateGivenFutureListener implements ChannelFutureListener {
        private final @NotNull BindInformation bindInformation;
        private final @NotNull SettableFuture<ListenerStartupInformation> settableFuture;

        UpdateGivenFutureListener(@NotNull final BindInformation bindInformation,
                                  @NotNull final SettableFuture<ListenerStartupInformation> settableFuture) {
            this.bindInformation = bindInformation;
            this.settableFuture = settableFuture;
        }

        @Override
        public void operationComplete(final @NotNull ChannelFuture future) throws Exception {
            final Listener listener = bindInformation.getListener();
            final int port = listener.getPort();
            if (future.isSuccess()) {
                settableFuture.set(ListenerStartupInformation.successfulListenerStartup(port, listener));
            } else {
                settableFuture.set(ListenerStartupInformation.failedListenerStartup(port, listener, future.cause()));
            }

        }
    }
}
