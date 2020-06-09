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
package com.hivemq.codec.encoder;

import com.hivemq.codec.encoder.mqtt3.Mqtt3ConnackEncoder;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Mqtt3ConnackEncoderTest {

    private EmbeddedChannel channel;

    private Mqtt3ConnackEncoder encoder;

    @Before
    public void setUp() throws Exception {

        encoder = new Mqtt3ConnackEncoder();
        channel = new EmbeddedChannel(encoder);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
    }


    @Test
    public void test_mqtt311_connack_no_sp() throws Exception {

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        final CONNACK connack = new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED, false);
        channel.writeOutbound(connack);

        final ByteBuf buf = channel.readOutbound();

        assertEquals(encoder.bufferSize(channel.pipeline().context(encoder), connack), buf.readableBytes());

        //Fixed header
        assertEquals(0b0010_0000, buf.readByte());
        //Length
        assertEquals(0b0000_0010, buf.readByte());
        //Flags
        assertEquals(0b0000_0000, buf.readByte());
        //Accepted
        assertEquals(0b0000_0000, buf.readByte());

        //Nothing more to read
        assertEquals(0, buf.readableBytes());

        //Let's make sure we weren't disconnected
        assertEquals(true, channel.isActive());
    }

    @Test
    public void test_mqtt311_connack_session_present() throws Exception {

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final CONNACK connack = new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED, true);
        channel.writeOutbound(connack);

        final ByteBuf buf = channel.readOutbound();

        assertEquals(encoder.bufferSize(channel.pipeline().context(encoder), connack), buf.readableBytes());

        //Fixed header
        assertEquals(0b0010_0000, buf.readByte());
        //Length
        assertEquals(0b0000_0010, buf.readByte());
        //Flags
        assertEquals(0b0000_0001, buf.readByte());
        //Accepted
        assertEquals(0b0000_0000, buf.readByte());

        //Nothing more to read
        assertEquals(0, buf.readableBytes());

        //Let's make sure we weren't disconnected
        assertEquals(true, channel.isActive());
    }


    @Test
    public void test_mqtt31_connack() throws Exception {

        final CONNACK connack = new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED);
        channel.writeOutbound(connack);

        final ByteBuf buf = channel.readOutbound();

        assertEquals(encoder.bufferSize(channel.pipeline().context(encoder), connack), buf.readableBytes());

        //Fixed header
        assertEquals(0b0010_0000, buf.readByte());
        //Length
        assertEquals(0b0000_0010, buf.readByte());
        //Flags
        assertEquals(0b0000_0000, buf.readByte());
        //Accepted
        assertEquals(0b0000_0000, buf.readByte());

        //Nothing more to read
        assertEquals(0, buf.readableBytes());

        //Let's make sure we weren't disconnected
        assertEquals(true, channel.isActive());
    }

    @Test
    public void test_mqtt31_unacceptable_protocol_version() throws Exception {

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        final CONNACK connack = new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
        channel.writeOutbound(connack);

        final ByteBuf buf = channel.readOutbound();

        assertEquals(encoder.bufferSize(channel.pipeline().context(encoder), connack), buf.readableBytes());

        //Fixed header
        assertEquals(0b0010_0000, buf.readByte());
        //Length
        assertEquals(0b0000_0010, buf.readByte());
        //Flags
        assertEquals(0b0000_0000, buf.readByte());
        //Refused
        assertEquals(0b0000_0001, buf.readByte());

        //Nothing more to read
        assertEquals(0, buf.readableBytes());

        //Let's make sure we were disconnected because the return code was refused
        assertEquals(false, channel.isActive());
    }

    @Test
    public void test_disconnected_after_identifier_rejected() throws Exception {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel(new Mqtt3ConnackEncoder());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        embeddedChannel.writeOutbound(new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_IDENTIFIER_REJECTED));

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_disconnected_after_unacceptable_protocol_version() throws Exception {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel(new Mqtt3ConnackEncoder());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        embeddedChannel.writeOutbound(new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION));

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_disconnected_after_bad_username_pasword() throws Exception {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel(new Mqtt3ConnackEncoder());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        embeddedChannel.writeOutbound(new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_BAD_USERNAME_OR_PASSWORD));

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_disconnected_after_not_authorized() throws Exception {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel(new Mqtt3ConnackEncoder());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        embeddedChannel.writeOutbound(new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED));

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_disconnected_after_server_unavailable() throws Exception {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel(new Mqtt3ConnackEncoder());
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        embeddedChannel.writeOutbound(new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_SERVER_UNAVAILABLE));

        assertEquals(false, embeddedChannel.isActive());
    }

}