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

import com.hivemq.codec.decoder.mqtt3.Mqtt311ConnectDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ClientIds;
import com.hivemq.util.Strings;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class Mqtt311ConnectDecoderValidationsTest {


    @Mock
    private Channel channel;

    @Mock
    private EventLog eventLog;

    @Mock
    private MqttConnacker connacker;

    private Mqtt311ConnectDecoder decoder;

    private static final byte fixedHeader = 0b0001_0000;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        when(channel.attr(any(AttributeKey.class))).thenReturn(mock(Attribute.class));
        decoder = new Mqtt311ConnectDecoder(connacker,
                new ClientIds(new HivemqId()),
                new TestConfigurationBootstrap().getFullConfigurationService(),
                new HivemqId());
    }


    @Test
    public void test_invalid_protocol_name_mqtt_invalid_case() {

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("Mqtt".getBytes(UTF_8));

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }

    @Test
    public void test_invalid_protocol_name_mqtt_wrong_spelled() {

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("QMTT".getBytes(UTF_8));

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }


    @Test
    public void test_invalid_connect_flag() {

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_0001);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }

    @Test
    public void test_invalid_will_is_not_set_but_will_qos_1() {

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_1000);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }

    @Test
    public void test_invalid_will_is_not_set_but_will_qos_2() {

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0001_0000);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }

    @Test
    public void test_invalid_will_is_not_set_but_will_retain() {

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0010_0000);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }

    @Test
    public void test_invalid_will_is_set_but_qos_3() {

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0001_1100);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }

    @Test
    public void test_invalid_password_set_but_no_username() {

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0100_0000);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }

    @Test
    public void test_invalid_client_id_length() {
        final int invalidLength = 100;
        final ByteBuf buffer = Unpooled.buffer();

        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_0000);
        buffer.writeShort(60);

        buffer.writeShort(invalidLength);
        buffer.writeBytes("clientID".getBytes());

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_invalid_will_topic_length() {
        final int invalidLength = 100;
        final ByteBuf buffer = Unpooled.buffer();

        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_0100);
        buffer.writeShort(60);

        Strings.createPrefixedBytesFromString("clientID", buffer);

        buffer.writeShort(invalidLength);
        buffer.writeBytes("willTopic".getBytes());

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_invalid_username_length() {
        final int invalidLength = 100;
        final ByteBuf buffer = Unpooled.buffer();

        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b1000_0000);
        buffer.writeShort(60);

        Strings.createPrefixedBytesFromString("clientID", buffer);

        buffer.writeShort(invalidLength);
        buffer.writeBytes("user".getBytes());

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_no_connect_header() {
        final ByteBuf buffer = Unpooled.buffer(9);

        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);


        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }

    @Test
    public void test_invalid_persistent_session_but_no_client_id() {

        final ChannelFuture cf = mock(ChannelFuture.class);

        when(channel.writeAndFlush(any())).thenReturn(cf);

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_0000);
        //keepAlive
        buffer.writeShort(14);
        //payload length
        buffer.writeShort(0);

        assertNull(decoder.decode(channel, buffer, fixedHeader));
        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID), anyString());
    }

    @Test
    public void test_invalid_clean_session_but_no_client_id_not_allowed() {

        final FullConfigurationService fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfigurationService.securityConfiguration().setAllowServerAssignedClientId(false);

        decoder = new Mqtt311ConnectDecoder(connacker,
                new ClientIds(new HivemqId()),
                fullConfigurationService,
                new HivemqId());

        final ChannelFuture cf = mock(ChannelFuture.class);

        when(channel.writeAndFlush(any())).thenReturn(cf);

        final ByteBuf buffer = Unpooled.buffer(10);
        buffer.writeBytes(new byte[]{0, 4});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_0010);
        //keepAlive
        buffer.writeShort(14);
        //payload length
        buffer.writeShort(0);

        assertNull(decoder.decode(channel, buffer, fixedHeader));
        verify(connacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID), anyString());
    }
}