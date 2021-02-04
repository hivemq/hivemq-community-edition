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
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestMessageUtil;

import java.util.concurrent.atomic.AtomicInteger;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_SUBSCRIBE_MESSAGE_BARRIER;
import static com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode.fromCode;
import static org.junit.Assert.assertEquals;

public class SubscribeMessageBarrierTest {

    private EmbeddedChannel embeddedChannel;
    private SubscribeMessageBarrier subscribeMessageBarrier;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        subscribeMessageBarrier = new SubscribeMessageBarrier();
        embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addFirst(MQTT_SUBSCRIBE_MESSAGE_BARRIER, subscribeMessageBarrier);
    }

    @Test
    public void test_default() {
        assertEquals(false, embeddedChannel.config().isAutoRead());
    }

    @Test
    public void test_subscribe_sent() {

        embeddedChannel.writeInbound(new SUBSCRIBE(ImmutableList.of(), 1));
        assertEquals(false, embeddedChannel.config().isAutoRead());
        assertEquals(1, subscribeMessageBarrier.getQueue().size());
    }

    @Test
    public void test_queue_publishes() {

        embeddedChannel.writeInbound(new SUBSCRIBE(ImmutableList.of(), 1));
        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        embeddedChannel.writeInbound(new PUBACK(0));
        embeddedChannel.writeInbound(new DISCONNECT());

        assertEquals(false, embeddedChannel.config().isAutoRead());
        assertEquals(4, subscribeMessageBarrier.getQueue().size());
    }

    @Test
    public void test_messages_sent_queued_publishes() {

        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());

        assertEquals(2, subscribeMessageBarrier.getQueue().size());

        final AtomicInteger counter = new AtomicInteger(0);

        embeddedChannel.pipeline().addAfter(MQTT_SUBSCRIBE_MESSAGE_BARRIER, "inbound_handler", new SimpleChannelInboundHandler<PUBLISH>() {

            @Override
            protected void channelRead0(final ChannelHandlerContext ctx, final PUBLISH msg) {
                counter.incrementAndGet();
            }
        });

        embeddedChannel.writeOutbound(new SUBACK(1, fromCode(1)));

        assertEquals(2, counter.get());
    }

    @Test
    public void test_messages_sent_publishes_and_subscribe() {

        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());

        embeddedChannel.writeInbound(new SUBSCRIBE(ImmutableList.of(), 2));

        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());
        embeddedChannel.writeInbound(TestMessageUtil.createMqtt3Publish());

        assertEquals(5, subscribeMessageBarrier.getQueue().size());

        final AtomicInteger counter = new AtomicInteger(0);

        embeddedChannel.pipeline().addAfter(MQTT_SUBSCRIBE_MESSAGE_BARRIER, "inbound_handler", new SimpleChannelInboundHandler<PUBLISH>() {

            @Override
            protected void channelRead0(final ChannelHandlerContext ctx, final PUBLISH msg) {
                counter.incrementAndGet();
            }
        });

        embeddedChannel.writeOutbound(new SUBACK(1, fromCode(1)));

        assertEquals(2, subscribeMessageBarrier.getQueue().size());
        assertEquals(2, counter.get());

        embeddedChannel.writeOutbound(new SUBACK(2, fromCode(1)));

        assertEquals(4, counter.get());
        assertEquals(0, subscribeMessageBarrier.getQueue().size());
    }

}