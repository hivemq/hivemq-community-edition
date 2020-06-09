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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.netty.ChannelInitializerFactoryImpl;
import com.hivemq.bootstrap.netty.NettyConfiguration;
import com.hivemq.bootstrap.netty.initializer.AbstractChannelInitializer;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.RandomPortGenerator;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static util.TlsTestUtil.createDefaultTLS;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class HiveMQNettyBootstrapTest {

    private HiveMQNettyBootstrap hiveMQNettyBootstrap;

    @Mock
    private ShutdownHooks shutdownHooks;

    @Mock
    private ListenerConfigurationService listenerConfigurationService;

    @Mock
    private ChannelInitializerFactoryImpl channelInitializerFactoryImpl;

    @Mock
    private AbstractChannelInitializer abstractChannelInitializer;

    private final int randomPort = RandomPortGenerator.get();

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        hiveMQNettyBootstrap = new HiveMQNettyBootstrap(shutdownHooks, listenerConfigurationService, channelInitializerFactoryImpl,
                new NettyConfiguration(NioServerSocketChannel.class, NioSocketChannel.class, new NioEventLoopGroup(1), new NioEventLoopGroup(1)));

        when(channelInitializerFactoryImpl.getChannelInitializer(any(Listener.class))).thenReturn(abstractChannelInitializer);
    }

    @Test
    public void test_server_bootstrap_no_listeners() {

        when(listenerConfigurationService.getTcpListeners()).thenReturn(Lists.newArrayList());
        when(listenerConfigurationService.getTlsTcpListeners()).thenReturn(Lists.newArrayList());
        when(listenerConfigurationService.getWebsocketListeners()).thenReturn(Lists.newArrayList());
        when(listenerConfigurationService.getTlsWebsocketListeners()).thenReturn(Lists.newArrayList());

        hiveMQNettyBootstrap.bootstrapServer();
    }

    @Test
    public void test_server_bootstrap_tcp() throws Exception {

        setupTcpListener(randomPort);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture = hiveMQNettyBootstrap.bootstrapServer();

        //check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(1, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
    }


    @Test
    public void test_server_bootstrap_tls_tcp() throws Exception {
        setupTlsTcpListener(randomPort);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture = hiveMQNettyBootstrap.bootstrapServer();

        //check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(1, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
    }


    @Test
    public void test_server_bootstrap_websocket() throws Exception {
        setupWebsocketListener(randomPort);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture = hiveMQNettyBootstrap.bootstrapServer();

        //check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(1, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
    }


    @Test
    public void test_server_bootstrap_tls_websocket() throws Exception {
        setupTlsWebsocketListener(randomPort);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture = hiveMQNettyBootstrap.bootstrapServer();

        //check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(1, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
    }

    @Test
    public void test_server_bootstrap_combined() throws Exception {

        setupTcpListener(randomPort);
        setupTlsTcpListener(randomPort + 1);
        setupWebsocketListener(randomPort + 2);
        setupTlsWebsocketListener(randomPort + 3);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture = hiveMQNettyBootstrap.bootstrapServer();

        //check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(4, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
        assertTrue(listenableFuture.get().get(1).isSuccessful());
        assertTrue(listenableFuture.get().get(2).isSuccessful());
        assertTrue(listenableFuture.get().get(3).isSuccessful());
    }

    private TlsWebsocketListener createTlsWebsocketListener(final int givenPort) {
        final Tls tls = createDefaultTLS();
        final String bindAddress = "0.0.0.0";
        final TlsWebsocketListener tlsWebsocketListener = new TlsWebsocketListener.Builder()
                .bindAddress(bindAddress).port(givenPort).tls(tls).build();

        return tlsWebsocketListener;
    }

    private TcpListener createTcpListener(final int givenPort) {
        final TcpListener tcpListener = new TcpListener(givenPort, "0.0.0.0");

        return tcpListener;
    }

    private TlsTcpListener createTlsTcpListener(final int givenPort) {
        final Tls tls = createDefaultTLS();
        final String bindAddress = "0.0.0.0";

        final TlsTcpListener tlsTcpListener = new TlsTcpListener(givenPort, bindAddress, tls);
        return tlsTcpListener;
    }

    private WebsocketListener createWebsocketListener(final int givenPort) {
        final String bindAddress = "0.0.0.0";
        final WebsocketListener websocketListener = new WebsocketListener.Builder()
                .bindAddress(bindAddress).port(givenPort).build();
        return websocketListener;
    }

    private void setupTlsWebsocketListener(final int givenPort) {
        final List<TlsWebsocketListener> tlsWebsocketListeners = Lists.newArrayList(createTlsWebsocketListener(givenPort));
        when(listenerConfigurationService.getTlsWebsocketListeners()).thenReturn(tlsWebsocketListeners);
    }

    private void setupTcpListener(final int givenPort) {
        final List<TcpListener> tcpListeners = Lists.newArrayList(createTcpListener(givenPort));
        when(listenerConfigurationService.getTcpListeners()).thenReturn(tcpListeners);
    }

    private void setupTlsTcpListener(final int givenPort) {

        final List<TlsTcpListener> tlsTcpListeners = Lists.newArrayList(createTlsTcpListener(givenPort));
        when(listenerConfigurationService.getTlsTcpListeners()).thenReturn(tlsTcpListeners);
    }

    private void setupWebsocketListener(final int givenPort) {

        final List<WebsocketListener> websocketListeners = Lists.newArrayList(createWebsocketListener(givenPort));
        when(listenerConfigurationService.getWebsocketListeners()).thenReturn(websocketListeners);
    }


}
