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
package com.hivemq.codec.decoder.mqtt311;

import com.google.common.primitives.Bytes;
import com.hivemq.codec.decoder.mqtt3.Mqtt311ConnectDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ClientIds;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class Mqtt311ConnectDecoderTest {

    @Mock
    Channel channel;

    @Mock
    EventLog eventLog;

    @Mock
    private FullConfigurationService fullConfiguration;

    @Mock
    private SecurityConfigurationService securityConfigurationService;

    @Mock
    private MqttConnacker connacker;


    private Mqtt311ConnectDecoder decoder;

    private static final byte fixedHeader = 0b0001_0000;
    private HivemqId hiveMQId;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        when(channel.attr(any(AttributeKey.class))).thenReturn(mock(Attribute.class));
        when(fullConfiguration.securityConfiguration()).thenReturn(securityConfigurationService);
        when(securityConfigurationService.validateUTF8()).thenReturn(true);

        hiveMQId = new HivemqId();
        decoder = new Mqtt311ConnectDecoder(connacker,
                new ClientIds(hiveMQId),
                new TestConfigurationBootstrap().getFullConfigurationService(),
                hiveMQId);
    }

    @Test
    public void test_decode_no_will_no_user_no_pw() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0010);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1_1, connectPacket.getProtocolVersion());
        assertEquals("clientId", connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(true, connectPacket.isCleanStart());
        assertEquals(null, connectPacket.getWillPublish());

        assertNull(connectPacket.getPassword());
        assertNull(connectPacket.getUsername());

    }

    @Test
    public void test_decode_no_will_no_user_no_pw_no_clean_session() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0000);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1_1, connectPacket.getProtocolVersion());
        assertEquals("clientId", connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(false, connectPacket.isCleanStart());
        assertEquals(SESSION_EXPIRY_MAX, connectPacket.getSessionExpiryInterval());
        assertEquals(null, connectPacket.getWillPublish());

        assertNull(connectPacket.getPassword());
        assertNull(connectPacket.getUsername());

    }

    @Test
    public void test_decode_no_will_user_no_pw() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b1000_0000);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(8);
        buf.writeBytes("username".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1_1, connectPacket.getProtocolVersion());
        assertEquals("clientId", connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(false, connectPacket.isCleanStart());
        assertEquals(SESSION_EXPIRY_MAX, connectPacket.getSessionExpiryInterval());
        assertEquals(null, connectPacket.getWillPublish());

        assertEquals("username", connectPacket.getUsername());
        assertNull(connectPacket.getPassword());

    }

    @Test
    public void test_decode_no_will_user_pw() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b1100_0000);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(8);
        buf.writeBytes("username".getBytes(UTF_8));
        buf.writeShort(8);
        buf.writeBytes("password".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1_1, connectPacket.getProtocolVersion());
        assertEquals("clientId", connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(false, connectPacket.isCleanStart());
        assertEquals(SESSION_EXPIRY_MAX, connectPacket.getSessionExpiryInterval());
        assertEquals(null, connectPacket.getWillPublish());

        assertEquals("username", connectPacket.getUsername());
        assertArrayEquals("password".getBytes(UTF_8), connectPacket.getPassword());
    }

    @Test
    public void test_decode_will_user_pw() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b1100_0100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(9);
        buf.writeBytes("willTopic".getBytes(UTF_8));
        buf.writeShort(11);
        buf.writeBytes("willPayload".getBytes(UTF_8));
        buf.writeShort(8);
        buf.writeBytes("username".getBytes(UTF_8));
        buf.writeShort(8);
        buf.writeBytes("password".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1_1, connectPacket.getProtocolVersion());
        assertEquals("clientId", connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(false, connectPacket.isCleanStart());
        assertEquals(SESSION_EXPIRY_MAX, connectPacket.getSessionExpiryInterval());
        assertEquals(false, connectPacket.getWillPublish().isRetain());

        assertEquals("username", connectPacket.getUsername());
        assertArrayEquals("password".getBytes(UTF_8), connectPacket.getPassword());
        assertArrayEquals("willPayload".getBytes(UTF_8), connectPacket.getWillPublish().getPayload());
        assertEquals("willTopic", connectPacket.getWillPublish().getTopic());
        assertEquals(QoS.AT_MOST_ONCE, connectPacket.getWillPublish().getQos());
    }

    @Test
    public void test_decode_will_no_user_no_pw() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(9);
        buf.writeBytes("willTopic".getBytes(UTF_8));
        buf.writeShort(11);
        buf.writeBytes("willPayload".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1_1, connectPacket.getProtocolVersion());
        assertEquals("clientId", connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(false, connectPacket.isCleanStart());
        assertEquals(SESSION_EXPIRY_MAX, connectPacket.getSessionExpiryInterval());
        assertEquals(false, connectPacket.getWillPublish().isRetain());

        assertNull(connectPacket.getUsername());
        assertNull(connectPacket.getPassword());
        assertArrayEquals("willPayload".getBytes(UTF_8), connectPacket.getWillPublish().getPayload());
        assertEquals("willTopic", connectPacket.getWillPublish().getTopic());
        assertEquals(QoS.AT_MOST_ONCE, connectPacket.getWillPublish().getQos());
    }

    @Test
    public void test_decode_will_no_user_no_pw_zero_length_topic() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(0);
        buf.writeBytes("".getBytes(UTF_8));
        buf.writeShort(11);
        buf.writeBytes("willPayload".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertNull(connectPacket);
        verify(connacker).connackError(any(Channel.class), isNull(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_decode_will_qos2_no_user_no_pw() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0001_0100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(9);
        buf.writeBytes("willTopic".getBytes(UTF_8));
        buf.writeShort(11);
        buf.writeBytes("willPayload".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1_1, connectPacket.getProtocolVersion());
        assertEquals("clientId", connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(false, connectPacket.isCleanStart());
        assertEquals(SESSION_EXPIRY_MAX, connectPacket.getSessionExpiryInterval());
        assertEquals(false, connectPacket.getWillPublish().isRetain());

        assertNull(connectPacket.getUsername());
        assertNull(connectPacket.getPassword());
        assertArrayEquals("willPayload".getBytes(UTF_8), connectPacket.getWillPublish().getPayload());
        assertEquals("willTopic", connectPacket.getWillPublish().getTopic());
        assertEquals(QoS.EXACTLY_ONCE, connectPacket.getWillPublish().getQos());
    }

    @Test
    public void test_decode_will_qos1_retain_no_user_no_pw() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0010_1100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(9);
        buf.writeBytes("willTopic".getBytes(UTF_8));
        buf.writeShort(11);
        buf.writeBytes("willPayload".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1_1, connectPacket.getProtocolVersion());
        assertEquals("clientId", connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(false, connectPacket.isCleanStart());
        assertEquals(SESSION_EXPIRY_MAX, connectPacket.getSessionExpiryInterval());
        assertEquals(true, connectPacket.getWillPublish().isRetain());

        assertNull(connectPacket.getUsername());
        assertNull(connectPacket.getPassword());
        assertArrayEquals("willPayload".getBytes(UTF_8), connectPacket.getWillPublish().getPayload());
        assertEquals("willTopic", connectPacket.getWillPublish().getTopic());
        assertEquals(QoS.AT_LEAST_ONCE, connectPacket.getWillPublish().getQos());
    }


    @Test
    public void test_decode_65535_bytes() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0010);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeByte(0b1111_1111);
        buf.writeByte(0b1111_1111);
        final String randomClientId = RandomStringUtils.randomAlphanumeric(65535);
        buf.writeBytes(randomClientId.getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1_1, connectPacket.getProtocolVersion());
        assertEquals(randomClientId, connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(true, connectPacket.isCleanStart());
        //clean session
        assertEquals(0, connectPacket.getSessionExpiryInterval());
        assertEquals(null, connectPacket.getWillPublish());

        assertNull(connectPacket.getPassword());
        assertNull(connectPacket.getUsername());

    }


    @Test
    public void test_empty_client_id_set_to_random() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0010);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(0);

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertTrue(connectPacket.getClientIdentifier().length() > 9);
        assertEquals("hmq_" + hiveMQId.get(), connectPacket.getClientIdentifier().substring(0, 9));
    }

    @Test
    public void test_wrong_client_id_length() {

        final ChannelFuture cf = mock(ChannelFuture.class);

        when(channel.writeAndFlush(any())).thenReturn(cf);

        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0010);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(1000);

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertNull(connectPacket);
        assertFalse(channel.isActive());
    }

    @Test
    public void test_client_id_contains_control_character() {

        final ByteBuf buf = Unpooled.buffer();
        final String clientId = "clientId" + '\u0013';

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0010_1100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(9);
        buf.writeBytes(clientId.getBytes(UTF_8));
        buf.writeShort(9);
        buf.writeBytes("willTopic".getBytes(UTF_8));
        buf.writeShort(11);
        buf.writeBytes("willPayload".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertNull(connectPacket);
        assertFalse(channel.isActive());
    }

    @Test
    public void test_client_id_contains_non_character() {

        final ByteBuf buf = Unpooled.buffer();
        final String clientId = "clientId" + '\uFFFF';

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0010_1100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(9);
        buf.writeBytes(clientId.getBytes(UTF_8));
        buf.writeShort(9);
        buf.writeBytes("willTopic".getBytes(UTF_8));
        buf.writeShort(11);
        buf.writeBytes("willPayload".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertNull(connectPacket);
        assertFalse(channel.isActive());
    }

    @Test
    public void test_client_id_contains_bad_utf8_character() {

        final byte[] bytes = {(byte) 0xE0, (byte) 0x80};
        final byte[] clientId = Bytes.concat("clientId".getBytes(), bytes);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0010_1100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(10);
        buf.writeBytes(clientId);
        buf.writeShort(9);
        buf.writeBytes("willTopic".getBytes(UTF_8));
        buf.writeShort(11);
        buf.writeBytes("willPayload".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertNull(connectPacket);
        assertFalse(channel.isActive());
    }

    @Test
    public void test_decode_bad_will_topic() {

        final byte[] bytes = {(byte) 0xE0, (byte) 0x80};
        final byte[] willtopic = Bytes.concat("willTopic".getBytes(), bytes);

        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 4});
        buf.writeBytes("MQTT".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b1100_0100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(11);
        buf.writeBytes(willtopic);
        buf.writeShort(11);
        buf.writeBytes("willPayload".getBytes(UTF_8));
        buf.writeShort(8);
        buf.writeBytes("username".getBytes(UTF_8));
        buf.writeShort(8);
        buf.writeBytes("password".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertNull(connectPacket);
        assertFalse(channel.isActive());
    }
}