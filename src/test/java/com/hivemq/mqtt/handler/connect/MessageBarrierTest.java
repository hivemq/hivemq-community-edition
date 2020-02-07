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

package com.hivemq.mqtt.handler.connect;

import com.google.common.collect.ImmutableList;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.DummyHandler;
import util.TestMessageUtil;

import java.util.concurrent.atomic.AtomicInteger;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_MESSAGE_BARRIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Christoph Schäbel
 */
public class MessageBarrierTest {

    private EmbeddedChannel embeddedChannel;
    private MessageBarrier messageBarrier;

    @Before
    public void before() {
        messageBarrier = new MessageBarrier(new EventLog());
        embeddedChannel = new EmbeddedChannel(new DummyHandler());
        embeddedChannel.pipeline().addFirst(MQTT_MESSAGE_BARRIER, messageBarrier);
    }

    @Test
    public void test_default() {
        assertEquals(false, messageBarrier.getConnectReceived());
    }

    @Test
    public void test_connect_sent() {

        embeddedChannel.writeInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());
        assertEquals(true, messageBarrier.getConnectReceived());
    }

    @Test
    public void test_message_sent_before_connect() {

        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        assertEquals(false, embeddedChannel.isActive());

    }

    @Test
    public void test_queue_messages_after_connect() {

        embeddedChannel.writeInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());

        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        embeddedChannel.writeInbound(new SUBSCRIBE(ImmutableList.of(), 1));
        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        embeddedChannel.writeInbound(new PINGREQ());
        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        embeddedChannel.writeInbound(new DISCONNECT());

        assertEquals(true, embeddedChannel.isActive());
        assertEquals(6, messageBarrier.getQueue().size());
    }

    @Test
    public void test_messages_not_sent_on_connack_fail() {

        embeddedChannel.writeInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());

        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        embeddedChannel.writeInbound(new SUBSCRIBE(ImmutableList.of(), 1));

        assertEquals(2, messageBarrier.getQueue().size());

        final AtomicInteger counter = new AtomicInteger(0);

        embeddedChannel.pipeline().addFirst(new ChannelDuplexHandler() {

            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                counter.incrementAndGet();
            }
        });

        embeddedChannel.writeOutbound(ConnackMessages.REFUSED_NOT_AUTHORIZED);

        assertEquals(0, counter.get());
    }

    @Test
    public void test_messages_sent_on_connack_success() {

        embeddedChannel.writeInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());

        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        embeddedChannel.writeInbound(new SUBSCRIBE(ImmutableList.of(), 1));

        assertEquals(2, messageBarrier.getQueue().size());

        final AtomicInteger counter = new AtomicInteger(0);

        embeddedChannel.pipeline().addAfter(MQTT_MESSAGE_BARRIER, "test", new ChannelDuplexHandler() {

            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) {

                if (msg instanceof PUBLISH || msg instanceof SUBSCRIBE) {
                    counter.incrementAndGet();
                }
            }
        });

        embeddedChannel.writeOutbound(ConnackMessages.ACCEPTED_MSG_NO_SESS);

        assertEquals(2, counter.get());
        assertFalse(embeddedChannel.pipeline().names().contains(MQTT_MESSAGE_BARRIER));
    }

}