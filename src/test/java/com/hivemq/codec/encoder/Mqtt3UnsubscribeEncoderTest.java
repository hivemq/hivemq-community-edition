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

package com.hivemq.codec.encoder;

import com.google.common.collect.ImmutableList;
import com.hivemq.codec.encoder.mqtt3.Mqtt3UnsubscribeEncoder;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Mqtt3UnsubscribeEncoderTest {

    private EmbeddedChannel channel;
    final Mqtt3UnsubscribeEncoder mqtt3UnsubscribeEncoder = new Mqtt3UnsubscribeEncoder();

    @Before
    public void setUp() throws Exception {
        channel = new EmbeddedChannel(mqtt3UnsubscribeEncoder);
    }

    @Test
    public void test_one_topic() {

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(ImmutableList.of("topic"), 1);

        channel.writeOutbound(unsubscribe);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3UnsubscribeEncoder.bufferSize(channel.pipeline().context(mqtt3UnsubscribeEncoder),
                unsubscribe), buf.readableBytes());

        assertEquals((byte) 0b1010_0010, buf.readByte());      //header
        assertEquals(9, buf.readByte());                      //length
        assertEquals(1, buf.readShort());                      //message ID
        assertEquals("topic", Strings.getPrefixedString(buf)); //topic
    }

    @Test
    public void test_one_topic_utf_8() {

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(ImmutableList.of("topié"), 1);

        channel.writeOutbound(unsubscribe);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3UnsubscribeEncoder.bufferSize(channel.pipeline().context(mqtt3UnsubscribeEncoder),
                unsubscribe), buf.readableBytes());

        assertEquals((byte) 0b1010_0010, buf.readByte());      //header
        assertEquals(10, buf.readByte());                      //length
        assertEquals(10, buf.readableBytes());
        assertEquals(1, buf.readShort());                      //message ID
        assertEquals("topié", Strings.getPrefixedString(buf)); //topic
    }

    @Test
    public void test_many_topic() {

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(ImmutableList.of("topic1", "topic2", "topic3"), 1);

        channel.writeOutbound(unsubscribe);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3UnsubscribeEncoder.bufferSize(channel.pipeline().context(mqtt3UnsubscribeEncoder),
                unsubscribe), buf.readableBytes());

        assertEquals((byte) 0b1010_0010, buf.readByte());      //header
        assertEquals(26, buf.readByte());                      //length
        assertEquals(1, buf.readShort());                      //message ID
        assertEquals("topic1", Strings.getPrefixedString(buf)); //topic
        assertEquals("topic2", Strings.getPrefixedString(buf)); //topic
        assertEquals("topic3", Strings.getPrefixedString(buf)); //topic
    }

}