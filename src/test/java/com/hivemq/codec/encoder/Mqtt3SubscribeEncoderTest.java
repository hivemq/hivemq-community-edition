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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hivemq.codec.encoder.mqtt3.Mqtt3SubscribeEncoder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Mqtt3SubscribeEncoderTest {

    private EmbeddedChannel channel;
    private final Mqtt3SubscribeEncoder mqtt3SubscribeEncoder = new Mqtt3SubscribeEncoder();

    @Before
    public void setUp() throws Exception {

        channel = new EmbeddedChannel(mqtt3SubscribeEncoder);
    }

    @Test
    public void test_one_topic() throws Exception {

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(new Topic("topic", QoS.AT_LEAST_ONCE))), 1);

        channel.writeOutbound(subscribe);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3SubscribeEncoder.bufferSize(channel.pipeline().context(mqtt3SubscribeEncoder), subscribe), buf.readableBytes());
        assertEquals((byte) 0b1000_0010, buf.readByte());      //header
        assertEquals(10, buf.readByte());                      //length
        assertEquals(1, buf.readShort());                      //message ID
        assertEquals("topic", Strings.getPrefixedString(buf)); //topic
        assertEquals(1, buf.readByte());                       //QoS
    }

    @Test
    public void test_one_topic_utf_8() throws Exception {

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(new Topic("topié", QoS.AT_LEAST_ONCE))), 1);

        channel.writeOutbound(subscribe);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3SubscribeEncoder.bufferSize(channel.pipeline().context(mqtt3SubscribeEncoder), subscribe), buf.readableBytes());
        assertEquals((byte) 0b1000_0010, buf.readByte());      //header
        assertEquals(11, buf.readByte());                      //length
        assertEquals(11, buf.readableBytes());
        assertEquals(1, buf.readShort());                      //message ID
        assertEquals("topié", Strings.getPrefixedString(buf)); //topic
        assertEquals(1, buf.readByte());                       //QoS
    }

    @Test
    public void test_many_topic() throws Exception {

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(new Topic("topic1", QoS.AT_LEAST_ONCE), new Topic("topic2", QoS.AT_MOST_ONCE), new Topic("topic3", QoS.EXACTLY_ONCE))), 1);

        channel.writeOutbound(subscribe);
        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3SubscribeEncoder.bufferSize(channel.pipeline().context(mqtt3SubscribeEncoder), subscribe), buf.readableBytes());

        assertEquals((byte) 0b1000_0010, buf.readByte());      //header
        assertEquals(29, buf.readByte());                      //length
        assertEquals(1, buf.readShort());                      //message ID
        assertEquals("topic1", Strings.getPrefixedString(buf)); //topic
        assertEquals(1, buf.readByte());                       //QoS
        assertEquals("topic2", Strings.getPrefixedString(buf)); //topic
        assertEquals(0, buf.readByte());                       //QoS
        assertEquals("topic3", Strings.getPrefixedString(buf)); //topic
        assertEquals(2, buf.readByte());                       //QoS
    }
}