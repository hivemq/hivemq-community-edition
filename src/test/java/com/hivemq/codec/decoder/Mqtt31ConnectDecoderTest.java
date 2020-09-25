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
import com.hivemq.codec.decoder.mqtt3.Mqtt31ConnectDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ClientIds;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class Mqtt31ConnectDecoderTest {

    @Mock
    private Channel channel;

    @Mock
    private ChannelFuture channelFuture;

    @Mock
    private EventLog eventLog;

    private Mqtt31ConnectDecoder decoder;

    @Mock
    private Attribute<ProtocolVersion> protocolVersionAttribute;

    @Mock
    private MqttConnacker connacker;

    @Mock
    Attribute<String> clientIdAttr;


    private static final byte fixedHeader = 0b0001_0000;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(channel.writeAndFlush(any())).thenReturn(channelFuture);
        when(channel.attr(any(AttributeKey.class))).thenReturn(mock(Attribute.class));
        when(channel.attr(ChannelAttributes.CLIENT_ID)).thenReturn(clientIdAttr);
        when(clientIdAttr.get()).thenReturn("clientId");
        when(channel.attr(ChannelAttributes.MQTT_VERSION)).thenReturn(protocolVersionAttribute);
        when(protocolVersionAttribute.get()).thenReturn(ProtocolVersion.MQTTv3_1);

        decoder = new Mqtt31ConnectDecoder(connacker,
                new ClientIds(new HivemqId()),
                new TestConfigurationBootstrap().getFullConfigurationService(),
                new HivemqId());
    }


    @Test
    public void test_decode_no_will_no_user_no_pw() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0010);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1, connectPacket.getProtocolVersion());
        assertEquals("clientId", connectPacket.getClientIdentifier());
        assertEquals(14, connectPacket.getKeepAlive());
        assertEquals(true, connectPacket.isCleanStart());
        assertEquals(0, connectPacket.getSessionExpiryInterval());
        assertEquals(null, connectPacket.getWillPublish());

        assertNull(connectPacket.getPassword());
        assertNull(connectPacket.getUsername());

    }

    @Test
    public void test_decode_no_will_no_user_no_pw_no_clean_session() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0000);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertEquals(ProtocolVersion.MQTTv3_1, connectPacket.getProtocolVersion());
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

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        assertEquals(ProtocolVersion.MQTTv3_1, connectPacket.getProtocolVersion());
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

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        assertEquals(ProtocolVersion.MQTTv3_1, connectPacket.getProtocolVersion());
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

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        assertEquals(ProtocolVersion.MQTTv3_1, connectPacket.getProtocolVersion());
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

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        assertEquals(ProtocolVersion.MQTTv3_1, connectPacket.getProtocolVersion());
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

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        decoder.decode(channel, buf, fixedHeader);
        verify(connacker).connackError(any(), any(), anyString(), any(), anyString());

    }

    @Test
    public void test_decode_will_qos2_no_user_no_pw() {
        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        assertEquals(ProtocolVersion.MQTTv3_1, connectPacket.getProtocolVersion());
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

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        assertEquals(ProtocolVersion.MQTTv3_1, connectPacket.getProtocolVersion());
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
    public void test_empty_client_id_disconnected() {

        final ChannelFuture cf = mock(ChannelFuture.class);

        when(channel.writeAndFlush(any())).thenReturn(cf);

        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b0000_0010);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(0);

        final CONNECT connectPacket = decoder.decode(channel, buf, fixedHeader);

        assertNull(connectPacket);
        verify(connacker).connackError(any(), any(), anyString(), any(), anyString());
    }

    @Test
    public void test_wrong_client_id_length() {

        final ChannelFuture cf = mock(ChannelFuture.class);

        when(channel.writeAndFlush(any())).thenReturn(cf);

        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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
        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
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

    @Test
    public void test_decode_will_topic_non_character() {

        final byte[] willtopic = ("willTopic" + '\uFFFF' + 'a' + 'b').getBytes();

        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b1100_0100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(12);
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

    @Test
    public void test_decode_will_topic_control_character() {

        final byte[] willtopic = ("willTopic" + '\u0013' + 'a' + 'b').getBytes();

        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(new byte[]{0, 6});
        buf.writeBytes("MQIsdp".getBytes(UTF_8));
        buf.writeByte(4);
        buf.writeByte(0b1100_0100);
        //keepAlive
        buf.writeShort(14);
        //payload length
        buf.writeShort(8);
        buf.writeBytes("clientId".getBytes(UTF_8));
        buf.writeShort(12);
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