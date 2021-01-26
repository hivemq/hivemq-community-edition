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
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore("A CONNACK is never decoded by HiveMQ")
public class Mqtt3ConnackDecoderTest {

    private EmbeddedChannel embeddedChannel;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

    }

    @Test
    public void test_session_present_mqtt_311() throws Exception {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0010_0000);
        buf.writeByte(0b0000_0010);
        buf.writeByte(0b0000_0001);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final CONNACK connack = embeddedChannel.readInbound();

        assertNotNull(connack);

        assertEquals(Mqtt3ConnAckReturnCode.ACCEPTED, connack.getReturnCode());
        assertEquals(true, connack.isSessionPresent());
    }

    @Test
    public void test_session_present_mqtt_31() throws Exception {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0010_0000);
        buf.writeByte(0b0000_0010);
        buf.writeByte(0b0000_0001);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        final CONNACK connack = embeddedChannel.readInbound();

        assertNotNull(connack);

        assertEquals(Mqtt3ConnAckReturnCode.ACCEPTED, connack.getReturnCode());
        assertEquals(false, connack.isSessionPresent());
    }

    @Test
    public void test_connack_accepted_received() throws Exception {

        embeddedChannel.writeInbound(createConnack((byte) 0b0000_0000));

        final CONNACK connack = embeddedChannel.readInbound();

        assertNotNull(connack);

        assertEquals(Mqtt3ConnAckReturnCode.ACCEPTED, connack.getReturnCode());
        assertEquals(false, connack.isSessionPresent());

    }

    @Test
    public void test_connack_unacceptable_protocol_version_received() throws Exception {

        embeddedChannel.writeInbound(createConnack((byte) 0b0000_0001));

        final CONNACK connack = embeddedChannel.readInbound();

        assertNotNull(connack);

        assertEquals(Mqtt3ConnAckReturnCode.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, connack.getReturnCode());
    }

    @Test
    public void test_connack_identifier_rejected_received() throws Exception {

        embeddedChannel.writeInbound(createConnack((byte) 0b0000_0010));

        final CONNACK connack = embeddedChannel.readInbound();

        assertNotNull(connack);

        assertEquals(Mqtt3ConnAckReturnCode.REFUSED_IDENTIFIER_REJECTED, connack.getReturnCode());
    }

    @Test
    public void test_connack_server_unavailable_received() throws Exception {

        embeddedChannel.writeInbound(createConnack((byte) 0b0000_0011));

        final CONNACK connack = embeddedChannel.readInbound();

        assertNotNull(connack);

        assertEquals(Mqtt3ConnAckReturnCode.REFUSED_SERVER_UNAVAILABLE, connack.getReturnCode());
    }

    @Test
    public void test_connack_bad_user_or_password_received() throws Exception {

        embeddedChannel.writeInbound(createConnack((byte) 0b0000_0100));

        final CONNACK connack = embeddedChannel.readInbound();

        assertNotNull(connack);

        assertEquals(Mqtt3ConnAckReturnCode.REFUSED_BAD_USERNAME_OR_PASSWORD, connack.getReturnCode());
    }

    @Test
    public void test_connack_not_authorized_received() throws Exception {

        embeddedChannel.writeInbound(createConnack((byte) 0b0000_0101));

        final CONNACK connack = embeddedChannel.readInbound();

        assertNotNull(connack);

        assertEquals(Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED, connack.getReturnCode());
    }


    @Test
    public void test_connack_invalid_header_mqtt_311() throws Exception {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0010_0100);
        buf.writeByte(0b0000_0010);
        buf.writeByte(0b0000_0000);
        buf.writeByte(0b0000_0000);
        embeddedChannel.writeInbound(buf);

        //The client needs to get disconnected
        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_connack_invalid_header_mqtt_31() throws Exception {

        //In this test we check that additional headers are ignored in MQTT 3.1 if they're invalid

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0010_0100);
        buf.writeByte(0b0000_0010);
        buf.writeByte(0b0000_0000);
        buf.writeByte(0b0000_0000);

        embeddedChannel.writeInbound(buf);

        final CONNACK disconnect = embeddedChannel.readInbound();

        assertNotNull(disconnect);

        assertEquals(true, embeddedChannel.isActive());
    }

    private ByteBuf createConnack(final byte lastByte) {
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0010_0000);
        buf.writeByte(0b0000_0010);
        buf.writeByte(0b0000_0000);
        buf.writeByte(lastByte);
        return buf;
    }

}