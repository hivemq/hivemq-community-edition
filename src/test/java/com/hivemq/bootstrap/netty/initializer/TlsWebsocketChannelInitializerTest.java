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

package com.hivemq.bootstrap.netty.initializer;

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.bootstrap.netty.FakeChannelPipeline;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.configuration.service.entity.TlsWebsocketListener;
import com.hivemq.logging.EventLog;
import com.hivemq.security.ssl.SslFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.DummyHandler;
import util.TlsTestUtil;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class TlsWebsocketChannelInitializerTest {

    @Mock
    private SocketChannel socketChannel;

    @Mock
    private Attribute<Listener> attribute;

    @Mock
    private ChannelDependencies channelDependencies;

    @Mock
    private SslHandler sslHandler;

    @Mock
    private SslFactory ssl;

    @Mock
    private Future<Channel> future;

    @Mock
    private EventLog eventLog;

    @Mock
    private FullConfigurationService fullConfigurationService;

    @Mock
    private TlsWebsocketListener mockListener;

    @Mock
    private Tls tls;

    private ChannelPipeline pipeline;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        pipeline = new FakeChannelPipeline();

        when(socketChannel.pipeline()).thenReturn(pipeline);
        when(socketChannel.attr(any(AttributeKey.class))).thenReturn(attribute);
        when(sslHandler.handshakeFuture()).thenReturn(future);
        when(channelDependencies.getConfigurationService()).thenReturn(fullConfigurationService);
        when(mockListener.getTls()).thenReturn(tls);
        when(ssl.getSslHandler(any(SocketChannel.class), any(Tls.class))).thenReturn(sslHandler);
    }

    @Test
    public void test_add_special_handlers() {

        final TlsWebsocketListener tlsWebsocketListener = new TlsWebsocketListener.Builder()
                .bindAddress("")
                .port(0)
                .tls(TlsTestUtil.createDefaultTLS())
                .build();

        final TlsWebsocketChannelInitializer tlsWebsocketChannelInitializer = new TlsWebsocketChannelInitializer(channelDependencies, tlsWebsocketListener, ssl, eventLog);

        pipeline.addLast(AbstractChannelInitializer.FIRST_ABSTRACT_HANDLER, new DummyHandler());

        tlsWebsocketChannelInitializer.addSpecialHandlers(socketChannel);

        assertEquals(SSL_HANDLER, pipeline.names().get(0));
        assertEquals(SSL_EXCEPTION_HANDLER, pipeline.names().get(1));
        assertEquals(SSL_PARAMETER_HANDLER, pipeline.names().get(2));
        assertEquals(HTTP_SERVER_CODEC, pipeline.names().get(3));
        assertEquals(AbstractChannelInitializer.FIRST_ABSTRACT_HANDLER, pipeline.names().get(pipeline.names().size() - 3));
        assertEquals(NEW_CONNECTION_IDLE_HANDLER, pipeline.names().get(pipeline.names().size() - 2));
        assertEquals(NO_TLS_HANDSHAKE_IDLE_EVENT_HANDLER, pipeline.names().get(pipeline.names().size() - 1));
    }

}