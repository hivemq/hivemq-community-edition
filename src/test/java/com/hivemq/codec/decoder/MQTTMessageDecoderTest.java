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

import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMqttDecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MQTTMessageDecoderTest {

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        embeddedChannel = new EmbeddedChannel(TestMqttDecoder.create());
        //setting version to fake connected
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

    }

    /* ***********************
     * Test invalid messages *
     * ***********************/


    @Test
    public void test_reserved_zero_received() {


        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0000_0000);
        buf.writeByte(0b0000_000);
        embeddedChannel.writeInbound(buf);

        assertNull(embeddedChannel.readInbound());

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_reserved_fifteen_received() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1111_0000);
        buf.writeByte(0b0000_000);
        embeddedChannel.writeInbound(buf);

        assertNull(embeddedChannel.readInbound());

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_connack_received() {

        //We must not receive CONNACK from clients because only servers must send CONNACKs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b0010_0000);
        buf.writeByte(0b0000_000);
        embeddedChannel.writeInbound(buf);

        assertNull(embeddedChannel.readInbound());

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_suback_received() {

        //We must not receive a SUBACK from clients because only servers must send SUBACKs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1001_0000);
        buf.writeByte(0b0000_000);
        embeddedChannel.writeInbound(buf);

        assertNull(embeddedChannel.readInbound());

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_unsuback_received() {

        //We must not receive a UNSUBACK from clients because only servers must send UNSUBACKs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1011_0000);
        buf.writeByte(0b0000_000);
        embeddedChannel.writeInbound(buf);

        assertNull(embeddedChannel.readInbound());

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_pingresp_received_received() {

        //We must not receive a PINGRESP from clients because only servers must send PINGRESPs
        final ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0b1101_0000);
        buf.writeByte(0b0000_000);
        embeddedChannel.writeInbound(buf);

        assertNull(embeddedChannel.readInbound());

        assertEquals(false, embeddedChannel.isActive());
    }

    @Test
    public void test_second_connect_received() {

        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(null);

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
                0, 4, 't', 'e', 's', 't'
        };

        final ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(connect);
        embeddedChannel.writeInbound(buf);

        assertEquals(true, embeddedChannel.isOpen());

        final ByteBuf buf2 = Unpooled.buffer();
        buf2.writeBytes(connect);
        embeddedChannel.writeInbound(buf2);

        //verify that the client was disconnected
        assertEquals(false, embeddedChannel.isOpen());

    }
}