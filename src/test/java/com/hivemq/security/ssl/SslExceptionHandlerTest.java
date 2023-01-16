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
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.NotSslRecordException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestChannelAttribute;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
public class SslExceptionHandlerTest {

    @Mock
    ChannelHandlerContext ctx;

    @Mock
    Channel channel;

    @Mock
    Throwable throwable;

    @Mock
    EventLog eventLog;

    SslExceptionHandler sslExceptionHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(ctx.channel()).thenReturn(channel);

        final ClientConnection clientConnection = new ClientConnection(channel, null);
        clientConnection.setClientId("client");

        when(channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)).thenReturn(new TestChannelAttribute<>(clientConnection));
        when(channel.isActive()).thenReturn(true);

        final MqttServerDisconnector mqttServerDisconnector = new MqttServerDisconnectorImpl(eventLog);

        sslExceptionHandler = new SslExceptionHandler(mqttServerDisconnector);
    }

    @Test
    public void test_ignorable_exception() throws Exception {
        sslExceptionHandler.exceptionCaught(ctx, new NotSslRecordException());
        verify(channel).close();
        verify(ctx, never()).fireExceptionCaught(any(Throwable.class));
    }

    @Test
    public void test_handshake_exception() throws Exception {
        when(throwable.getCause()).thenReturn(new SSLHandshakeException(""));
        sslExceptionHandler.exceptionCaught(ctx, throwable);
        verify(channel).close();
        verify(ctx, never()).fireExceptionCaught(any(Throwable.class));
    }

    @Test
    public void test_ssl_exception() throws Exception {
        when(throwable.getCause()).thenReturn(new SSLException(""));
        sslExceptionHandler.exceptionCaught(ctx, throwable);
        verify(channel).close();
        verify(ctx, never()).fireExceptionCaught(any(Throwable.class));
    }

    @Test
    public void test_any_exception() throws Exception {
        sslExceptionHandler.exceptionCaught(ctx, throwable);
        verify(channel, never()).close();
        verify(ctx).fireExceptionCaught(any(Throwable.class));

    }
}