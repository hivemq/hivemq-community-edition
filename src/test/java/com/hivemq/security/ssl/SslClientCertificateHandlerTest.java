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
package com.hivemq.security.ssl;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.netty.ChannelHandlerNames;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public class SslClientCertificateHandlerTest {

    private EmbeddedChannel channel;

    @Mock
    private MqttServerDisconnectorImpl mqttServerDisconnector;

    @Mock
    private Tls tls;

    @Mock
    private SslHandler sslHandler;

    @Mock
    private SSLEngine sslEngine;

    @Mock
    private SSLSession sslSession;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(sslHandler.engine()).thenReturn(sslEngine);
        when(sslEngine.getSession()).thenReturn(sslSession);

        channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.pipeline().addLast(new SslClientCertificateHandler(tls, mqttServerDisconnector));
        channel.pipeline().addLast(ChannelHandlerNames.SSL_HANDLER, sslHandler);
    }

    @Test
    public void test_user_event_not_ssl_cert_event() {

        channel.pipeline().fireUserEventTriggered("");
        assertNotNull(channel.pipeline().get(SslClientCertificateHandler.class));
    }

    @Test
    public void test_success() throws SSLPeerUnverifiedException {

        when(tls.getClientAuthMode()).thenReturn(Tls.ClientAuthMode.OPTIONAL);
        when(sslSession.getPeerCertificates()).thenReturn(new Certificate[0]);
        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);

        assertNotNull(channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getAuthCertificate());

    }

    @Test
    public void test_not_success() {

        channel.pipeline().fireUserEventTriggered(new SslHandshakeCompletionEvent(new RuntimeException()));
        verify(sslHandler, never()).engine();
    }

    @Test
    public void test_peer_not_authenticated_but_required() throws SSLPeerUnverifiedException, InterruptedException {

        when(tls.getClientAuthMode()).thenReturn(Tls.ClientAuthMode.REQUIRED);
        when(sslSession.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("peer not authenticated"));
        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);

        verify(mqttServerDisconnector).logAndClose(eq(channel), isNull(), anyString());
    }

    @Test
    public void test_peer_not_authenticated_but_optional() throws SSLPeerUnverifiedException, InterruptedException {

        when(tls.getClientAuthMode()).thenReturn(Tls.ClientAuthMode.OPTIONAL);
        when(sslSession.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("peer not authenticated"));
        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);

        verify(mqttServerDisconnector, never()).logAndClose(eq(channel), anyString(), anyString());
    }

    @Test
    public void test_peer_not_verified_but_required() throws SSLPeerUnverifiedException, InterruptedException {

        when(tls.getClientAuthMode()).thenReturn(Tls.ClientAuthMode.REQUIRED);
        when(sslSession.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("peer not verified"));
        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);

        verify(mqttServerDisconnector).logAndClose(eq(channel), isNull(), anyString());
    }

    @Test
    public void test_peer_not_verified_but_optional() throws SSLPeerUnverifiedException, InterruptedException {

        when(tls.getClientAuthMode()).thenReturn(Tls.ClientAuthMode.OPTIONAL);
        when(sslSession.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("peer not verified"));
        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);

        verify(mqttServerDisconnector, never()).logAndClose(eq(channel), anyString(), anyString());
    }

    @Test
    public void test_peer_other_exception() throws SSLPeerUnverifiedException, InterruptedException {

        when(tls.getClientAuthMode()).thenReturn(Tls.ClientAuthMode.OPTIONAL);
        when(sslSession.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("other exception"));
        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);

        verify(mqttServerDisconnector).logAndClose(eq(channel), isNull(), anyString());
    }

    @Test
    public void test_class_cast_exception_no_ssl_handler() throws SSLPeerUnverifiedException, InterruptedException {

        channel = new EmbeddedChannel();
        channel.pipeline().addLast(new SslClientCertificateHandler(tls, mqttServerDisconnector));
        channel.pipeline().addLast(ChannelHandlerNames.SSL_HANDLER, new WrongHandler());

        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);

        verify(mqttServerDisconnector).logAndClose(eq(channel), isNull(), anyString());
    }

    private class WrongHandler extends SimpleChannelInboundHandler<Object> {

        @Override
        protected void channelRead0(final ChannelHandlerContext channelHandlerContext, final Object o) throws Exception {
            super.channelRead(channelHandlerContext, o);
        }
    }

}