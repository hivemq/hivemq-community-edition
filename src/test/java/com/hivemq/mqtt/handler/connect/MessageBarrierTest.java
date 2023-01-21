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
package com.hivemq.mqtt.handler.connect;

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
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
import org.mockito.MockitoAnnotations;
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

    private EmbeddedChannel channel;
    private MessageBarrier messageBarrier;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        final MqttServerDisconnector mqttServerDisconnector = new MqttServerDisconnectorImpl(new EventLog());

        messageBarrier = new MessageBarrier(mqttServerDisconnector);
        channel = new EmbeddedChannel(new DummyHandler());
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.pipeline().addFirst(MQTT_MESSAGE_BARRIER, messageBarrier);
    }

    @Test
    public void test_default() {
        assertEquals(false, messageBarrier.getConnectReceived());
    }

    @Test
    public void test_connect_sent() {

        channel.writeInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());
        assertEquals(true, messageBarrier.getConnectReceived());
    }

    @Test
    public void test_message_sent_before_connect() {

        channel.writeInbound(TestMessageUtil.createMqtt3Publish());
        assertEquals(false, channel.isActive());

    }

    @Test
    public void test_queue_messages_after_connect() {

        channel.writeInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());

        channel.writeInbound(TestMessageUtil.createMqtt3Publish());
        channel.writeInbound(new SUBSCRIBE(ImmutableList.of(), 1));
        channel.writeInbound(TestMessageUtil.createMqtt3Publish());
        channel.writeInbound(new PINGREQ());
        channel.writeInbound(TestMessageUtil.createMqtt3Publish());
        channel.writeInbound(new DISCONNECT());

        assertEquals(true, channel.isActive());
        assertEquals(6, messageBarrier.getQueue().size());
    }

    @Test
    public void test_messages_not_sent_on_connack_fail() {

        channel.writeInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());

        channel.writeInbound(TestMessageUtil.createMqtt3Publish());
        channel.writeInbound(new SUBSCRIBE(ImmutableList.of(), 1));

        assertEquals(2, messageBarrier.getQueue().size());

        final AtomicInteger counter = new AtomicInteger(0);

        channel.pipeline().addFirst(new ChannelDuplexHandler() {

            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                counter.incrementAndGet();
            }
        });

        channel.writeOutbound(ConnackMessages.REFUSED_NOT_AUTHORIZED);

        assertEquals(0, counter.get());
    }

    @Test
    public void test_messages_sent_on_connack_success() {

        channel.writeInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());

        channel.writeInbound(TestMessageUtil.createMqtt3Publish());
        channel.writeInbound(new SUBSCRIBE(ImmutableList.of(), 1));

        assertEquals(2, messageBarrier.getQueue().size());

        final AtomicInteger counter = new AtomicInteger(0);

        channel.pipeline().addAfter(MQTT_MESSAGE_BARRIER, "test", new ChannelDuplexHandler() {

            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) {

                if (msg instanceof PUBLISH || msg instanceof SUBSCRIBE) {
                    counter.incrementAndGet();
                }
            }
        });

        channel.writeOutbound(ConnackMessages.ACCEPTED_MSG_NO_SESS);

        assertEquals(2, counter.get());
        assertFalse(channel.pipeline().names().contains(MQTT_MESSAGE_BARRIER));
    }

}