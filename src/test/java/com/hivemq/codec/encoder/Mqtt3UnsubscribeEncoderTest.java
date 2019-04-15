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

import com.hivemq.codec.encoder.mqtt3.Mqtt3UnsubscribeEncoder;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class Mqtt3UnsubscribeEncoderTest {

    private EmbeddedChannel channel;
    final Mqtt3UnsubscribeEncoder mqtt3UnsubscribeEncoder = new Mqtt3UnsubscribeEncoder();

    @Before
    public void setUp() throws Exception {
        channel = new EmbeddedChannel(mqtt3UnsubscribeEncoder);
    }

    @Test
    public void test_one_topic() throws Exception {

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(Lists.newArrayList("topic"), 1);

        channel.writeOutbound(unsubscribe);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3UnsubscribeEncoder.bufferSize(channel.pipeline().context(mqtt3UnsubscribeEncoder), unsubscribe), buf.readableBytes());

        assertEquals((byte) 0b1010_0010, buf.readByte());      //header
        assertEquals(9, buf.readByte());                      //length
        assertEquals(1, buf.readShort());                      //message ID
        assertEquals("topic", Strings.getPrefixedString(buf)); //topic
    }

    @Test
    public void test_one_topic_utf_8() throws Exception {

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(Lists.newArrayList("topié"), 1);

        channel.writeOutbound(unsubscribe);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3UnsubscribeEncoder.bufferSize(channel.pipeline().context(mqtt3UnsubscribeEncoder), unsubscribe), buf.readableBytes());

        assertEquals((byte) 0b1010_0010, buf.readByte());      //header
        assertEquals(10, buf.readByte());                      //length
        assertEquals(10, buf.readableBytes());
        assertEquals(1, buf.readShort());                      //message ID
        assertEquals("topié", Strings.getPrefixedString(buf)); //topic
    }

    @Test
    public void test_many_topic() throws Exception {

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(Arrays.asList("topic1", "topic2", "topic3"), 1);

        channel.writeOutbound(unsubscribe);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3UnsubscribeEncoder.bufferSize(channel.pipeline().context(mqtt3UnsubscribeEncoder), unsubscribe), buf.readableBytes());

        assertEquals((byte) 0b1010_0010, buf.readByte());      //header
        assertEquals(26, buf.readByte());                      //length
        assertEquals(1, buf.readShort());                      //message ID
        assertEquals("topic1", Strings.getPrefixedString(buf)); //topic
        assertEquals("topic2", Strings.getPrefixedString(buf)); //topic
        assertEquals("topic3", Strings.getPrefixedString(buf)); //topic
    }

}