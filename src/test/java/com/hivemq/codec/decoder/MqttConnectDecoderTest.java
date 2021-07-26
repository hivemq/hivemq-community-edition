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

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.HivemqId;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ClientIds;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class MqttConnectDecoderTest {

    private static final byte fixedHeader = 0b0001_0000;

    private @NotNull MqttConnacker mqttConnacker;
    private @NotNull Channel channel;
    private @NotNull MqttConnectDecoder decoder;
    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        mqttConnacker = mock(MqttConnacker.class);
        final HivemqId hiveMQId = new HivemqId();
        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(null);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        decoder = new MqttConnectDecoder(mqttConnacker,
                new TestConfigurationBootstrap().getFullConfigurationService(),
                hiveMQId,
                new ClientIds(hiveMQId));
    }

    @Test
    public void test_no_protocol_version() {
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{1});
        decoder.decode(channel, buf, fixedHeader);
        verify(mqttConnacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION), anyString());
    }

    @Test
    public void test_invalid_protocol_version_not_enough_readable_bytes() {
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 1, 2, 3, 4});
        decoder.decode(channel, buf, fixedHeader);
        verify(mqttConnacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION), anyString());
    }

    @Test
    public void test_valid_mqtt5_version() {
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 'M', 'Q', 'T', 'T', 5});
        try {
            decoder.decode(channel, buf, fixedHeader);
        } catch (final Exception e) {
            //ignore because mqtt5ConnectDecoder not tested here
        }

        assertSame(ProtocolVersion.MQTTv5, clientConnection.getProtocolVersion());
        assertNotNull(channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getConnectReceivedTimestamp());
    }

    @Test
    public void test_valid_mqtt3_1_1_version() {
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 'M', 'Q', 'T', 'T', 4});
        decoder.decode(channel, buf, fixedHeader);
        assertSame(ProtocolVersion.MQTTv3_1_1, clientConnection.getProtocolVersion());
        assertNotNull(channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getConnectReceivedTimestamp());
    }

    @Test
    public void test_valid_mqtt3_1_version() {
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 6, 'M', 'Q', 'T', 'T', 3, 1});
        decoder.decode(channel, buf, fixedHeader);
        assertSame(ProtocolVersion.MQTTv3_1, clientConnection.getProtocolVersion());
        assertNotNull(channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getConnectReceivedTimestamp());
    }

    @Test
    public void test_invalid_protocol_version_mqtt_5() {
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 5});
        decoder.decode(channel, buf, fixedHeader);
        verify(mqttConnacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION), anyString());
    }

    @Test
    public void test_invalid_protocol_version_7() {
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 4, 'M', 'Q', 'T', 'T', 7});
        decoder.decode(channel, buf, fixedHeader);
        verify(mqttConnacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION), anyString());
    }

    @Test
    public void test_invalid_protocol_version_length() {
        final ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0, 5, 'M', 'Q', 'T', 'T', 7});
        decoder.decode(channel, buf, fixedHeader);
        verify(mqttConnacker).connackError(eq(channel), anyString(), anyString(), eq(Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION), anyString());
    }
}