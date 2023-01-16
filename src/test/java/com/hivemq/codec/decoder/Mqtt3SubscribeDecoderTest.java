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
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.*;

public class Mqtt3SubscribeDecoderTest {

    private @NotNull EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        channel = new EmbeddedChannel(TestMqttDecoder.create());
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
    }

    @Test
    public void test_subscribe_received() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(14);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});
        //QoS 1
        buf.writeByte(1);

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});
        //QoS 1
        buf.writeByte(2);


        channel.writeInbound(buf);

        final SUBSCRIBE subscribe = channel.readInbound();

        assertNotNull(subscribe);

        assertEquals(55555, subscribe.getPacketIdentifier());
        assertEquals(2, subscribe.getTopics().size());

        assertEquals(1, subscribe.getTopics().get(0).getQoS().getQosNumber());
        assertEquals("a/b", subscribe.getTopics().get(0).getTopic());

        assertEquals(2, subscribe.getTopics().get(1).getQoS().getQosNumber());
        assertEquals("c/d", subscribe.getTopics().get(1).getTopic());

        assertTrue(channel.isActive());
    }

    @Test
    public void test_subscribe_zero_message_id() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(14);

        //MessageID
        buf.writeShort(0);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});
        //QoS 1
        buf.writeByte(1);

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});
        //QoS 1
        buf.writeByte(2);


        channel.writeInbound(buf);

        channel.readInbound();

        assertFalse(channel.isActive());
    }

    @Test
    public void test_empty_subscribe() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(2);

        //MessageID
        buf.writeShort(55555);

        channel.writeInbound(buf);

        final SUBSCRIBE subscribe = channel.readInbound();

        assertNull(subscribe);


        //Client got disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_subscription_qos_higher_than_2_received() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(14);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});
        //QoS 3. Invalid!
        buf.writeByte(3);

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});
        //QoS 2
        buf.writeByte(2);


        channel.writeInbound(buf);


        final SUBSCRIBE subscribe = channel.readInbound();

        assertNull(subscribe);


        //Client got disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_subscription_qos_lower_than_0_received() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(14);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});
        //QoS -1. Invalid!
        buf.writeByte(-1);

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});
        //QoS 2
        buf.writeByte(2);


        channel.writeInbound(buf);


        final SUBSCRIBE subscribe = channel.readInbound();

        assertNull(subscribe);

        //Client got disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_subscription_has_null_byte_received() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(14);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});
        //QoS 0. Invalid!
        buf.writeByte(0);

        // "c/NULL"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x0});
        //QoS 2
        buf.writeByte(2);


        channel.writeInbound(buf);


        final SUBSCRIBE subscribe = channel.readInbound();

        assertNull(subscribe);

        //Client got disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_subscription_has_empty_topic() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(5);

        //MessageID
        buf.writeShort(55555);

        // Empty
        buf.writeBytes(new byte[]{0, 0});
        //QoS 0. Invalid!
        buf.writeByte(0);


        channel.writeInbound(buf);


        final SUBSCRIBE subscribe = channel.readInbound();

        assertNull(subscribe);

        //Client got disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_subscription_with_one_byte_message_id() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(1);

        //MessageID
        buf.writeByte(5);

        channel.writeInbound(buf);

        final SUBSCRIBE subscribe = channel.readInbound();

        assertNull(subscribe);

        //Client got disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_subscribe_invalid_header_mqtt_311() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0100);
        //Remaining length
        buf.writeByte(14);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});
        //QoS 1
        buf.writeByte(1);

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});
        //QoS 1
        buf.writeByte(2);

        channel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_subscribe_invalid_header_mqtt_31() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0100);
        //Remaining length
        buf.writeByte(14);

        //MessageID
        buf.writeShort(55555);

        // "a/b"
        buf.writeBytes(new byte[]{0, 3, 0x61, 0x2F, 0x62});
        //QoS 1
        buf.writeByte(1);

        // "c/d"
        buf.writeBytes(new byte[]{0, 3, 0x63, 0x2F, 0x64});
        //QoS 1
        buf.writeByte(2);

        channel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(channel.isActive());
    }


    @Test
    public void test_subscribe_topic_length_max() {

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final String maxTopic1 = RandomStringUtils.randomAlphabetic(65535);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeBytes(new byte[]{(byte) 0x84, (byte) 0x80, 4});

        //MessageID
        buf.writeShort(12345);

        // topic length
        buf.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xFF});

        //topic bytes
        buf.writeBytes(maxTopic1.getBytes());

        //QoS 1
        buf.writeByte(1);

        channel.writeInbound(buf);

        final SUBSCRIBE subscribe = channel.readInbound();

        assertNotNull(subscribe);

        assertEquals(12345, subscribe.getPacketIdentifier());
        assertEquals(1, subscribe.getTopics().size());

        assertEquals(1, subscribe.getTopics().get(0).getQoS().getQosNumber());
        assertEquals(maxTopic1, subscribe.getTopics().get(0).getTopic());

        assertTrue(channel.isActive());

    }
}