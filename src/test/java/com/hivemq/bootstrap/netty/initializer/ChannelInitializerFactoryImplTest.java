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
package com.hivemq.bootstrap.netty.initializer;

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.bootstrap.netty.ChannelInitializerFactoryImpl;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.security.ssl.NonSslHandler;
import com.hivemq.security.ssl.SslFactory;
import io.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static util.TlsTestUtil.createDefaultTLS;

@SuppressWarnings("NullabilityAnnotations")
public class ChannelInitializerFactoryImplTest {

    private static final String TYPE_TCP = "TCP";
    private static final String TYPE_TLS_TCP = "TYPE_TLS_TCP";
    private static final String TYPE_WEBSOCKET = "TYPE_WEBSOCKET";
    private static final String TYPE_TLS_WEBSOCKET = "TYPE_TLS_WEBSOCKET";

    @Mock
    private ChannelDependencies channelDependencies;

    @Mock
    private SslFactory sslFactory;

    @Mock
    private Provider<NonSslHandler> nonSslHandlerProvider;

    @Mock
    private EventLog eventLog;

    @Mock
    private FullConfigurationService fullConfigurationService;

    @Mock
    private RestrictionsConfigurationService restrictionsConfigurationService;

    private ChannelInitializerFactoryImpl channelInitializerFactory;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(channelDependencies.getConfigurationService()).thenReturn(fullConfigurationService);
        when(channelDependencies.getRestrictionsConfigurationService()).thenReturn(restrictionsConfigurationService);
        when(restrictionsConfigurationService.incomingLimit()).thenReturn(0L);
        channelInitializerFactory = new TestChannelInitializerFactory(channelDependencies,
                sslFactory,
                nonSslHandlerProvider);
    }

    @Test
    public void test_get_tcp_initializer() {
        final TcpListener tcpListener = new TcpListener(0, "0");

        final AbstractChannelInitializer initializer = channelInitializerFactory.getChannelInitializer(tcpListener);

        assertEquals(TYPE_TCP, ((FakeAbstractChannelInitializer) initializer).getType());
    }

    @Test
    public void test_get_tls_tcp_initializer() {

        final Tls tls = createDefaultTLS();
        final TlsTcpListener tlsTcpListener = new TlsTcpListener(0, "0", tls);

        final AbstractChannelInitializer initializer = channelInitializerFactory.getChannelInitializer(tlsTcpListener);

        assertEquals(TYPE_TLS_TCP, ((FakeAbstractChannelInitializer) initializer).getType());
    }

    @Test
    public void test_get_websocket_initializer() {

        final WebsocketListener websocketListener = new WebsocketListener.Builder()
                .bindAddress("0")
                .port(0)
                .build();

        final AbstractChannelInitializer initializer = channelInitializerFactory.getChannelInitializer(websocketListener);

        assertEquals(TYPE_WEBSOCKET, ((FakeAbstractChannelInitializer) initializer).getType());

    }

    @Test
    public void test_get_tls_websocket_initializer() {

        final Tls tls = createDefaultTLS();

        final TlsWebsocketListener websocketListener = new TlsWebsocketListener.Builder()
                .bindAddress("0")
                .port(0)
                .tls(tls)
                .build();

        final AbstractChannelInitializer initializer = channelInitializerFactory.getChannelInitializer(websocketListener);

        assertEquals(TYPE_TLS_WEBSOCKET, ((FakeAbstractChannelInitializer) initializer).getType());
    }

    @Test
    public void test_create_tcp() {

        channelInitializerFactory = new ChannelInitializerFactoryImpl(channelDependencies, sslFactory, nonSslHandlerProvider, eventLog);
        final TcpListener tcpListener = new TcpListener(0, "0");
        final AbstractChannelInitializer channelInitializer = channelInitializerFactory.getChannelInitializer(tcpListener);
        assertTrue(channelInitializer instanceof TcpChannelInitializer);

    }

    @Test
    public void test_create_tcp_tls() {

        channelInitializerFactory = new ChannelInitializerFactoryImpl(channelDependencies, sslFactory, nonSslHandlerProvider, eventLog);
        final Tls tls = createDefaultTLS();
        final TlsTcpListener tlsTcpListener = new TlsTcpListener(0, "0", tls);
        final AbstractChannelInitializer channelInitializer = channelInitializerFactory.getChannelInitializer(tlsTcpListener);
        assertTrue(channelInitializer instanceof TlsTcpChannelInitializer);

    }

    @Test
    public void test_create_websocket() {

        channelInitializerFactory = new ChannelInitializerFactoryImpl(channelDependencies, sslFactory, nonSslHandlerProvider, eventLog);
        final WebsocketListener websocketListener = new WebsocketListener.Builder()
                .bindAddress("0")
                .port(0)
                .build();
        final AbstractChannelInitializer channelInitializer = channelInitializerFactory.getChannelInitializer(websocketListener);
        assertTrue(channelInitializer instanceof WebsocketChannelInitializer);

    }

    @Test
    public void test_create_websocket_tls() {

        channelInitializerFactory = new ChannelInitializerFactoryImpl(channelDependencies, sslFactory, nonSslHandlerProvider, eventLog);
        final Tls tls = createDefaultTLS();

        final TlsWebsocketListener websocketListener = new TlsWebsocketListener.Builder()
                .bindAddress("0")
                .port(0)
                .tls(tls)
                .build();
        final AbstractChannelInitializer channelInitializer = channelInitializerFactory.getChannelInitializer(websocketListener);
        assertTrue(channelInitializer instanceof TlsWebsocketChannelInitializer);

    }

    @SuppressWarnings("NullabilityAnnotations")
    private class TestChannelInitializerFactory extends ChannelInitializerFactoryImpl {

        TestChannelInitializerFactory(final ChannelDependencies channelDependencies,
                                      final SslFactory sslFactory,
                                      final Provider<NonSslHandler> nonSslHandlerProvider) {
            super(channelDependencies, sslFactory, nonSslHandlerProvider, eventLog);
        }

        @NotNull
        protected AbstractChannelInitializer createTcpInitializer(@NotNull final TcpListener listener) {
            return new FakeAbstractChannelInitializer(channelDependencies, TYPE_TCP);
        }

        @NotNull
        protected AbstractChannelInitializer createTlsTcpInitializer(@NotNull final TlsTcpListener listener) {
            return new FakeAbstractChannelInitializer(channelDependencies, TYPE_TLS_TCP);
        }

        @NotNull
        protected AbstractChannelInitializer createWebsocketInitializer(@NotNull final WebsocketListener listener) {
            return new FakeAbstractChannelInitializer(channelDependencies, TYPE_WEBSOCKET);
        }

        @NotNull
        protected AbstractChannelInitializer createTlsWebsocketInitializer(@NotNull final TlsWebsocketListener listener) {
            return new FakeAbstractChannelInitializer(channelDependencies, TYPE_TLS_WEBSOCKET);
        }
    }

    private class FakeAbstractChannelInitializer extends AbstractChannelInitializer {

        private final String type;

        public FakeAbstractChannelInitializer(final ChannelDependencies channelDependencies, final String type) {
            super(channelDependencies, new FakeListener());
            this.type = type;
        }

        @Override
        protected void addSpecialHandlers(@NotNull final Channel ch) {
            //no-op
        }

        public String getType() {
            return type;
        }
    }

    private class FakeListener implements Listener {

        @Override
        public int getPort() {
            return 0;
        }

        @Override
        public String getBindAddress() {
            return null;
        }

        @Override
        public String readableName() {
            return null;
        }

        @Override
        public @NotNull String getName() {
            return "listener";
        }
    }
}