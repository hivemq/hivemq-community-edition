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
package com.hivemq.mqtt.handler.unsubscribe;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.netty.ChannelHandlerNames;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.DummyHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpoeck
 */
@SuppressWarnings("unchecked")
public class UnsubscribeHandlerTest {

    @Mock
    @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;

    @Mock
    @NotNull SharedSubscriptionService sharedSubscriptionService;

    private @NotNull UnsubscribeHandler unsubscribeHandler;

    private @NotNull EmbeddedChannel channel;
    private final @NotNull ClientConnection clientConnection = new ClientConnection(channel, null);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        unsubscribeHandler = new UnsubscribeHandler(clientSessionSubscriptionPersistence, sharedSubscriptionService);
        channel = new EmbeddedChannel(unsubscribeHandler);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.pipeline().addFirst(ChannelHandlerNames.MQTT_MESSAGE_ENCODER, new DummyHandler());
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("myTestClient");
        when(clientSessionSubscriptionPersistence.remove(anyString(), any(String.class))).thenReturn(Futures.immediateFuture(null));
        when(clientSessionSubscriptionPersistence.removeSubscriptions(anyString(), any(ImmutableSet.class))).thenReturn(Futures.<Void>immediateFuture(null));
    }

    @Test
    public void test_unsubscribe_single_and_acknowledge() {
        final String topic = "myTopic";
        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(Lists.newArrayList(topic), 10);

        channel.writeInbound(unsubscribe);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final UNSUBACK response = (UNSUBACK) objects.element();
        assertEquals(10, response.getPacketIdentifier());

        verify(clientSessionSubscriptionPersistence).remove("myTestClient", topic);

    }

    @Test
    public void test_unsubscribe_single_and_acknowledge_mqtt5() {

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        final String topic = "myTopic";
        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(Lists.newArrayList(topic), 10);

        channel.writeInbound(unsubscribe);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final UNSUBACK response = (UNSUBACK) objects.element();
        assertEquals(10, response.getPacketIdentifier());
        assertEquals(1, response.getReasonCodes().size());
        assertEquals(Mqtt5UnsubAckReasonCode.SUCCESS, response.getReasonCodes().get(0));

        verify(clientSessionSubscriptionPersistence).remove("myTestClient", topic);

    }

    @Test
    public void test_unsubscribe_single_and_acknowledge_error_mqtt5() {

        when(clientSessionSubscriptionPersistence.remove(anyString(), any(String.class))).thenReturn(Futures.immediateFailedFuture(new NullPointerException("something is missing")));

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        final String topic = "myTopic";
        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(Lists.newArrayList(topic), 10);

        channel.writeInbound(unsubscribe);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final UNSUBACK response = (UNSUBACK) objects.element();
        assertEquals(10, response.getPacketIdentifier());
        assertEquals(1, response.getReasonCodes().size());
        assertEquals(Mqtt5UnsubAckReasonCode.UNSPECIFIED_ERROR, response.getReasonCodes().get(0));

        verify(clientSessionSubscriptionPersistence).remove("myTestClient", topic);

    }

    @Test
    public void test_unsubscribe_batched_and_acknowledge() {

        final String topic1 = "myTopic1";
        final String topic2 = "myTopic2";
        final String topic3 = "myTopic3";
        final List<String> aList = newArrayList();
        aList.add(topic1);
        aList.add(topic2);
        aList.add(topic3);

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(aList, 10);

        channel.writeInbound(unsubscribe);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final UNSUBACK response = (UNSUBACK) objects.element();
        assertEquals(10, response.getPacketIdentifier());

        verify(clientSessionSubscriptionPersistence).removeSubscriptions(eq("myTestClient"), any(ImmutableSet.class));


    }

    @Test
    public void test_unsubscribe_batched_and_acknowledge_mqtt5() {

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        final String topic1 = "myTopic1";
        final String topic2 = "myTopic2";
        final String topic3 = "myTopic3";
        final List<String> aList = newArrayList();
        aList.add(topic1);
        aList.add(topic2);
        aList.add(topic3);

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(aList, 10);

        channel.writeInbound(unsubscribe);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final UNSUBACK response = (UNSUBACK) objects.element();
        assertEquals(10, response.getPacketIdentifier());
        assertEquals(3, response.getReasonCodes().size());
        assertEquals(Mqtt5UnsubAckReasonCode.SUCCESS, response.getReasonCodes().get(0));
        assertEquals(Mqtt5UnsubAckReasonCode.SUCCESS, response.getReasonCodes().get(1));
        assertEquals(Mqtt5UnsubAckReasonCode.SUCCESS, response.getReasonCodes().get(2));

        verify(clientSessionSubscriptionPersistence).removeSubscriptions(eq("myTestClient"), any(ImmutableSet.class));

    }

    @Test
    public void test_unsubscribe_batched_and_acknowledge_error_mqtt5() {

        when(clientSessionSubscriptionPersistence.removeSubscriptions(anyString(), any(ImmutableSet.class))).thenReturn(Futures.immediateFailedFuture(new NullPointerException("something is missing")));

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        final String topic1 = "myTopic1";
        final String topic2 = "myTopic2";
        final String topic3 = "myTopic3";
        final List<String> aList = newArrayList();
        aList.add(topic1);
        aList.add(topic2);
        aList.add(topic3);

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(aList, 10);

        channel.writeInbound(unsubscribe);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final UNSUBACK response = (UNSUBACK) objects.element();
        assertEquals(10, response.getPacketIdentifier());
        assertEquals(3, response.getReasonCodes().size());
        assertEquals(Mqtt5UnsubAckReasonCode.UNSPECIFIED_ERROR, response.getReasonCodes().get(0));
        assertEquals(Mqtt5UnsubAckReasonCode.UNSPECIFIED_ERROR, response.getReasonCodes().get(1));
        assertEquals(Mqtt5UnsubAckReasonCode.UNSPECIFIED_ERROR, response.getReasonCodes().get(2));

        verify(clientSessionSubscriptionPersistence).removeSubscriptions(eq("myTestClient"), any(ImmutableSet.class));

    }

    @Test
    public void single_topic_dont_batch() {

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(Collections.singletonList("topic1"), 1);
        assertFalse(unsubscribeHandler.batch(unsubscribe));
    }

    @Test
    public void batch() {

        final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(Arrays.asList("topic1", "topic2"), 1);
        assertTrue(unsubscribeHandler.batch(unsubscribe));
    }

    static class UnsubscribeEventFiredHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void userEventTriggered(final @NotNull ChannelHandlerContext ctx, final @NotNull Object evt) throws Exception {
            super.userEventTriggered(ctx, evt);
        }
    }
}