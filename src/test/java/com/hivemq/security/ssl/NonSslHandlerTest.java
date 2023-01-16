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
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public class NonSslHandlerTest {

    private EmbeddedChannel channel;

    private MqttServerDisconnector disconnector;

    private static final byte[] VALID_SSL_PACKET = {
            22, 3, 3, 1, 42, 1, 0, 1, 38, 3, 3, 54, -41, -98, 66, -38, 61, 6, -46, 107, -62, 115, 33, 110, 94, -82,
            92, -1, -54, 9, -55, -88, -13, -96, -28, 35, -61, 47, -123, 94, -10, -83, -108, 0, 0, 86, -64, 44, -64,
            43, -64, 48, 0, -99, -64, 46, -64, 50, 0, -97, 0, -93, -64, 47, 0, -100, -64, 45, -64, 49, 0, -98, 0,
            -94, -64, 36, -64, 40, 0, 61, -64, 38, -64, 42, 0, 107, 0, 106, -64, 10, -64, 20, 0, 53, -64, 5, -64, 15,
            0, 57, 0, 56, -64, 35, -64, 39, 0, 60, -64, 37, -64, 41, 0, 103, 0, 64, -64, 9, -64, 19, 0, 47, -64, 4,
            -64, 14, 0, 51, 0, 50, 0, -1, 1, 0, 0, -89, 0, 5, 0, 5, 1, 0, 0, 0, 0, 0, 10, 0, 32, 0, 30, 0, 23, 0, 24,
            0, 25, 0, 9, 0, 10, 0, 11, 0, 12, 0, 13, 0, 14, 0, 22, 1, 0, 1, 1, 1, 2, 1, 3, 1, 4, 0, 11, 0, 2, 1, 0, 0,
            13, 0, 40, 0, 38, 4, 3, 5, 3, 6, 3, 8, 4, 8, 5, 8, 6, 8, 9, 8, 10, 8, 11, 4, 1, 5, 1, 6, 1, 4, 2, 3, 3,
            3, 1, 3, 2, 2, 3, 2, 1, 2, 2, 0, 50, 0, 40, 0, 38, 4, 3, 5, 3, 6, 3, 8, 4, 8, 5, 8, 6, 8, 9, 8, 10, 8,
            11, 4, 1, 5, 1, 6, 1, 4, 2, 3, 3, 3, 1, 3, 2, 2, 3, 2, 1, 2, 2, 0, 17, 0, 9, 0, 7, 2, 0, 4, 0, 0, 0, 0,
            0, 23, 0, 0, 0, 43, 0, 7, 6, 3, 3, 3, 2, 3, 1
    };

    private static final byte[] VALID_SSL_CONNECT_PACKET = {
            16, 3, 3, 1, 42, 1, 0, 'M', 'Q', 'T', 'T', 54, -41, -98, 66, -38, 61, 6, -46, 107, -62, 115, 33, 110, 94, -82,
            92, -1, -54, 9, -55, -88, -13, -96, -28, 35, -61, 47, -123, 94, -10, -83, -108, 0, 0, 86, -64, 44, -64,
            43, -64, 48, 0, -99, -64, 46, -64, 50, 0, -97, 0, -93, -64, 47, 0, -100, -64, 45, -64, 49, 0, -98, 0,
            -94, -64, 36, -64, 40, 0, 61, -64, 38, -64, 42, 0, 107, 0, 106, -64, 10, -64, 20, 0, 53, -64, 5, -64, 15,
            0, 57, 0, 56, -64, 35, -64, 39, 0, 60, -64, 37, -64, 41, 0, 103, 0, 64, -64, 9, -64, 19, 0, 47, -64, 4,
            -64, 14, 0, 51, 0, 50, 0, -1, 1, 0, 0, -89, 0, 5, 0, 5, 1, 0, 0, 0, 0, 0, 10, 0, 32, 0, 30, 0, 23, 0, 24,
            0, 25, 0, 9, 0, 10, 0, 11, 0, 12, 0, 13, 0, 14, 0, 22, 1, 0, 1, 1, 1, 2, 1, 3, 1, 4, 0, 11, 0, 2, 1, 0, 0,
            13, 0, 40, 0, 38, 4, 3, 5, 3, 6, 3, 8, 4, 8, 5, 8, 6, 8, 9, 8, 10, 8, 11, 4, 1, 5, 1, 6, 1, 4, 2, 3, 3,
            3, 1, 3, 2, 2, 3, 2, 1, 2, 2, 0, 50, 0, 40, 0, 38, 4, 3, 5, 3, 6, 3, 8, 4, 8, 5, 8, 6, 8, 9, 8, 10, 8,
            11, 4, 1, 5, 1, 6, 1, 4, 2, 3, 3, 3, 1, 3, 2, 2, 3, 2, 1, 2, 2, 0, 17, 0, 9, 0, 7, 2, 0, 4, 0, 0, 0, 0,
            0, 23, 0, 0, 0, 43, 0, 7, 6, 3, 3, 3, 2, 3, 1
    };

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        disconnector = new MqttServerDisconnectorImpl(new EventLog());
        channel.pipeline().addLast(new NonSslHandler(disconnector));
    }

    @Test
    public void test_non_ssl_decode_to_small() {

        final byte[] bytes = {
                22, 3, 3, 1, 42, 1, 0, 1, 38, 3,
        };

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(bytes);
        channel.writeInbound(byteBuf);

        assertNotNull(channel.pipeline().get(NonSslHandler.class));

    }

    @Test
    public void test_non_ssl_decode_ssl_packet() {

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(VALID_SSL_PACKET);
        channel.writeInbound(byteBuf);

        assertFalse(channel.isActive());

    }

    @Test
    public void test_non_ssl_decode_connect_packet() {

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(VALID_SSL_CONNECT_PACKET);
        channel.writeInbound(byteBuf);

        assertTrue(channel.isActive());
        assertNull(channel.pipeline().get(NonSslHandler.class));

    }

}