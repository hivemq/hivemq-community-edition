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
package com.hivemq.mqtt.callback;

import com.google.common.util.concurrent.Futures;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;
import util.TestException;
import util.TestMessageUtil;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public class PublishStatusFutureCallbackTest {

    @Rule
    public InitFutureUtilsExecutorRule futureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    private PublishStatusFutureCallback publishStatusFutureCallback;

    @Mock
    private PublishPayloadPersistence payloadPersistence;

    @Mock
    private PublishPollService publishPollService;

    @Mock
    private MessageIDPool messageIDPool;

    private boolean sharedSubscription;

    private String queueId;

    private PUBLISH publish;

    private Channel channel;

    private String client;


    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        sharedSubscription = false;
        queueId = "queueId";
        publish = TestMessageUtil.createMqtt5Publish();
        channel = new EmbeddedChannel();
        client = "client";

        when(publishPollService.removeMessageFromSharedQueue(anyString(), anyString())).thenReturn(Futures.immediateFuture(null));
        when(publishPollService.removeInflightMarker(anyString(), anyString())).thenReturn(Futures.immediateFuture(null));
        when(publishPollService.removeMessageFromQueue(anyString(), anyInt())).thenReturn(Futures.immediateFuture(null));

        publishStatusFutureCallback = new PublishStatusFutureCallback(payloadPersistence, publishPollService, sharedSubscription, queueId, publish, messageIDPool, channel, client);
    }

    @Test
    public void test_on_success_in_progress() {

        publishStatusFutureCallback.onSuccess(PublishStatus.IN_PROGRESS);
        verify(payloadPersistence, never()).decrementReferenceCounter(anyLong());

    }

    @Test
    public void test_on_success_qos_0_new_messages_available() {

        publish = TestMessageUtil.getDefaultPublishBuilder(payloadPersistence).withQoS(QoS.AT_MOST_ONCE).build();
        publishStatusFutureCallback = new PublishStatusFutureCallback(payloadPersistence, publishPollService, sharedSubscription, queueId, publish, messageIDPool, channel, client);
        publishStatusFutureCallback.onSuccess(PublishStatus.DELIVERED);
        verify(payloadPersistence).decrementReferenceCounter(anyLong());
        verify(publishPollService).pollMessages(client, channel);

    }

    @Test
    public void test_on_success_qos_0_no_new_messages_available() {

        channel.attr(ChannelAttributes.IN_FLIGHT_MESSAGES).set(new AtomicInteger(1000));
        publish = TestMessageUtil.getDefaultPublishBuilder(payloadPersistence).withQoS(QoS.AT_MOST_ONCE).build();
        publishStatusFutureCallback = new PublishStatusFutureCallback(payloadPersistence, publishPollService, sharedSubscription, queueId, publish, messageIDPool, channel, client);
        publishStatusFutureCallback.onSuccess(PublishStatus.DELIVERED);
        verify(payloadPersistence).decrementReferenceCounter(anyLong());
        verify(publishPollService, never()).pollMessages(client, channel);

    }

    @Test
    public void test_on_success_qos_1_shared_delivered() {

        publish = TestMessageUtil.getDefaultPublishBuilder(payloadPersistence).withPersistence(payloadPersistence).build();
        sharedSubscription = true;
        publishStatusFutureCallback = new PublishStatusFutureCallback(payloadPersistence, publishPollService, sharedSubscription, queueId, publish, messageIDPool, channel, client);
        publishStatusFutureCallback.onSuccess(PublishStatus.DELIVERED);
        verify(payloadPersistence).decrementReferenceCounter(anyLong());
        verify(publishPollService).removeMessageFromSharedQueue(queueId, publish.getUniqueId());
        verify(publishPollService).pollMessages(client, channel);

    }

    @Test
    public void test_on_success_qos_1_not_shared_delivered() {

        publish = TestMessageUtil.getDefaultPublishBuilder(payloadPersistence).withPersistence(payloadPersistence).build();
        sharedSubscription = false;
        publishStatusFutureCallback = new PublishStatusFutureCallback(payloadPersistence, publishPollService, sharedSubscription, queueId, publish, messageIDPool, channel, client);
        publishStatusFutureCallback.onSuccess(PublishStatus.DELIVERED);
        verify(payloadPersistence).decrementReferenceCounter(anyLong());
        verify(publishPollService, never()).removeMessageFromSharedQueue(queueId, publish.getUniqueId());
        verify(publishPollService).removeMessageFromQueue(queueId, publish.getPacketIdentifier());
        verify(publishPollService).pollMessages(client, channel);

    }

    @Test
    public void test_on_success_qos_1_shared_not_connected() {

        publish = TestMessageUtil.getDefaultPublishBuilder(payloadPersistence).withPersistence(payloadPersistence).build();
        sharedSubscription = true;
        publishStatusFutureCallback = new PublishStatusFutureCallback(payloadPersistence, publishPollService, sharedSubscription, queueId, publish, messageIDPool, channel, client);
        publishStatusFutureCallback.onSuccess(PublishStatus.NOT_CONNECTED);
        verify(payloadPersistence).decrementReferenceCounter(anyLong());
        verify(publishPollService).removeInflightMarker(queueId, publish.getUniqueId());
        verify(publishPollService, never()).pollMessages(client, channel);

    }

    @Test
    public void test_on_success_qos_1_shared_failed() {

        publish = TestMessageUtil.getDefaultPublishBuilder(payloadPersistence).withPersistence(payloadPersistence).build();
        sharedSubscription = true;
        publishStatusFutureCallback = new PublishStatusFutureCallback(payloadPersistence, publishPollService, sharedSubscription, queueId, publish, messageIDPool, channel, client);
        publishStatusFutureCallback.onSuccess(PublishStatus.FAILED);
        verify(payloadPersistence).decrementReferenceCounter(anyLong());
        verify(publishPollService).removeInflightMarker(queueId, publish.getUniqueId());
        verify(publishPollService).pollMessages(client, channel);

    }

    @Test
    public void test_on_failure_cancelation() {

        publishStatusFutureCallback.onFailure(new CancellationException());
        verify(payloadPersistence, never()).decrementReferenceCounter(anyLong());

    }

    @Test
    public void test_on_failure_no_cancelation() {

        publish = TestMessageUtil.getDefaultPublishBuilder(payloadPersistence).withQoS(QoS.AT_MOST_ONCE).build();
        publishStatusFutureCallback = new PublishStatusFutureCallback(payloadPersistence, publishPollService, sharedSubscription, queueId, publish, messageIDPool, channel, client);
        publishStatusFutureCallback.onFailure(TestException.INSTANCE);
        verify(payloadPersistence).decrementReferenceCounter(anyLong());

    }
}