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

import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 */
public class MqttPingreqDecoderTest {

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
    }

    @Test
    public void test_ping_request_received_mqtt_311() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0000);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final Object pingreq = embeddedChannel.readInbound();

        assertEquals(true, pingreq instanceof PINGREQ);

        assertEquals(true, embeddedChannel.isActive());
    }

    @Test
    public void test_ping_request_received_mqtt_5() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0000);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final Object pingreq = embeddedChannel.readInbound();

        assertEquals(true, pingreq instanceof PINGREQ);

        assertEquals(true, embeddedChannel.isActive());
    }

    @Test
    public void test_ping_request_invalid_header_mqtt_311() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0001);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_ping_request_invalid_header_mqtt_5() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0001);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);


        //The client needs to get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_ping_request_invalid_header_ignored_mqtt_31() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        //In this test we check that additional headers are ignored in MQTT 3.1 if they're invalid

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1100_0001);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final Object pingreq = embeddedChannel.readInbound();

        assertEquals(true, pingreq instanceof PINGREQ);

        assertEquals(true, embeddedChannel.isActive());
    }

}