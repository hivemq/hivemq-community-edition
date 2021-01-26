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
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@Ignore("A SUBACK is never decoded by HiveMQ")
public class Mqtt3SubackDecoderTest {

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
    }

    @Test
    public void test_suback_received() {
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1001_0000);
        buf.writeByte(0b0000_0101);
        buf.writeShort(55555);
        buf.writeByte(0);
        buf.writeByte(1);
        buf.writeByte(2);
        embeddedChannel.writeInbound(buf);

        final SUBACK suback = embeddedChannel.readInbound();

        assertEquals(55555, suback.getPacketIdentifier());
        assertArrayEquals(suback.getReasonCodes().toArray(), new Mqtt5SubAckReasonCode[]{Mqtt5SubAckReasonCode.GRANTED_QOS_0, Mqtt5SubAckReasonCode.GRANTED_QOS_1, Mqtt5SubAckReasonCode.GRANTED_QOS_2});

        assertEquals(true, embeddedChannel.isActive());
    }

    @Test
    public void test_suback_invalid_header_mqtt_311() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1001_0010);
        buf.writeByte(0b0000_0101);
        buf.writeShort(55555);
        buf.writeByte(0);
        buf.writeByte(1);
        buf.writeByte(2);
        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_suback_invalid_qos() {
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1001_0010);
        buf.writeByte(0b0000_0101);
        buf.writeShort(55555);
        buf.writeByte(0);
        buf.writeByte(1);
        buf.writeByte(3);
        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }


    @Test
    public void test_suback_invalid_header_mqtt_31() {

        //In this test we check that additional headers are ignored in MQTT 3.1 if they're invalid
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1001_0010);
        buf.writeByte(0b0000_0101);
        buf.writeShort(55555);
        buf.writeByte(0);
        buf.writeByte(1);
        buf.writeByte(2);
        embeddedChannel.writeInbound(buf);

        final SUBACK suback = embeddedChannel.readInbound();

        assertEquals(55555, suback.getPacketIdentifier());

        assertEquals(true, embeddedChannel.isActive());
    }
}