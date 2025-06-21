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
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.DummyClientConnection;
import util.TestConfigurationBootstrap;
import util.TestMqttDecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
    public void decode_whenReceivesReservedZero_thenConnectionIsClosed() {

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0000_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void decode_whenReceivesReservedFifteen_thenConnectionIsClosed() {

        ClientConnection.of(channel).setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1111_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void decode_whenReceivesCONNACK_thenConnectionIsClosed() {

        //We must not receive CONNACK from clients because only servers must send CONNACKs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0010_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void decode_whenReceivesSUBACK_thenConnectionIsClosed() {

        //We must not receive a SUBACK from clients because only servers must send SUBACKs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1001_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void decode_whenReceivesUNSUBACK_thenConnectionIsClosed() {

        //We must not receive a UNSUBACK from clients because only servers must send UNSUBACKs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1011_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void decode_whenReceivesPINGRESP_thenConnectionIsClosed() {

        //We must not receive a PINGRESP from clients because only servers must send PINGRESPs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1101_0000);
        buf.writeByte(0b0000_000);
        channel.writeInbound(buf);

        assertNull(channel.readInbound());

        assertFalse(channel.isActive());
    }

    @Test
    public void decode_whenReceivesSecondCONNECT_thenConnectionIsClosed() {

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
    public void decode_whenReceivesMqtt5CONNECTTooLarge_thenConnectionIsClosedAndCONNACKIsReceived() {
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

        testConnectPacketSizeTooLarge(mqtt5Connect);

        //verify that the client was disconnected
        assertFalse(channel.isOpen());

        //verify that the client received the proper CONNACK
        final CONNACK connack = channel.readOutbound();
        assertEquals(Mqtt5ConnAckReasonCode.PACKET_TOO_LARGE, connack.getReasonCode());
        assertEquals(ReasonStrings.CONNACK_PACKET_TOO_LARGE, connack.getReasonString());
    }

    @Test
    public void decode_whenReceives311CONNECTTooLarge_thenConnectionIsClosedAndCONNACKIsReceived() {
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
                4,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'};

        testConnectPacketSizeTooLarge(mqtt311Connect);

        //verify that the client was disconnected
        assertFalse(channel.isOpen());

        //verify that the client received the proper CONNACK
        final CONNACK connack = channel.readOutbound();
        assertEquals(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, connack.getReasonCode());
        assertNull(connack.getReasonString());
    }

    @Test
    public void decode_whenReceives31CONNECTTooLarge_thenConnectionIsClosedAndCONNACKIsReceived() {
        final byte[] mqtt31Connect = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                19,
                // variable header
                //   protocol name
                0, 6, 'M', 'Q', 'I', 's', 'd', 'p',
                //   protocol version
                4,
                //   connect flags
                (byte) 0b0000_0000,
                //   keep alive
                0, 0,
                //   properties
                0,
                // payload
                //   client identifier
                0, 4, 't', 'e', 's', 't'};

        testConnectPacketSizeTooLarge(mqtt31Connect);

        //verify that the client was disconnected
        assertFalse(channel.isOpen());

        //verify that the client received the proper CONNACK
        final CONNACK connack = channel.readOutbound();
        assertEquals(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, connack.getReasonCode());
        assertNull(connack.getReasonString());
    }

    @Test
    public void decode_whenReceivesMqtt5PUBLISHTooLarge_thenConnectionIsClosed() {
        testPublishPacketSizeTooLarge(ProtocolVersion.MQTTv5);
    }

    @Test
    public void decode_whenReceivesMqtt311PUBLISHTooLarge_thenConnectionIsClosed() {
        testPublishPacketSizeTooLarge(ProtocolVersion.MQTTv3_1_1);
    }

    @Test
    public void decode_whenReceivesMqtt31PUBLISHTooLarge_thenConnectionIsClosed() {
        testPublishPacketSizeTooLarge(ProtocolVersion.MQTTv3_1);
    }

    @Test
    public void decode_whenReceivesPartialMqtt5CONNECTTooLarge_nextMessageIsReadThenConnectionIsClosedAndCONNACKIsReceived() {
        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxPacketSize(15);
        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        clientConnection = new DummyClientConnection(channel, null);
        clientConnection.setProtocolVersion(null);
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);

        final ByteBuf buf1 = Unpooled.buffer();
        final ByteBuf buf2 = Unpooled.buffer();
        final byte[] mqtt5ConnectPart1 = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                15};
        final byte[] mqtt5ConnectPart2 = {
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                5};
        buf1.writeBytes(mqtt5ConnectPart1);
        buf2.writeBytes(mqtt5ConnectPart2);
        channel.writeInbound(buf1);
        CONNACK connack = channel.readOutbound();
        assertNull(connack);

        channel.writeInbound(buf2);
        connack = channel.readOutbound();
        assertNotNull(connack);

        //verify that the client was disconnected and it received the proper CONNACK
        assertFalse(channel.isOpen());
        assertEquals(Mqtt5ConnAckReasonCode.PACKET_TOO_LARGE, connack.getReasonCode());
        assertEquals(ReasonStrings.CONNACK_PACKET_TOO_LARGE, connack.getReasonString());
    }

    @Test
    public void decode_whenReceivesPartialMqtt31CONNECTTooLarge_nextMessageIsReadThenConnectionIsClosedAndCONNACKIsReceived() {
        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxPacketSize(15);
        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        clientConnection = new DummyClientConnection(channel, null);
        clientConnection.setProtocolVersion(null);
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);

        final ByteBuf buf1 = Unpooled.buffer();
        final ByteBuf buf2 = Unpooled.buffer();
        final byte[] mqtt31ConnectPart1 = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                15};
        final byte[] mqtt31ConnectPart2 = {
                // variable header
                //   protocol name
                0, 6, 'M', 'Q', 'I', 's', 'd', 'p',
                //   protocol version
                4};
        buf1.writeBytes(mqtt31ConnectPart1);
        buf2.writeBytes(mqtt31ConnectPart2);
        channel.writeInbound(buf1);
        CONNACK connack = channel.readOutbound();
        assertNull(connack);

        channel.writeInbound(buf2);
        connack = channel.readOutbound();
        assertNotNull(connack);

        //verify that the client was disconnected and it received the proper CONNACK
        assertFalse(channel.isOpen());
        assertEquals(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, connack.getReasonCode());
    }

    @Test
    public void decode_whenReceivesPartialMqtt311CONNECTTooLarge_nextMessageIsReadThenConnectionIsClosedAndCONNACKIsReceived() {
        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxPacketSize(15);
        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        clientConnection = new DummyClientConnection(channel, null);
        clientConnection.setProtocolVersion(null);
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);

        final ByteBuf buf1 = Unpooled.buffer();
        final ByteBuf buf2 = Unpooled.buffer();
        final byte[] mqtt311ConnectPart1 = {
                // fixed header
                //   type, reserved
                0b0001_0000,
                // remaining length
                15};
        final byte[] mqtt311ConnectPart2 = {
                // variable header
                //   protocol name
                0, 4, 'M', 'Q', 'T', 'T',
                //   protocol version
                4};
        buf1.writeBytes(mqtt311ConnectPart1);
        buf2.writeBytes(mqtt311ConnectPart2);
        channel.writeInbound(buf1);
        CONNACK connack = channel.readOutbound();
        assertNull(connack);

        channel.writeInbound(buf2);
        connack = channel.readOutbound();
        assertNotNull(connack);

        //verify that the client was disconnected and it received the proper CONNACK
        assertFalse(channel.isOpen());
        assertEquals(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, connack.getReasonCode());
    }

    @Test
    public void decode_whenReceivesMinimumPUBLISHTooLarge_thenConnectionIsClosed_3_1() {
        final ProtocolVersion protocolVersion = ProtocolVersion.MQTTv3_1;

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxPacketSize(15);
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
                22};

        final ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(publish);
        channel.writeInbound(buf);

        //verify that the client was disconnected
        assertFalse(channel.isOpen());
    }

    @Test
    public void decode_whenReceivesMinimumPUBLISHTooLarge_thenConnectionIsClosed_3_1_1() {
        final ProtocolVersion protocolVersion = ProtocolVersion.MQTTv3_1_1;

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxPacketSize(15);
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
                22};

        final ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(publish);
        channel.writeInbound(buf);

        //verify that the client was disconnected
        assertFalse(channel.isOpen());
    }

    @Test
    public void decode_whenReceivesMinimumPUBLISHTooLarge_thenConnectionIsClosed_5() {
        final ProtocolVersion protocolVersion = ProtocolVersion.MQTTv5;

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxPacketSize(15);
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
                22};

        final ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(publish);
        channel.writeInbound(buf);

        //verify that the client was disconnected
        assertFalse(channel.isOpen());
    }

    private void testPublishPacketSizeTooLarge(final @NotNull ProtocolVersion protocolVersion) {
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

    private void testConnectPacketSizeTooLarge(byte[] connect) {
        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxPacketSize(10);
        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        clientConnection = new DummyClientConnection(channel, null);
        clientConnection.setProtocolVersion(null);
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(connect);
        channel.writeInbound(buf);
    }

}
