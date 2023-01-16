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
package com.hivemq.codec.decoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.*;

public class Mqtt3UnsubscribeDecoderTest {

    private @NotNull EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        channel = new EmbeddedChannel(TestMqttDecoder.create());
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
    }

    @Test
    public void test_unsubscribe_received() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0010);
        //Remaining length
        buf.writeByte(12);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});


        channel.writeInbound(buf);

        final UNSUBSCRIBE subscribe = channel.readInbound();

        assertNotNull(subscribe);

        assertEquals(55555, subscribe.getPacketIdentifier());
        assertEquals(2, subscribe.getTopics().size());

        assertEquals("a/b", subscribe.getTopics().get(0));

        assertEquals("c/d", subscribe.getTopics().get(1));

        assertTrue(channel.isActive());
    }

    @Test
    public void test_empty_unsubscribe() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0010);
        //Remaining length
        buf.writeByte(2);

        //MessageID
        buf.writeShort(55555);

        channel.writeInbound(buf);

        final UNSUBSCRIBE unsubscribe = channel.readInbound();

        assertNull(unsubscribe);


        //Client got disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_unsubscribe_has_empty_topic() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0010);
        //Remaining length
        buf.writeByte(4);

        //MessageID
        buf.writeShort(55555);

        // Empty
        buf.writeBytes(new byte[]{0, 0});


        channel.writeInbound(buf);


        final UNSUBSCRIBE unsubscribe = channel.readInbound();

        assertNull(unsubscribe);

        //Client got disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_unsubscribe_invalid_header_mqtt_311() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0100);
        //Remaining length
        buf.writeByte(12);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});

        channel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_unsubscribe_invalid_header_mqtt_31() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1010_0100);
        //Remaining length
        buf.writeByte(12);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});

        channel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(channel.isActive());
    }

}