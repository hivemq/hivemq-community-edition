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
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.DummyClientConnection;
import util.TestConfigurationBootstrap;
import util.TestMqttDecoder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MQTTMessageDecoderTest {

    private @NotNull EmbeddedChannel channel;
    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        channel = new EmbeddedChannel(TestMqttDecoder.create());
        clientConnection = new DummyClientConnection(channel, null);
        //setting version to fake "connected" state
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
    }

    /* ***********************
     * Test invalid messages *
     * ***********************/

    @Test
    public void test_reserved_zero_received() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0000_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void test_reserved_fifteen_received() {

        ClientConnection.of(channel).setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1111_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void test_connack_received() {

        //We must not receive CONNACK from clients because only servers must send CONNACKs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0010_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void test_suback_received() {

        //We must not receive a SUBACK from clients because only servers must send SUBACKs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1001_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void test_unsuback_received() {

        //We must not receive a UNSUBACK from clients because only servers must send UNSUBACKs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1011_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void test_pingresp_received_received() {

        //We must not receive a PINGRESP from clients because only servers must send PINGRESPs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1101_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void test_second_connect_received() {

        clientConnection.setProtocolVersion(null);

        final byte[] connect = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'};

        final ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(connect);
        channel.writeInbound(buf);

        assertTrue(channel.isOpen());

        final ByteBuf buf2 = Unpooled.buffer();
        buf2.writeBytes(connect);
        channel.writeInbound(buf2);

        //verify that the client was disconnected
        assertFalse(channel.isOpen());

    }

    @Test
    public void test_connect_mqtt5_packet_size_too_large() {
        final byte[] mqtt5Connect = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'};

        test_connect_packet_size_too_large(mqtt5Connect);
    }

    @Test
    public void test_connect_mqtt3_1_1_packet_size_too_large() {
        final byte[] mqtt311Connect = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'};

        test_connect_packet_size_too_large(mqtt311Connect);
    }

    @Test
    public void test_connect_mqtt3_1_packet_size_too_large() {
        final byte[] mqtt31Connect = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                17,
                // variable header
                //   protocol name
                0, 6, 'M', 'Q', 'T', 'T',
                //   protocol version
                3, 1,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'};

        test_connect_packet_size_too_large(mqtt31Connect);
    }

    @Test
    public void test_publish_mqtt5_packet_size_too_large() {
        test_publish_packet_size_too_large(ProtocolVersion.MQTTv5);
    }

    @Test
    public void test_publish_mqtt3_1_1_packet_size_too_large() {
        test_publish_packet_size_too_large(ProtocolVersion.MQTTv3_1_1);
    }

    @Test
    public void test_publish_mqtt3_1_packet_size_too_large() {
        test_publish_packet_size_too_large(ProtocolVersion.MQTTv3_1);
    }

    private void test_publish_packet_size_too_large(final @NotNull ProtocolVersion protocolVersion) {
        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxPacketSize(10);
        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        clientConnection = new DummyClientConnection(channel, null);
        //setting version to fake "connected" state
        clientConnection.setProtocolVersion(protocolVersion);
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);

        final byte[] publish = {
                // fixed header
                //   type, flags
                0b0011_0000,
                //   remaining length
                22,
                // variable header
                //   topic name
                0, 5, 't', 'o', 'p', 'i', 'c',
                //   properties
                14,
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        final ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(publish);
        channel.writeInbound(buf);

        //verify that the client was disconnected
        assertFalse(channel.isOpen());
    }


    private void test_connect_packet_size_too_large(byte[] connect) {
        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxPacketSize(10);
        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        clientConnection = new DummyClientConnection(channel, null);
        clientConnection.setProtocolVersion(null);
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(connect);
        channel.writeInbound(buf);

        //verify that the client was not disconnected
        assertFalse(channel.isOpen());
    }

}
