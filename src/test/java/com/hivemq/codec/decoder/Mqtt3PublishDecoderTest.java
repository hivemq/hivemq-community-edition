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

import com.google.common.primitives.Bytes;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMqttDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

@SuppressWarnings("NullabilityAnnotations")
public class Mqtt3PublishDecoderTest {

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
    }

    @Test
    public void test_valid_pub_qos_0() {

        final String topic = "topic";
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0000);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertEquals(QoS.AT_MOST_ONCE, publish.getQoS());
        assertEquals(topic, publish.getTopic());
        assertEquals(false, publish.isDuplicateDelivery());
        assertEquals(false, publish.isRetain());
        assertEquals(0, publish.getPacketIdentifier());
        assertArrayEquals(payload.getBytes(UTF_8), publish.getPayload());

        //Make sure we didn't get disconnected for some reason
        assertEquals(true, embeddedChannel.isActive());

        assertNotNull(publish.getHivemqId());
        assertNotNull(publish.getUniqueId());
        assertTrue(publish.getPublishId() > 0);
        assertTrue(publish.getTimestamp() > 0);
    }

    @Test
    public void test_valid_pub_qos_1() {

        final String topic = "topic";
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0010);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeShort(1);
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertEquals(QoS.AT_LEAST_ONCE, publish.getQoS());
        assertEquals(topic, publish.getTopic());
        assertEquals(false, publish.isDuplicateDelivery());
        assertEquals(false, publish.isRetain());
        assertEquals(1, publish.getPacketIdentifier());
        assertArrayEquals(payload.getBytes(UTF_8), publish.getPayload());

        //Make sure we didn't get disconnected for some reason
        assertEquals(true, embeddedChannel.isActive());
    }


    @Test
    public void test_valid_pub_qos_2() {

        final String topic = "topic";
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0100);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeShort(1);
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertEquals(QoS.EXACTLY_ONCE, publish.getQoS());
        assertEquals(topic, publish.getTopic());
        assertEquals(false, publish.isDuplicateDelivery());
        assertEquals(false, publish.isRetain());
        assertEquals(1, publish.getPacketIdentifier());
        assertArrayEquals(payload.getBytes(UTF_8), publish.getPayload());

        //Make sure we didn't get disconnected for some reason
        assertEquals(true, embeddedChannel.isActive());
    }

    @Test
    public void test_valid_pub_qos_0_retained() {

        final String topic = "topic";
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0001);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertEquals(QoS.AT_MOST_ONCE, publish.getQoS());
        assertEquals(topic, publish.getTopic());
        assertEquals(false, publish.isDuplicateDelivery());
        assertEquals(true, publish.isRetain());
        assertEquals(0, publish.getPacketIdentifier());
        assertArrayEquals(payload.getBytes(UTF_8), publish.getPayload());

        //Make sure we didn't get disconnected for some reason
        assertEquals(true, embeddedChannel.isActive());
    }

    @Test
    public void test_valid_pub_retain_not_supported() {

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setRetainedMessagesEnabled(false);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));

        final String topic = "topic";
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0001);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertNull(publish);
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_valid_pub_qos_1_dup() {

        final String topic = "topic";
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_1010);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeShort(1);
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertEquals(QoS.AT_LEAST_ONCE, publish.getQoS());
        assertEquals(topic, publish.getTopic());
        assertEquals(true, publish.isDuplicateDelivery());
        assertEquals(false, publish.isRetain());
        assertEquals(1, publish.getPacketIdentifier());
        assertArrayEquals(payload.getBytes(UTF_8), publish.getPayload());

        //Make sure we didn't get disconnected for some reason
        assertEquals(true, embeddedChannel.isActive());
    }

    @Test
    public void test_invalid_dup_set_for_qos0() {

        final String topic = "topic";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_1000);
        buf.writeByte(topic.getBytes(UTF_8).length + 2);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final Object publish = embeddedChannel.readInbound();

        assertNull(publish);

        //We got disconnected because an invalid qos was received
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_invalid_qos() {

        final String topic = "topic";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0110);
        buf.writeByte(topic.getBytes(UTF_8).length + 2);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final Object publish = embeddedChannel.readInbound();

        assertNull(publish);

        //We got disconnected because an invalid qos was received
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_invalid_message_id_for_qos1_message_0() {

        final String topic = "topic";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0010);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + 2);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeShort(0);
        embeddedChannel.writeInbound(buf);

        final Object publish = embeddedChannel.readInbound();

        assertNull(publish);

        //We got disconnected because an invalid qos was received
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_invalid_message_id_for_qos2_message_0() {

        final String topic = "topic";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0110);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + 2);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeShort(0);
        embeddedChannel.writeInbound(buf);

        final Object publish = embeddedChannel.readInbound();

        assertNull(publish);

        //We got disconnected because an invalid qos was received
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_invalid_topic_wildcard_subtree_level() {

        final String topic = "topic/#";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0000);
        buf.writeByte(topic.getBytes(UTF_8).length + 2);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final Object publish = embeddedChannel.readInbound();

        assertNull(publish);

        //We got disconnected because an invalid qos was received
        assertEquals(false, embeddedChannel.isActive());
    }


    @Test
    public void test_invalid_topic_wildcard_topic_level() {

        final String topic = "topic/+/subtopic";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0000);
        buf.writeByte(topic.getBytes(UTF_8).length + 2);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final Object publish = embeddedChannel.readInbound();

        assertNull(publish);

        //We got disconnected because an invalid qos was received
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_valid_pub_max_topic_length() {

        final String topic = RandomStringUtils.randomAlphabetic(65535);
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0000);

        buf.writeBytes(new byte[]{(byte) 0x88, (byte) 0x80, 4});

        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertEquals(QoS.AT_MOST_ONCE, publish.getQoS());
        assertEquals(topic, publish.getTopic());
        assertEquals(false, publish.isDuplicateDelivery());
        assertEquals(false, publish.isRetain());
        assertEquals(0, publish.getPacketIdentifier());
        assertArrayEquals(payload.getBytes(UTF_8), publish.getPayload());

        //Make sure we didn't get disconnected for some reason
        assertEquals(true, embeddedChannel.isActive());
    }

    @Test
    public void test_invalid_topic_length() {

        final int invalidLength = 1000;

        final String topic = "topic";
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_0000);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(invalidLength);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertNull(publish);

        //Make sure we did get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_topic_contains_control_character() {

        final String topic = "topic" + '\u0013';
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_1010);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeShort(1);
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertNull(publish);

        //Make sure we did get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_topic_contains_non_character() {

        final String topic = "topic" + '\uFFFF';
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_1010);
        buf.writeByte(topic.getBytes(UTF_8).length + 2 + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.getBytes(UTF_8).length);
        buf.writeBytes(topic.getBytes(UTF_8));
        buf.writeShort(1);
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertNull(publish);

        //Make sure we did get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_topic_contains_bad_utf_8_character() {

        final byte[] bytes = {(byte) 0xE0, (byte) 0x80};

        final byte[] topic = Bytes.concat("topic".getBytes(), bytes);
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_1010);
        buf.writeByte(topic.length + 2 + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.length);
        buf.writeBytes(topic);
        buf.writeShort(1);
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertNull(publish);

        //Make sure we did get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_topic_contains_another_bad_utf_8_character() {

        final byte[] bytes = {(byte) 0xED, (byte) 0xA0};

        final byte[] topic = Bytes.concat("topic".getBytes(), bytes);
        final String payload = "payload";

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0011_1010);
        buf.writeByte(topic.length + 2 + 2 + payload.getBytes(UTF_8).length);
        buf.writeShort(topic.length);
        buf.writeBytes(topic);
        buf.writeShort(1);
        buf.writeBytes(payload.getBytes(UTF_8));
        embeddedChannel.writeInbound(buf);

        final PUBLISH publish = embeddedChannel.readInbound();

        assertNull(publish);

        //Make sure we did get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }
}