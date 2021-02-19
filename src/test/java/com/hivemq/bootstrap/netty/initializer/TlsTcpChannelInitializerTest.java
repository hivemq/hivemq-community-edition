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
import com.hivemq.bootstrap.netty.FakeChannelPipeline;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.configuration.service.entity.TlsTcpListener;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.security.ssl.SslFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class TlsTcpChannelInitializerTest {

    @Mock
    private SocketChannel socketChannel;

    @Mock
    private Attribute<Listener> attribute;

    @Mock
    private ChannelDependencies channelDependencies;

    @Mock
    private SslHandler sslHandler;

    @Mock
    private TlsTcpListener tlsTcpListener;

    @Mock
    private Tls tls;

    @Mock
    private SslFactory sslFactory;

    @Mock
    private SslContext sslContext;

    @Mock
    private Future<Channel> future;

    @Mock
    private EventLog eventLog;

    @Mock
    private FullConfigurationService fullConfigurationService;

    @Mock
    private RestrictionsConfigurationService restrictionsConfigurationService;

    private ChannelPipeline pipeline;

    private TlsTcpChannelInitializer tlstcpChannelInitializer;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        pipeline = new FakeChannelPipeline();

        when(tlsTcpListener.getTls()).thenReturn(tls);
        when(sslFactory.getSslContext(any(Tls.class))).thenReturn(sslContext);
        when(sslFactory.getSslHandler(any(SocketChannel.class), any(Tls.class), any(SslContext.class))).thenReturn(sslHandler);
        when(sslHandler.handshakeFuture()).thenReturn(future);
        when(socketChannel.pipeline()).thenReturn(pipeline);
        when(socketChannel.attr(any(AttributeKey.class))).thenReturn(attribute);
        when(socketChannel.isActive()).thenReturn(true);
        when(channelDependencies.getConfigurationService()).thenReturn(fullConfigurationService);
        when(channelDependencies.getRestrictionsConfigurationService()).thenReturn(restrictionsConfigurationService);
        when(restrictionsConfigurationService.incomingLimit()).thenReturn(0L);


        final MqttServerDisconnector mqttServerDisconnector =new MqttServerDisconnectorImpl(eventLog, new HivemqId());
        when(channelDependencies.getMqttServerDisconnector()).thenReturn(mqttServerDisconnector);

        tlstcpChannelInitializer = new TlsTcpChannelInitializer(channelDependencies, tlsTcpListener, sslFactory);

    }

    @Test
    public void test_add_special_handlers() throws Exception {

        when(tls.getClientAuthMode()).thenReturn(Tls.ClientAuthMode.REQUIRED);

        tlstcpChannelInitializer.addSpecialHandlers(socketChannel);

        assertEquals(4, pipeline.names().size());
        assertEquals(SSL_HANDLER, pipeline.names().get(0));
        assertEquals(SSL_EXCEPTION_HANDLER, pipeline.names().get(1));
        assertEquals(SSL_PARAMETER_HANDLER, pipeline.names().get(2));
        assertEquals(SSL_CLIENT_CERTIFICATE_HANDLER, pipeline.names().get(3));
    }

    @Test
    public void test_add_special_handlers_with_timeout() throws Exception {

        when(tls.getClientAuthMode()).thenReturn(Tls.ClientAuthMode.REQUIRED);
        when(tls.getHandshakeTimeout()).thenReturn(30);

        tlstcpChannelInitializer.addSpecialHandlers(socketChannel);

        assertEquals(6, pipeline.names().size());
        assertEquals(SSL_HANDLER, pipeline.names().get(0));
        assertEquals(SSL_EXCEPTION_HANDLER, pipeline.names().get(1));
        assertEquals(SSL_PARAMETER_HANDLER, pipeline.names().get(2));
        assertEquals(SSL_CLIENT_CERTIFICATE_HANDLER, pipeline.names().get(3));
        assertEquals(NEW_CONNECTION_IDLE_HANDLER, pipeline.names().get(pipeline.names().size() - 2));
        assertEquals(NO_TLS_HANDSHAKE_IDLE_EVENT_HANDLER, pipeline.names().get(pipeline.names().size() - 1));
    }

    @Test
    public void test_add_special_handlers_no_cert() throws Exception {


        when(tls.getClientAuthMode()).thenReturn(Tls.ClientAuthMode.NONE);

        tlstcpChannelInitializer.addSpecialHandlers(socketChannel);

        assertEquals(3, pipeline.names().size());
        assertEquals(SSL_HANDLER, pipeline.names().get(0));
        assertEquals(SSL_EXCEPTION_HANDLER, pipeline.names().get(1));
        assertEquals(SSL_PARAMETER_HANDLER, pipeline.names().get(2));
    }


}