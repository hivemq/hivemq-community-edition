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
package com.hivemq.mqtt.handler.publish;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.event.PubrelDroppedEvent;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.LogbackCapturingAppender;
import util.TestMessageUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.1
 */
public class MessageExpiryHandlerTest {

    @Mock
    private ChannelHandlerContext ctx;

    private EmbeddedChannel channel;

    LogbackCapturingAppender logCapture;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final MessageExpiryHandler messageExpiryHandler = new MessageExpiryHandler();
        channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("ClientId");
        channel.pipeline().addLast(messageExpiryHandler);
        when(ctx.channel()).thenReturn(channel);
        logCapture = LogbackCapturingAppender.Factory.weaveInto(MessageExpiryHandler.log);
    }

    @After
    public void tearDown() throws Exception {
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void test_message_expired_qos_0() throws Exception {

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_MOST_ONCE);
        publish.setMessageExpiryInterval(1);

        Thread.sleep(2000);

        final CountDownLatch droppedEventFiredLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, @NotNull final Object evt)
                    throws Exception {
                if (evt instanceof PublishDroppedEvent) {
                    droppedEventFiredLatch.countDown();
                }
            }
        });
        channel.writeOutbound(ctx, publish, channel.newPromise());

        assertTrue(droppedEventFiredLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_expired_qos_1() throws Exception {

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);
        publish.setMessageExpiryInterval(1);

        Thread.sleep(2000);

        final CountDownLatch droppedEventFiredLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, @NotNull final Object evt)
                    throws Exception {
                if (evt instanceof PublishDroppedEvent) {
                    droppedEventFiredLatch.countDown();
                }
            }
        });
        channel.writeOutbound(ctx, publish, channel.newPromise());

        assertTrue(droppedEventFiredLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_expired_qos_2_not_dup() throws Exception {

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.EXACTLY_ONCE);
        publish.setMessageExpiryInterval(1);

        Thread.sleep(2000);

        final CountDownLatch droppedEventFiredLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, @NotNull final Object evt)
                    throws Exception {
                if (evt instanceof PublishDroppedEvent) {
                    droppedEventFiredLatch.countDown();
                }
            }
        });
        channel.writeOutbound(ctx, publish, channel.newPromise());

        assertTrue(droppedEventFiredLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_qos_2_dup() throws Exception {
        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.EXACTLY_ONCE);
        publish.setMessageExpiryInterval(1);
        publish.setDuplicateDelivery(true);

        Thread.sleep(2000);

        final CountDownLatch droppedEventFiredLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, @NotNull final Object evt)
                    throws Exception {
                if (evt instanceof PublishDroppedEvent) {
                    droppedEventFiredLatch.countDown();
                }
            }
        });
        channel.writeOutbound(ctx, publish, channel.newPromise());

        assertFalse(droppedEventFiredLatch.await(1, TimeUnit.SECONDS));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_expired_qos_2_dup() throws Exception {
        InternalConfigurations.EXPIRE_INFLIGHT_MESSAGES_ENABLED = true;
        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.EXACTLY_ONCE);
        publish.setMessageExpiryInterval(1);
        publish.setDuplicateDelivery(true);

        Thread.sleep(2000);

        final CountDownLatch droppedEventFiredLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, @NotNull final Object evt)
                    throws Exception {
                if (evt instanceof PublishDroppedEvent) {
                    droppedEventFiredLatch.countDown();
                }
            }
        });
        channel.writeOutbound(ctx, publish, channel.newPromise());

        assertTrue(droppedEventFiredLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_pubrel_expired() throws InterruptedException {
        InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS_ENABLED = true;

        final PUBREL pubrel = new PUBREL(1);
        pubrel.setMessageExpiryInterval(0L);
        pubrel.setPublishTimestamp(System.currentTimeMillis());
        final CountDownLatch droppedEventFiredLatch = new CountDownLatch(1);
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, @NotNull final Object evt)
                    throws Exception {
                if (evt instanceof PubrelDroppedEvent) {
                    droppedEventFiredLatch.countDown();
                }
            }
        });

        channel.writeOutbound(pubrel);
        assertTrue(droppedEventFiredLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0L, pubrel.getMessageExpiryInterval().longValue());

    }

    @Test
    public void test_pubrel_dont_expired() throws InterruptedException {
        InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS_ENABLED = false;
        final PUBREL pubrel = new PUBREL(1);
        pubrel.setMessageExpiryInterval(0L);
        pubrel.setPublishTimestamp(System.currentTimeMillis());
        final CountDownLatch droppedEventFiredLatch = new CountDownLatch(1);

        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, @NotNull final Object evt)
                    throws Exception {
                if (evt instanceof PubrelDroppedEvent) {
                    droppedEventFiredLatch.countDown();
                }
            }
        });

        channel.writeOutbound(pubrel);
        assertFalse(droppedEventFiredLatch.await(50, TimeUnit.MILLISECONDS));
        assertEquals(0L, pubrel.getMessageExpiryInterval().longValue());
    }
}