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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class QoSSenderHandlerTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    public static final String CLIENT_ID = "client";

    @Mock
    private PublishPollService publishPollService;

    private EmbeddedChannel embeddedChannel;

    private QoSSenderHandler qoSSenderHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        qoSSenderHandler = new QoSSenderHandler(publishPollService);

        embeddedChannel = new EmbeddedChannel(qoSSenderHandler);

        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set(CLIENT_ID);

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

}