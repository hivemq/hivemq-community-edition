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

import com.hivemq.codec.encoder.mqtt3.Mqtt3DisconnectEncoder;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Mqtt3DisconnectEncoderTest {

    private EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {

        channel = new EmbeddedChannel(new Mqtt3DisconnectEncoder());
    }

    @Test
    public void test_disconnect_sent() throws Exception {
        channel.writeOutbound(new DISCONNECT());

        final ByteBuf buf = channel.readOutbound();

        final Mqtt3DisconnectEncoder mqtt3DisconnectEncoder = new Mqtt3DisconnectEncoder();
        assertEquals(mqtt3DisconnectEncoder.bufferSize(channel.pipeline().context(mqtt3DisconnectEncoder), new DISCONNECT()), buf.readableBytes());

        assertEquals(Mqtt3DisconnectEncoder.ENCODED_DISCONNECT_SIZE, buf.readableBytes());

        assertEquals((byte) 0b1110_0000, buf.readByte());
        assertEquals((byte) 0b0000_0000, buf.readByte());

        assertEquals(0, buf.readableBytes());

    }


}