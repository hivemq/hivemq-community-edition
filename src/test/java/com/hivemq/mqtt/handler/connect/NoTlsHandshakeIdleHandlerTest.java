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

package com.hivemq.mqtt.handler.connect;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.timeout.IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT;
import static io.netty.handler.timeout.IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 */
@SuppressWarnings("NullabilityAnnotations")
public class NoTlsHandshakeIdleHandlerTest {

    private MqttServerDisconnector mqttServerDisconnector;
    private NoTlsHandshakeIdleHandler handler;
    private EmbeddedChannel channel;
    private AtomicBoolean userEventTriggered;

    @Before
    public void setUp() throws Exception {
        mqttServerDisconnector = mock(MqttServerDisconnector.class);
        handler = new NoTlsHandshakeIdleHandler(mqttServerDisconnector);
        userEventTriggered = new AtomicBoolean(false);
        final ChannelInboundHandlerAdapter eventAdapter = new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
                userEventTriggered.set(true);
            }
        };
        channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.pipeline().addLast(handler);
        channel.pipeline().addLast(eventAdapter);
    }

    @Test
    public void test_nothing_happens_for_non_idle_state_event() throws Exception {
        handler.userEventTriggered(channel.pipeline().context(handler), "SomeEvent");

        verify(mqttServerDisconnector, never()).logAndClose(any(Channel.class), any(), any());
        assertTrue(userEventTriggered.get());
    }

    @Test
    public void test_nothing_happens_for_idle_state_writer_event() throws Exception {

        handler.userEventTriggered(channel.pipeline().context(handler), FIRST_WRITER_IDLE_STATE_EVENT);

        verify(mqttServerDisconnector, never()).logAndClose(any(Channel.class), any(), any());
        assertTrue(userEventTriggered.get());
    }

    @Test
    public void test_idle_state_reader_event() throws Exception {

        handler.userEventTriggered(channel.pipeline().context(handler), FIRST_READER_IDLE_STATE_EVENT);

        verify(mqttServerDisconnector, times(1)).logAndClose(any(Channel.class), any(), any());
        assertFalse(userEventTriggered.get());
    }
}