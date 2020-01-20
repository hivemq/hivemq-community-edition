/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttDisconnectUtil;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.Attribute;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class Mqtt31ConnectDecoderValidationsTest {


    @Mock
    private Channel channel;

    @Mock
    private Attribute attribute;

    @Mock
    private EventLog eventLog;

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
                new Mqtt3ServerDisconnector(new MqttDisconnectUtil(eventLog)),
                eventLog,
                new TestConfigurationBootstrap().getFullConfigurationService(),
                new HivemqId());

        when(channel.attr(ChannelAttributes.CLIENT_ID)).thenReturn(attribute);
        when(channel.attr(ChannelAttributes.CONNECT_KEEP_ALIVE)).thenReturn(attribute);
        when(channel.attr(ChannelAttributes.CLEAN_START)).thenReturn(attribute);
    }


    @Test
    public void test_invalid_protocol_name_mqtt_invalid_case() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("Mqisdp".getBytes(UTF_8));

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(channel).close();
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test
    public void test_invalid_protocol_name_mqtt_wrong_spelled() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("QMIsdp".getBytes(UTF_8));

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(channel).close();
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }


    @Test
    public void test_invalid_will_is_not_set_but_will_qos_1() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0000_1000);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(channel).close();
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test
    public void test_invalid_will_is_not_set_but_will_qos_2() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0001_0000);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(channel).close();
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test
    public void test_invalid_will_is_not_set_but_will_retain() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQTT".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0010_0000);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(channel).close();
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test
    public void test_invalid_will_is_set_but_qos_3() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0001_1100);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(channel).close();
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test
    public void test_invalid_password_set_but_no_username() {

        final ByteBuf buffer = Unpooled.buffer(12);
        buffer.writeBytes(new byte[]{0, 6});
        buffer.writeBytes("MQIsdp".getBytes(UTF_8));
        buffer.writeByte(4);
        buffer.writeByte(0b0100_0000);

        assertNull(decoder.decode(channel, buffer, fixedHeader));

        verify(channel).close();
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
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

        verify(channel).close();
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
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

        verify(connacker).connackError(any(Channel.class), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED), anyString());
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

        verify(channel).close();
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test
    public void test_invalid_persistent_session_but_no_user_name() {

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

        final ArgumentCaptor<CONNACK> captor = ArgumentCaptor.forClass(CONNACK.class);

        verify(channel).writeAndFlush(captor.capture());

        verify(cf).addListener(eq(ChannelFutureListener.CLOSE));

        assertEquals(Mqtt3ConnAckReturnCode.REFUSED_IDENTIFIER_REJECTED, captor.getValue().getReturnCode());
    }

}