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

import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
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

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
    }

    @Test
    public void test_subscribe_received() throws Exception {

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


        embeddedChannel.writeInbound(buf);

        final SUBSCRIBE subscribe = embeddedChannel.readInbound();

        assertNotNull(subscribe);

        assertEquals(55555, subscribe.getPacketIdentifier());
        assertEquals(2, subscribe.getTopics().size());

        assertEquals(1, subscribe.getTopics().get(0).getQoS().getQosNumber());
        assertEquals("a/b", subscribe.getTopics().get(0).getTopic());

        assertEquals(2, subscribe.getTopics().get(1).getQoS().getQosNumber());
        assertEquals("c/d", subscribe.getTopics().get(1).getTopic());

        assertEquals(true, embeddedChannel.isActive());
    }

    @Test
    public void test_subscribe_zero_message_id() throws Exception {

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


        embeddedChannel.writeInbound(buf);

        embeddedChannel.readInbound();

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_empty_subscribe() throws Exception {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(2);

        //MessageID
        buf.writeShort(55555);

        embeddedChannel.writeInbound(buf);

        final SUBSCRIBE subscribe = embeddedChannel.readInbound();

        assertNull(subscribe);


        //Client got disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_subscription_qos_higher_than_2_received() throws Exception {

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


        embeddedChannel.writeInbound(buf);


        final SUBSCRIBE subscribe = embeddedChannel.readInbound();

        assertNull(subscribe);


        //Client got disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_subscription_qos_lower_than_0_received() throws Exception {

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


        embeddedChannel.writeInbound(buf);


        final SUBSCRIBE subscribe = embeddedChannel.readInbound();

        assertNull(subscribe);

        //Client got disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_subscription_has_null_byte_received() throws Exception {

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


        embeddedChannel.writeInbound(buf);


        final SUBSCRIBE subscribe = embeddedChannel.readInbound();

        assertNull(subscribe);

        //Client got disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_subscription_has_empty_topic() throws Exception {

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


        embeddedChannel.writeInbound(buf);


        final SUBSCRIBE subscribe = embeddedChannel.readInbound();

        assertNull(subscribe);

        //Client got disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_subscription_with_one_byte_message_id() throws Exception {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1000_0010);
        //Remaining length
        buf.writeByte(1);

        //MessageID
        buf.writeByte(5);

        embeddedChannel.writeInbound(buf);

        final SUBSCRIBE subscribe = embeddedChannel.readInbound();

        assertNull(subscribe);

        //Client got disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_subscribe_invalid_header_mqtt_311() throws Exception {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

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

        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_subscribe_invalid_header_mqtt_31() throws Exception {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

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

        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }


    @Test
    public void test_subscribe_topic_length_max() throws Exception {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

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

        embeddedChannel.writeInbound(buf);

        final SUBSCRIBE subscribe = embeddedChannel.readInbound();

        assertNotNull(subscribe);

        assertEquals(12345, subscribe.getPacketIdentifier());
        assertEquals(1, subscribe.getTopics().size());

        assertEquals(1, subscribe.getTopics().get(0).getQoS().getQosNumber());
        assertEquals(maxTopic1, subscribe.getTopics().get(0).getTopic());

        assertEquals(true, embeddedChannel.isActive());

    }
}