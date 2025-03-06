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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.puback.PUBACK;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.DummyClientConnection;
import util.TestMqttDecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Mqtt3PubackDecoderTest {

    private @NotNull EmbeddedChannel channel;
    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);

        channel = new EmbeddedChannel(TestMqttDecoder.create());
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(new DummyClientConnection(channel, null));
        ClientConnection.of(channel).setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
    }

    @After
    public void releaseMocks() throws Exception {
        closeable. close();
    }

    @Test
    public void test_puback_received() {


        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0100_0000);
        buf.writeByte(0b0000_0010);
        buf.writeShort(55555);
        channel.writeInbound(buf);

        final PUBACK puback = channel.readInbound();

        assertEquals(55555, puback.getPacketIdentifier());

        assertTrue(channel.isActive());
    }

    @Test
    public void test_puback_invalid_header_mqtt_311() {
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(new DummyClientConnection(channel, null));
        ClientConnection.of(channel).setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0100_0010);
        buf.writeByte(0b0000_0010);
        buf.writeShort(55555);
        channel.writeInbound(buf);


        //The client needs to get disconnected
        assertFalse(channel.isActive());
    }

    @Test
    public void test_puback_invalid_header_mqtt_31() {

        //In this test we check that additional headers are ignored in MQTT 3.1 if they're invalid
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(new DummyClientConnection(channel, null));
        ClientConnection.of(channel).setProtocolVersion(ProtocolVersion.MQTTv3_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0100_0010);
        buf.writeByte(0b0000_0010);
        buf.writeShort(55555);
        channel.writeInbound(buf);

        final PUBACK puback = channel.readInbound();

        assertEquals(55555, puback.getPacketIdentifier());

        assertTrue(channel.isActive());
    }

}
