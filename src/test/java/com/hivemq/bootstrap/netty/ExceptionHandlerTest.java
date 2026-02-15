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
package com.hivemq.bootstrap.netty;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.CorruptedFrameException;
import org.junit.Before;
import org.junit.Test;
import util.TestChannelAttribute;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Optional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExceptionHandlerTest {

    private final @NotNull ChannelHandlerContext ctx = mock();
    private final @NotNull ChannelPipeline pipeline = mock();
    private final @NotNull Channel channel = mock();
    private final @NotNull MqttServerDisconnector mqttServerDisconnector = mock();
    private final @NotNull ClientConnection clientConnection = mock();

    private ExceptionHandler handler;

    @Before
    public void before() {
        when(ctx.pipeline()).thenReturn(pipeline);
        when(channel.pipeline()).thenReturn(pipeline);
        when(channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME)).thenReturn(new TestChannelAttribute<>(
                clientConnection));
        when(clientConnection.getChannelIP()).thenReturn(Optional.of("0.0.0.0"));
        when(ctx.channel()).thenReturn(channel);

        handler = new ExceptionHandler(mqttServerDisconnector);
    }

    @Test
    public void test_SSLException() throws Exception {

        handler.exceptionCaught(ctx, new SSLException("test"));

        verify(mqttServerDisconnector, never()).disconnect(any(Channel.class), any(), anyString(), any(), any());
    }

    @Test
    public void test_ClosedChannelException() throws Exception {

        handler.exceptionCaught(ctx, new ClosedChannelException());

        verify(mqttServerDisconnector, never()).disconnect(any(Channel.class), any(), anyString(), any(), any());
    }

    @Test
    public void test_IOException() throws Exception {

        handler.exceptionCaught(ctx, new IOException());

        verify(mqttServerDisconnector, never()).disconnect(any(Channel.class), any(), anyString(), any(), any());
    }

    @Test
    public void test_CorruptedFrameException() throws Exception {

        handler.exceptionCaught(ctx, new CorruptedFrameException());

        verify(mqttServerDisconnector).disconnect(any(Channel.class), any(), anyString(), any(), any());
    }

    @Test
    public void test_IllegalArgumentException() throws Exception {

        handler.exceptionCaught(ctx, new IllegalArgumentException("test"));

        verify(mqttServerDisconnector).disconnect(any(Channel.class), any(), anyString(), any(), any());
    }

    @Test
    public void test_OtherException() throws Exception {

        handler.exceptionCaught(ctx, new RuntimeException("test"));

        verify(mqttServerDisconnector).disconnect(any(Channel.class), any(), anyString(), any(), any());
    }
}
