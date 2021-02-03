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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extensions.handler.IncomingPublishHandler;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;
import util.TestMessageUtil;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

@SuppressWarnings("NullabilityAnnotations")
public class PublishFlowHandlerTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    public static final String CLIENT_ID = "client";

    @Mock
    private IncomingMessageFlowPersistence incomingMessageFlowPersistence;

    @Mock
    private PublishPollService publishPollService;

    @Mock
    private MessageIDPools messageIDPools;

    @Mock
    private MessageIDPool pool;

    @Mock
    private IncomingPublishHandler incomingPublishHandler;

    private OrderedTopicService orderedTopicService;

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE = 5;
        when(pool.takeNextId()).thenReturn(100);
        when(messageIDPools.forClientOrNull(anyString())).thenReturn(pool);
        orderedTopicService = new OrderedTopicService();
        embeddedChannel = new EmbeddedChannel(new PublishFlowHandler(publishPollService,
                incomingMessageFlowPersistence, orderedTopicService, messageIDPools, incomingPublishHandler,
                mock(DropOutgoingPublishesHandler.class)));
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set(CLIENT_ID);
    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE = 50;
    }

    @Test
    public void test_return_qos_1_message_id() throws Exception {

        final PUBACK puback = new PUBACK(pool.takeNextId());
        embeddedChannel.writeInbound(puback);

        verify(pool).returnId(eq(100));

    }

    @Test
    public void test_return_qos_2_message_id() throws Exception {

        final PUBCOMP pubcomp = new PUBCOMP(pool.takeNextId());
        embeddedChannel.writeInbound(pubcomp);

        verify(pool).returnId(eq(100));
    }

    @Test
    public void test_dont_return_message_id() throws Exception {

        final PUBREL pubrel = new PUBREL(pool.takeNextId());
        embeddedChannel.writeInbound(pubrel);

        verify(pool, never()).returnId(anyInt());
    }

    @Test
    public void test_dont_return_invalid_message_id() {

        final PUBACK puback = new PUBACK(-1);
        embeddedChannel.writeInbound(puback);

        verify(pool, never()).returnId(anyInt());
    }

    @Test
    public void test_qos_0_messages_not_acknowledged() {


        final PUBLISH publish = createPublish(QoS.AT_MOST_ONCE);
        embeddedChannel.writeInbound(publish);

        assertEquals(true, embeddedChannel.outboundMessages().isEmpty());
    }

    @Test
    public void test_qos_1_messages_not_acknowledged() {

        final PUBLISH publish = createPublish(QoS.AT_LEAST_ONCE);
        embeddedChannel.writeInbound(publish);

        assertEquals(true, embeddedChannel.outboundMessages().isEmpty());
    }


    @Test
    public void test_qos_1_messages_is_dup_not_forwarded() {

        final int messageid = 1;

        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder()
                .withTopic("topic")
                .withHivemqId("hivemqId")
                .withQoS(QoS.AT_LEAST_ONCE)
                .withPayload(new byte[100])
                .withDuplicateDelivery(true)
                .withPacketIdentifier(messageid)
                .build();

        when(incomingMessageFlowPersistence.get(CLIENT_ID, messageid)).thenReturn(null, publish);

        embeddedChannel.writeInbound(publish);

        //ack to remove from map
        final PUBREL pubrel = new PUBREL(messageid);
        embeddedChannel.writeInbound(pubrel);

        embeddedChannel.writeInbound(publish);

        //pubcomp is here
        assertEquals(1, embeddedChannel.outboundMessages().size());
    }

    @Test
    public void test_qos_1_messages_is_dup_ignored() {

        final int messageid = 1;

        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder()
                .withTopic("topic")
                .withHivemqId("hivemqId")
                .withQoS(QoS.AT_LEAST_ONCE)
                .withPayload(new byte[100])
                .withDuplicateDelivery(true)
                .withPacketIdentifier(messageid)
                .build();

        when(incomingMessageFlowPersistence.get(CLIENT_ID, messageid)).thenReturn(null, publish);

        embeddedChannel.writeInbound(publish);
        embeddedChannel.writeInbound(publish);

        assertEquals(true, embeddedChannel.outboundMessages().isEmpty());
    }

    @Test
    public void test_qos_1_messages_is_not_dup() {

        final int messageid = 1;

        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder()
                .withTopic("topic")
                .withHivemqId("hivemqId")
                .withQoS(QoS.AT_LEAST_ONCE)
                .withPayload(new byte[100])
                .withPacketIdentifier(messageid)
                .build();

        when(incomingMessageFlowPersistence.get(CLIENT_ID, messageid)).thenReturn(null, publish);

        embeddedChannel.writeInbound(publish);
        embeddedChannel.writeInbound(publish);

        assertEquals(true, embeddedChannel.outboundMessages().isEmpty());
        verify(incomingMessageFlowPersistence, times(2)).addOrReplace(CLIENT_ID, publish.getPacketIdentifier(), publish);
    }

    @Test
    public void test_qos_2_messages_not_acknowledged() {

        final PUBLISH publish = createPublish(QoS.EXACTLY_ONCE);
        embeddedChannel.writeInbound(publish);

        assertEquals(true, embeddedChannel.outboundMessages().isEmpty());
    }

    @Test
    public void test_qos_2_messages_is_dup_not_forwarded() {

        final int messageid = 1;

        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder()
                .withTopic("topic")
                .withHivemqId("hivemqId")
                .withQoS(QoS.EXACTLY_ONCE)
                .withPayload(new byte[100])
                .withDuplicateDelivery(true)
                .withPacketIdentifier(messageid)
                .build();

        when(incomingMessageFlowPersistence.get(CLIENT_ID, messageid)).thenReturn(null, publish);

        embeddedChannel.writeInbound(publish);

        //ack to remove from map
        final PUBREL pubrel = new PUBREL(messageid);
        embeddedChannel.writeInbound(pubrel);

        embeddedChannel.writeInbound(publish);

        //pubcomp is here
        assertEquals(1, embeddedChannel.outboundMessages().size());
    }

    @Test
    public void test_qos_2_messages_is_dup_ignored() {

        final int messageid = 1;

        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder()
                .withTopic("topic")
                .withHivemqId("hivemqId")
                .withQoS(QoS.EXACTLY_ONCE)
                .withPayload(new byte[100])
                .withDuplicateDelivery(true)
                .withPacketIdentifier(messageid)
                .build();

        when(incomingMessageFlowPersistence.get(CLIENT_ID, messageid)).thenReturn(null, publish);

        embeddedChannel.writeInbound(publish);
        embeddedChannel.writeInbound(publish);

        assertEquals(true, embeddedChannel.outboundMessages().isEmpty());
    }

    @Test
    public void test_qos_2_messages_is_not_dup() {

        final int messageid = 1;

        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder()
                .withTopic("topic")
                .withHivemqId("hivemqId")
                .withQoS(QoS.EXACTLY_ONCE)
                .withPayload(new byte[100])
                .withPacketIdentifier(messageid)
                .build();

        when(incomingMessageFlowPersistence.get(CLIENT_ID, messageid)).thenReturn(null, publish);

        embeddedChannel.writeInbound(publish);
        embeddedChannel.writeInbound(publish);

        assertEquals(true, embeddedChannel.outboundMessages().isEmpty());
        verify(incomingMessageFlowPersistence, times(2)).addOrReplace(CLIENT_ID, publish.getPacketIdentifier(), publish);
    }

    @Test
    public void test_acknowledge_qos_2_messages_with_pubcomp() {

        final PUBREL pubrel = new PUBREL(123);
        embeddedChannel.writeInbound(pubrel);

        assertEquals(false, embeddedChannel.outboundMessages().isEmpty());

        final PUBCOMP pubComp = embeddedChannel.readOutbound();

        assertNotNull(pubComp);
        assertEquals(pubrel.getPacketIdentifier(), pubComp.getPacketIdentifier());

        verify(incomingMessageFlowPersistence).addOrReplace(eq("client"), eq(pubrel.getPacketIdentifier()), same(pubrel));

        //We have to make sure that the client was actually deleted in the end
        verify(incomingMessageFlowPersistence).remove(eq("client"), eq(pubrel.getPacketIdentifier()));
    }

    @Test
    public void test_acknowledge_qos_1_message() {

        final PUBACK puback = new PUBACK(123);
        embeddedChannel.writeOutbound(puback);

        assertEquals(false, embeddedChannel.outboundMessages().isEmpty());

        final PUBACK pubackOut = embeddedChannel.readOutbound();

        assertNotNull(pubackOut);
        assertEquals(puback.getPacketIdentifier(), pubackOut.getPacketIdentifier());

        verify(incomingMessageFlowPersistence).addOrReplace(eq("client"), eq(puback.getPacketIdentifier()), same(puback));

        //We have to make sure that the client was actually deleted in the end
        verify(incomingMessageFlowPersistence).remove(eq("client"), eq(puback.getPacketIdentifier()));
    }

    @Test
    public void test_delete_everything_after_client_disconnects_on_clean_session() {
        embeddedChannel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT);

        embeddedChannel.finish();

        verify(incomingMessageFlowPersistence).delete(CLIENT_ID);
    }

    @Test
    public void test_dont_delete_anything_after_client_disconnects_on_persistent_session() {
        embeddedChannel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(500L);

        embeddedChannel.finish();

        verify(incomingMessageFlowPersistence, never()).delete(CLIENT_ID);
    }

    @Test
    public void test_puback_received() {

        final PUBACK puback = new PUBACK(123);
        embeddedChannel.writeInbound(puback);

        assertEquals(true, embeddedChannel.outboundMessages().isEmpty());
    }

    @Test
    public void test_pubrec_received_sending_back_pubrel() {

        when(publishPollService.putPubrelInQueue(anyString(), anyInt())).thenReturn(Futures.immediateFuture(null));

        final PUBREC pubrec = new PUBREC(123);
        embeddedChannel.writeInbound(pubrec);

        assertEquals(false, embeddedChannel.outboundMessages().isEmpty());

        final PUBREL pubrel = embeddedChannel.readOutbound();

        assertNotNull(pubrel);

        assertEquals(pubrec.getPacketIdentifier(), pubrel.getPacketIdentifier());

    }

    @Test
    public void test_pubcomp_received() {

        final PUBCOMP pubcomp = new PUBCOMP(123);
        embeddedChannel.writeInbound(pubcomp);

        assertEquals(true, embeddedChannel.outboundMessages().isEmpty());
    }

    @Test
    public void test_publish_sending() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withTopic("topic").withHivemqId("hivemqId").withQoS(QoS.AT_LEAST_ONCE).withPayload(new byte[100]).build();
        embeddedChannel.writeOutbound(publish);

        assertEquals(false, embeddedChannel.outboundMessages().isEmpty());

        final PUBLISH publishOut = embeddedChannel.readOutbound();

        assertNotNull(publishOut);

        assertEquals(publish.getPacketIdentifier(), publishOut.getPacketIdentifier());

    }


    @Test
    public void test_publish_sending_qos_0() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withTopic("topic").withHivemqId("hivemqId").withQoS(QoS.AT_MOST_ONCE).withPayload(new byte[100]).build();

        embeddedChannel.writeOutbound(publish);

        assertEquals(false, embeddedChannel.outboundMessages().isEmpty());

        final PUBLISH publishOut = embeddedChannel.readOutbound();

        assertNotNull(publishOut);

        assertEquals(publish.getPacketIdentifier(), publishOut.getPacketIdentifier());
    }

    @Test
    public void test_publish_with_future_not_shared_sending() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withTopic("topic").withHivemqId("hivemqId").withQoS(QoS.AT_LEAST_ONCE).withPayload(new byte[100]).build();

        final SettableFuture<PublishStatus> publishStatusSettableFuture = SettableFuture.create();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(publish, publishStatusSettableFuture, false);

        embeddedChannel.writeOutbound(publishWithFuture);

        assertEquals(false, embeddedChannel.outboundMessages().isEmpty());

        final PUBLISH publishOut = embeddedChannel.readOutbound();

        assertNotNull(publishOut);

        assertEquals(publish.getPacketIdentifier(), publishOut.getPacketIdentifier());
    }


    @Test
    public void test_pubrel_sending() {

        final PUBREL pubrel = new PUBREL(1, Mqtt5PubRelReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        embeddedChannel.writeOutbound(pubrel);

        assertEquals(false, embeddedChannel.outboundMessages().isEmpty());

        final PUBREL pubrelOut = embeddedChannel.readOutbound();

        assertNotNull(pubrelOut);

        assertEquals(pubrel.getPacketIdentifier(), pubrelOut.getPacketIdentifier());

    }

    @Test
    public void test_any_sending() {

        final MessageWithID messageWithID = new PUBACK(1);
        embeddedChannel.writeOutbound(messageWithID);

        assertEquals(false, embeddedChannel.outboundMessages().isEmpty());

        final MessageWithID messageOut = embeddedChannel.readOutbound();

        assertNotNull(messageOut);

        assertEquals(messageWithID.getPacketIdentifier(), messageOut.getPacketIdentifier());

    }

    @Test(timeout = 5000)
    public void test_qos1_release_next_message_on_next_puback() throws Exception {

        final PUBLISH publish = createPublish("topic", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = embeddedChannel.newPromise();
        final ChannelPromise promise2 = embeddedChannel.newPromise();
        final ChannelPromise promise3 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publish, promise1);
        embeddedChannel.writeAndFlush(publish2, promise2);
        embeddedChannel.writeAndFlush(publish3, promise3);

        embeddedChannel.pipeline().fireChannelRead(new PUBACK(1));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(2));

        promise1.await();
        promise2.await();
        promise3.await();

        assertEquals(0, orderedTopicService.queue.size());
        assertEquals(1, orderedTopicService.unacknowledgedMessages().size());
    }

    @Test(timeout = 5000)
    public void test_qos1_release_next_message_on_dropped() throws Exception {

        InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE = 1;

        final PUBLISH publish = createPublish("topic", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = embeddedChannel.newPromise();
        final ChannelPromise promise2 = embeddedChannel.newPromise();
        final ChannelPromise promise3 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publish, promise1);
        embeddedChannel.writeAndFlush(publish2, promise2);
        embeddedChannel.writeAndFlush(publish3, promise3);

        embeddedChannel.pipeline().fireUserEventTriggered(new PublishDroppedEvent(publish));
        embeddedChannel.pipeline().fireUserEventTriggered(new PublishDroppedEvent(publish2));

        promise1.await();
        promise2.await();
        promise3.await();
    }


    @Test(timeout = 5000)
    public void test_qos1_send_puback_queued_messages() throws Exception {


        final PUBLISH publish = createPublish("topic", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = embeddedChannel.newPromise();
        final ChannelPromise promise2 = embeddedChannel.newPromise();
        final ChannelPromise promise3 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publish, promise1);
        embeddedChannel.writeAndFlush(publish2, promise2);
        embeddedChannel.writeAndFlush(publish3, promise3);

        assertEquals(0, orderedTopicService.queue.size());
        assertEquals(3, orderedTopicService.unacknowledgedMessages().size());

        embeddedChannel.pipeline().fireChannelRead(new PUBACK(1));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(1));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(2));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(3));

        promise1.await();
        promise2.await();
        promise3.await();

        assertEquals(0, orderedTopicService.queue.size());
        assertEquals(0, orderedTopicService.unacknowledgedMessages().size());
    }

    @Test(timeout = 5000)
    public void test_qos1_send_puback_queued_messages_multiple_pubacks() throws Exception {

        embeddedChannel.attr(ChannelAttributes.CLIENT_RECEIVE_MAXIMUM).set(3);

        final PUBLISH publish = createPublish("topic", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.AT_LEAST_ONCE);
        final PUBLISH publish4 = createPublish("topic", 4, QoS.AT_LEAST_ONCE);
        final PUBLISH publish5 = createPublish("topic", 5, QoS.AT_LEAST_ONCE);
        final PUBLISH publish6 = createPublish("topic", 6, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = embeddedChannel.newPromise();
        final ChannelPromise promise2 = embeddedChannel.newPromise();
        final ChannelPromise promise3 = embeddedChannel.newPromise();
        final ChannelPromise promise4 = embeddedChannel.newPromise();
        final ChannelPromise promise5 = embeddedChannel.newPromise();
        final ChannelPromise promise6 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publish, promise1);
        embeddedChannel.writeAndFlush(publish2, promise2);
        embeddedChannel.writeAndFlush(publish3, promise3);
        embeddedChannel.writeAndFlush(publish4, promise4);
        embeddedChannel.writeAndFlush(publish5, promise5);
        embeddedChannel.writeAndFlush(publish6, promise6);

        assertEquals(3, orderedTopicService.queue.size());
        assertEquals(3, orderedTopicService.unacknowledgedMessages().size());

        embeddedChannel.pipeline().fireChannelRead(new PUBACK(1));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(1));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(2));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(3));

        embeddedChannel.pipeline().fireChannelRead(new PUBACK(4));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(4));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(5));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(5));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(6));
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(6));

        promise1.await();
        promise2.await();
        promise3.await();
        promise4.await();
        promise5.await();
        promise6.await();

        assertEquals(0, orderedTopicService.queue.size());
        assertEquals(0, orderedTopicService.unacknowledgedMessages().size());
    }

    @Test(timeout = 4_000)
    public void test_remove_messages() throws Exception {
        InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE = 1;

        final PUBLISH publish1 = createPublish("topic", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.AT_LEAST_ONCE);
        final PUBLISH publish4 = createPublish("topic", 4, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = embeddedChannel.newPromise();
        final ChannelPromise promise2 = embeddedChannel.newPromise();
        final ChannelPromise promise3 = embeddedChannel.newPromise();
        final ChannelPromise promise4 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publish1, promise1);
        embeddedChannel.writeAndFlush(publish2, promise2);
        embeddedChannel.writeAndFlush(publish3, promise3);
        embeddedChannel.writeAndFlush(publish4, promise4);

        promise1.await();

        assertEquals(3, orderedTopicService.queue.size());
        embeddedChannel.pipeline().fireChannelRead(new PUBACK(1));

        promise2.await();
        assertEquals(2, orderedTopicService.queue.size());

        embeddedChannel.pipeline().fireChannelRead(new PUBACK(2));
        promise3.await();

        embeddedChannel.pipeline().fireChannelRead(new PUBACK(3));
        promise4.await();


        assertTrue(orderedTopicService.queue.isEmpty());
    }

    @Test(timeout = 5000)
    public void test_qos2_release_next_message_on_next_pubcomp() throws Exception {

        InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE = 1;

        final PUBLISH publish = createPublish("topic", 1, QoS.EXACTLY_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.EXACTLY_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.EXACTLY_ONCE);

        final ChannelPromise promise1 = embeddedChannel.newPromise();
        final ChannelPromise promise2 = embeddedChannel.newPromise();
        final ChannelPromise promise3 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publish, promise1);
        embeddedChannel.writeAndFlush(publish2, promise2);
        embeddedChannel.writeAndFlush(publish3, promise3);

        embeddedChannel.pipeline().fireChannelRead(new PUBCOMP(1));
        embeddedChannel.pipeline().fireChannelRead(new PUBCOMP(2));

        promise1.await();
        promise2.await();
        promise3.await();

        assertEquals(0, orderedTopicService.queue.size());
        assertEquals(1, orderedTopicService.unacknowledgedMessages().size());
    }

    @Test(timeout = 5000)
    public void test_qos2_release_next_message_on_failed_pubrec() throws Exception {

        InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE = 1;

        final PUBLISH publish = createPublish("topic", 1, QoS.EXACTLY_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.EXACTLY_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.EXACTLY_ONCE);

        final ChannelPromise promise1 = embeddedChannel.newPromise();
        final ChannelPromise promise2 = embeddedChannel.newPromise();
        final ChannelPromise promise3 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publish, promise1);
        embeddedChannel.writeAndFlush(publish2, promise2);
        embeddedChannel.writeAndFlush(publish3, promise3);

        embeddedChannel.pipeline().fireChannelRead(new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR,
                null, Mqtt5UserProperties.NO_USER_PROPERTIES));
        embeddedChannel.pipeline().fireChannelRead(new PUBREC(2, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR,
                null, Mqtt5UserProperties.NO_USER_PROPERTIES));

        promise1.await();
        promise2.await();
        promise3.await();

        assertEquals(0, orderedTopicService.queue.size());
        assertEquals(1, orderedTopicService.unacknowledgedMessages().size());
    }

    @Test(timeout = 5000)
    public void test_qos1_return_publish_status_on_puback() throws Exception {

        final PUBLISH publish = createPublish("topic", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH internalPublish = TestMessageUtil.createMqtt3Publish("hivemqId", publish);
        final SettableFuture<PublishStatus> future = SettableFuture.create();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(internalPublish, future, true);

        final ChannelPromise promise1 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publishWithFuture, promise1);

        embeddedChannel.pipeline().fireChannelRead(new PUBACK(1));

        promise1.await();

        assertEquals(PublishStatus.DELIVERED, future.get());
    }

    @Test(timeout = 5000)
    public void test_qos2_return_publish_status_on_pubcomp() throws Exception {

        final PUBLISH publish = createPublish("topic", 1, QoS.EXACTLY_ONCE);
        final PUBLISH internalPublish = TestMessageUtil.createMqtt3Publish("hivemqId", publish);
        final SettableFuture<PublishStatus> future = SettableFuture.create();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(internalPublish, future, true);

        final ChannelPromise promise1 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publishWithFuture, promise1);

        embeddedChannel.pipeline().fireChannelRead(new PUBCOMP(1));

        promise1.await();

        assertEquals(PublishStatus.DELIVERED, future.get());
    }

    @Test(timeout = 5000)
    public void test_max_inflight_window() throws Exception {

        embeddedChannel.attr(ChannelAttributes.CLIENT_RECEIVE_MAXIMUM).set(50);
        InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE = 3;


        final PUBLISH publish = createPublish("topic", 1, QoS.EXACTLY_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.EXACTLY_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.EXACTLY_ONCE);
        final PUBLISH publish4 = createPublish("topic", 4, QoS.EXACTLY_ONCE);

        final ChannelPromise promise1 = embeddedChannel.newPromise();
        final ChannelPromise promise2 = embeddedChannel.newPromise();
        final ChannelPromise promise3 = embeddedChannel.newPromise();
        final ChannelPromise promise4 = embeddedChannel.newPromise();

        embeddedChannel.writeAndFlush(publish, promise1);
        embeddedChannel.writeAndFlush(publish2, promise2);
        embeddedChannel.writeAndFlush(publish3, promise3);
        embeddedChannel.writeAndFlush(publish4, promise4);

        assertEquals(1, orderedTopicService.queue.size());
        assertEquals(3, orderedTopicService.unacknowledgedMessages().size());
    }

    private PUBLISH createPublish(final String topic, final int messageId, final QoS qoS) {
        return createPublish(topic, messageId, qoS, false);
    }

    private PUBLISH createPublish(final String topic, final int messageId, final QoS qoS, final boolean dup) {

        return new PUBLISHFactory.Mqtt3Builder()
                .withHivemqId("hivemqId")
                .withMessageExpiryInterval(PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX)
                .withTopic(topic)
                .withQoS(qoS)
                .withPacketIdentifier(messageId)
                .withPayload("payload".getBytes())
                .withDuplicateDelivery(dup)
                .build();
    }

    private PUBLISH createPublish(final QoS qoS) {
        return TestMessageUtil.createMqtt3Publish(qoS);
    }
}