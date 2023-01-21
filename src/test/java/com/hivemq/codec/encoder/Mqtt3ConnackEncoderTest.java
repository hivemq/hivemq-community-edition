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

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.encoder.TestMessageEncoder;

import static org.junit.Assert.*;

public class Mqtt3ConnackEncoderTest {

    private @NotNull EmbeddedChannel channel;
    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        channel = new EmbeddedChannel(new TestMessageEncoder());
        clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);
    }

    @Test
    public void test_mqtt311_connack_no_sp() {

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final CONNACK connack = new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED, false);
        channel.writeOutbound(connack);

        final ByteBuf buf = channel.readOutbound();

        //Fixed header
        assertEquals(0b0010_0000, buf.readByte());
        //Length
        assertEquals(0b0000_0010, buf.readByte());
        //Flags
        assertEquals(0b0000_0000, buf.readByte());
        //Accepted
        assertEquals(0b0000_0000, buf.readByte());

        //Nothing more to read
        assertFalse(buf.isReadable());

        //Let's make sure we weren't disconnected
        assertTrue(channel.isActive());
    }

    @Test
    public void test_mqtt311_connack_session_present() {

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);

        final CONNACK connack = new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED, true);
        channel.writeOutbound(connack);

        final ByteBuf buf = channel.readOutbound();

        //Fixed header
        assertEquals(0b0010_0000, buf.readByte());
        //Length
        assertEquals(0b0000_0010, buf.readByte());
        //Flags
        assertEquals(0b0000_0001, buf.readByte());
        //Accepted
        assertEquals(0b0000_0000, buf.readByte());

        //Nothing more to read
        assertFalse(buf.isReadable());

        //Let's make sure we weren't disconnected
        assertTrue(channel.isActive());
    }

    @Test
    public void test_mqtt31_connack() {

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);

        final CONNACK connack = new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED);
        channel.writeOutbound(connack);

        final ByteBuf buf = channel.readOutbound();

        //Fixed header
        assertEquals(0b0010_0000, buf.readByte());
        //Length
        assertEquals(0b0000_0010, buf.readByte());
        //Flags
        assertEquals(0b0000_0000, buf.readByte());
        //Accepted
        assertEquals(0b0000_0000, buf.readByte());

        //Nothing more to read
        assertFalse(buf.isReadable());

        //Let's make sure we weren't disconnected
        assertTrue(channel.isActive());
    }
}
