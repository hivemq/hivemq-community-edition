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

import com.hivemq.configuration.HivemqId;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.connack.MqttConnackSendUtil;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttDisconnectUtil;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ClientIds;
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

import static org.mockito.Mockito.*;

public class MqttConnectDecoderTest {

    @Mock
    Channel channel;

    @Mock
    EventLog eventLog;

    @Mock
    Attribute<ProtocolVersion> protocolVersionAttribute;

    private MqttConnectDecoder decoder;

    private static final byte fixedHeader = 0b0001_0000;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        final HivemqId hiveMQId = new HivemqId();

        final MqttDisconnectUtil mqttDisconnectUtil = new MqttDisconnectUtil(eventLog);
        final MqttConnackSendUtil mqttConnackSendUtil = new MqttConnackSendUtil(eventLog);
        final Mqtt5ServerDisconnector mqtt5ServerDisconnector = new Mqtt5ServerDisconnector(mqttDisconnectUtil);
        final Mqtt3ServerDisconnector mqtt3ServerDisconnector = new Mqtt3ServerDisconnector(mqttDisconnectUtil);
        final MqttConnacker mqttConnacker = new MqttConnacker(mqttConnackSendUtil);

        decoder = new MqttConnectDecoder(mqtt5ServerDisconnector,
                mqtt3ServerDisconnector,
                mqttConnacker,
                eventLog,
                new TestConfigurationBootstrap().getFullConfigurationService(),
                hiveMQId,
                new ClientIds(hiveMQId));
    }

    @Test
    public void test_no_protocol_version() {

        final ChannelFuture cf = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(cf);

        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{1});

        decoder.decode(channel, buf, fixedHeader);

        verify(channel).close();

    }

    @Test
    public void test_invalid_protocol_version_not_enough_readable_bytes() {

        final ChannelFuture cf = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(cf);

        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 1, 2, 3, 4});

        decoder.decode(channel, buf, fixedHeader);

        verify(channel).close();

    }

    @Test
    public void test_valid_mqtt5_version() {

        final ChannelFuture cf = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(cf);
        when(channel.attr(ChannelAttributes.MQTT_VERSION)).thenReturn(protocolVersionAttribute);

        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 'M', 'Q', 'T', 'T', 5});

        try {
            decoder.decode(channel, buf, fixedHeader);
        } catch (final Exception e) {
            //ignore because mqtt5ConnectDecoder not tested here
        }

        verify(protocolVersionAttribute).set(ProtocolVersion.MQTTv5);

    }

    @Test
    public void test_valid_mqtt3_1_1_version() {

        final ChannelFuture cf = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(cf);
        when(channel.attr(ChannelAttributes.MQTT_VERSION)).thenReturn(protocolVersionAttribute);

        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 'M', 'Q', 'T', 'T', 4});

        decoder.decode(channel, buf, fixedHeader);

        verify(protocolVersionAttribute).set(ProtocolVersion.MQTTv3_1_1);

    }

    @Test
    public void test_valid_mqtt3_1_version() {

        final ChannelFuture cf = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(cf);
        when(channel.attr(ChannelAttributes.MQTT_VERSION)).thenReturn(protocolVersionAttribute);

        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 6, 'M', 'Q', 'T', 'T', 3, 1});

        decoder.decode(channel, buf, fixedHeader);

        verify(protocolVersionAttribute).set(ProtocolVersion.MQTTv3_1);

    }

    @Test
    public void test_invalid_protocol_version_mqtt_5() {

        final ChannelFuture cf = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(cf);
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 5});

        decoder.decode(channel, buf, fixedHeader);

        verify(channel).close();

    }

    @Test
    public void test_invalid_protocol_version_7() {

        final ChannelFuture cf = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(cf);
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 'M', 'Q', 'T', 'T', 7});

        decoder.decode(channel, buf, fixedHeader);

        verify(channel).close();

    }

    @Test
    public void test_invalid_protocol_version_length() {

        final ChannelFuture cf = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(cf);
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 5, 'M', 'Q', 'T', 'T', 7});


        decoder.decode(channel, buf, fixedHeader);

        verify(channel).close();

    }
}