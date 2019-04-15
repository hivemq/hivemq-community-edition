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

package com.hivemq.mqtt.handler.publish.qos;

import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

public class QoSReceiverHandlerTest {

    public static final String CLIENT_ID = "client";
    @Mock
    private IncomingMessageFlowPersistence incomingMessageFlowPersistence;

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        embeddedChannel = new EmbeddedChannel(new QoSReceiverHandler(incomingMessageFlowPersistence));
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set(CLIENT_ID);
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

    private PUBLISH createPublish(final QoS qoS) {
        return TestMessageUtil.createMqtt3Publish(qoS);
    }
}