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

import com.hivemq.codec.decoder.mqtt3.Mqtt31ConnectDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ClientIds;
import com.hivemq.util.ReasonStrings;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Attribute;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class Mqtt31ConnectDecoderValidationsTest {


    @Mock
    private Channel channel;

    @Mock
    private Attribute attribute;

    @Mock
    private FullConfigurationService fullConfigurationService;

    @Mock
    private MqttConfigurationService mqttConfigurationService;

    @Mock
    private MqttConnacker connacker;

    private Mqtt31ConnectDecoder decoder;

    private static final byte fixedHeader = 0b0001_0000;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        when(fullConfigurationService.mqttConfiguration()).thenReturn(mqttConfigurationService);
        decoder = new Mqtt31ConnectDecoder(connacker,
                new ClientIds(new HivemqId()),
                new TestConfigurationBootstrap().getFullConfigurationService(),
                new HivemqId());

        when(channel.attr(ChannelAttributes.CLIENT_ID)).thenReturn(attribute);
        when(channel.attr(ChannelAttributes.CONNECT_KEEP_ALIVE)).thenReturn(attribute);
        when(channel.attr(ChannelAttributes.CLEAN_START)).thenReturn(attribute);
    }

    @Test
    public void test_invalid_header() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[11]);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), anyString());
    }

    @Test
    public void test_invalid_protocol_name_mqtt_invalid_case() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("Mqisdp".getBytes(UTF_8));

        //fill the rest to have enough readable bytes.
        buffer.writeBytes(new byte[4]);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION), anyString());
    }

    @Test
    public void test_invalid_protocol_name_mqtt_wrong_spelled() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("QMIsdp".getBytes(UTF_8));

        //fill the rest to have enough readable bytes.
        buffer.writeBytes(new byte[4]);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION), anyString());
    }


    @Test
    public void test_invalid_will_is_not_set_but_will_qos_1() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_1000);

        //fill the rest to have enough readable bytes.
        buffer.writeBytes(new byte[2]);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_invalid_will_is_not_set_but_will_qos_2() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0001_0000);

        //fill the rest to have enough readable bytes.
        buffer.writeBytes(new byte[2]);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_invalid_will_is_not_set_but_will_retain() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0010_0000);

        //fill the rest to have enough readable bytes.
        buffer.writeBytes(new byte[2]);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_invalid_will_is_set_but_qos_3() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0001_1100);

        //fill the rest to have enough readable bytes.
        buffer.writeBytes(new byte[2]);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_invalid_password_set_but_no_username() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0100_0000);

        //fill the rest to have enough readable bytes.
        buffer.writeBytes(new byte[2]);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.PROTOCOL_ERROR), eq(ReasonStrings.CONNACK_PROTOCOL_ERROR_INVALID_USER_PASS_COMB_MQTT3));
    }

    @Test
    public void test_invalid_client_id_length() {
        final int invalidLength = 100;
        final ByteBuf buffer = Unpooled.buffer();

        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_0000);
        buffer.writeShort(60);

        buffer.writeShort(invalidLength);
        buffer.writeBytes("clientID".getBytes());

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_invalid_will_topic_length() {
        final int invalidLength = 100;
        final ByteBuf buffer = Unpooled.buffer();

        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
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

        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b1000_0000);
        buffer.writeShort(60);

        Strings.createPrefixedBytesFromString("clientID", buffer);

        buffer.writeShort(invalidLength);
        buffer.writeBytes("user".getBytes());

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.MALFORMED_PACKET), anyString());
    }

    @Test
    public void test_invalid_persistent_session_but_no_client_id() {

        final ChannelFuture cf = mock(ChannelFuture.class);

        when(channel.writeAndFlush(any())).thenReturn(cf);

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_0000);
        //keepAlive
        buffer.writeShort(14);
        //payload length
        buffer.writeShort(0);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID), anyString());

    }

}