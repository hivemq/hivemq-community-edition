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
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.bootstrap.netty.ChannelHandlerNames;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.DummyClientConnection;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public class SslParameterHandlerTest {

    private EmbeddedChannel channel;

    @Mock
    private SslHandler sslHandler;

    @Mock
    private SSLEngine sslEngine;

    @Mock
    private SSLSession sslSession;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        channel = new EmbeddedChannel();
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(new DummyClientConnection(channel, null));
        channel.pipeline().addLast(new SslParameterHandler());
        channel.pipeline().addLast(ChannelHandlerNames.SSL_HANDLER, sslHandler);
    }

    @Test
    public void test_other_user_event() {
        channel.pipeline().fireUserEventTriggered("");
        assertNotNull(channel.pipeline().get(SslParameterHandler.class));
    }

    @Test
    public void test_ssl_completion_user_event() {
        when(sslHandler.engine()).thenReturn(sslEngine);
        when(sslEngine.getSession()).thenReturn(sslSession);
        when(sslSession.getCipherSuite()).thenReturn("CipherSuite");
        when(sslSession.getProtocol()).thenReturn("Protocol");
        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);
        assertEquals("Protocol", ClientConnection.of(channel).getAuthProtocol());
        assertEquals("CipherSuite", ClientConnection.of(channel).getAuthCipherSuite());
        assertNull(channel.pipeline().get(SslParameterHandler.class));
    }
}
