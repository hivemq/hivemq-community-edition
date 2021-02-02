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
package com.hivemq.mqtt.services;

import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;
import util.TestMessageUtil;
import util.TestSingleWriterFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 */
public class PublishDistributorImplTest {

    @Rule
    public @NotNull InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    @Mock
    private @NotNull PublishPayloadPersistence payloadPersistence;
    @Mock
    private @NotNull ClientQueuePersistence clientQueuePersistence;
    @Mock
    private @NotNull ClientSessionPersistence clientSessionPersistence;
    @Mock
    private @NotNull MqttConfigurationService mqttConfigurationService;

    private @NotNull PublishDistributorImpl publishDistributor;
    private @NotNull SingleWriterService singleWriterService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        singleWriterService = TestSingleWriterFactory.defaultSingleWriter();
        publishDistributor = new PublishDistributorImpl(payloadPersistence, clientQueuePersistence, clientSessionPersistence,
                singleWriterService, mqttConfigurationService);
    }

    @Test(timeout = 5000)
    public void test_not_connected() throws ExecutionException, InterruptedException {
        when(clientSessionPersistence.getSession("client", false)).thenReturn(new ClientSession(false, 1000L));

        final PublishStatus status = publishDistributor.sendMessageToSubscriber(createPublish(QoS.AT_LEAST_ONCE), "client",
                0, false, false, ImmutableIntArray.of(1)).get();

        assertEquals(PublishStatus.NOT_CONNECTED, status);

    }

    @Test(timeout = 5000)
    public void test_session_expired() throws ExecutionException, InterruptedException {
        when(clientSessionPersistence.getSession("client", false)).thenReturn(null);

        final PublishStatus status = publishDistributor.sendMessageToSubscriber(createPublish(QoS.AT_LEAST_ONCE), "client",
                0, false, false, ImmutableIntArray.of(1)).get();

        assertEquals(PublishStatus.NOT_CONNECTED, status);
    }

    @Test(timeout = 5000)
    public void test_success() throws ExecutionException, InterruptedException {
        when(clientSessionPersistence.getSession("client", false)).thenReturn(new ClientSession(true, 1000L));
        when(clientQueuePersistence.add(eq("client"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong())).thenReturn(Futures.immediateFuture(null));


        final PublishStatus status = publishDistributor.sendMessageToSubscriber(createPublish(QoS.AT_LEAST_ONCE), "client",
                0, false, false, ImmutableIntArray.of(1)).get();

        verify(clientQueuePersistence).add(eq("client"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong());
        assertEquals(PublishStatus.DELIVERED, status);
    }

    @Test(timeout = 5000)
    public void test_failed() throws ExecutionException, InterruptedException {
        when(clientSessionPersistence.getSession("client", false)).thenReturn(new ClientSession(true, 1000L));
        when(clientQueuePersistence.add(eq("client"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("test")));


        final PublishStatus status = publishDistributor.sendMessageToSubscriber(createPublish(QoS.AT_LEAST_ONCE), "client",
                0, false, false, ImmutableIntArray.of(1)).get();

        verify(clientQueuePersistence).add(eq("client"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong());
        assertEquals(PublishStatus.FAILED, status);
    }

    @Test(timeout = 5000)
    public void test_success_shared() throws ExecutionException, InterruptedException {
        when(clientQueuePersistence.add(eq("group/topic"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong())).thenReturn(Futures.immediateFuture(null));


        final PublishStatus status = publishDistributor.sendMessageToSubscriber(createPublish(QoS.AT_LEAST_ONCE), "group/topic",
                0, true, false, ImmutableIntArray.of(1)).get();

        verify(clientQueuePersistence).add(eq("group/topic"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong());
        assertEquals(PublishStatus.DELIVERED, status);
    }

    @Test
    public void test_distribute_to_non_shared() {
        when(clientSessionPersistence.getSession("client1", false)).thenReturn(new ClientSession(true, 1000L));
        when(clientSessionPersistence.getSession("client2", false)).thenReturn(new ClientSession(true, 1000L));
        when(clientQueuePersistence.add(eq("client1"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong())).thenReturn(Futures.immediateFuture(null));
        when(clientQueuePersistence.add(eq("client2"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong())).thenReturn(Futures.immediateFuture(null));

        final Map<String, SubscriberWithIdentifiers> subscribers = Map.of(
                "client1", new SubscriberWithIdentifiers("client1", 1, (byte) 0, null),
                "client2", new SubscriberWithIdentifiers("client2", 1, (byte) 0, null)
        );

        publishDistributor.distributeToNonSharedSubscribers(subscribers, TestMessageUtil.createMqtt5Publish(), MoreExecutors.newDirectExecutorService());

        verify(clientQueuePersistence).add(eq("client1"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong());
        verify(clientQueuePersistence).add(eq("client2"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong());
    }

    @Test
    public void test_distribute_to_shared_subs() {
        when(clientQueuePersistence.add(eq("name/topic1"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong())).thenReturn(Futures.immediateFuture(null));
        when(clientQueuePersistence.add(eq("name/topic2"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong())).thenReturn(Futures.immediateFuture(null));

        final Set<String> subscribers = Set.of("name/topic1", "name/topic2");

        publishDistributor.distributeToSharedSubscribers(subscribers, TestMessageUtil.createMqtt5Publish("topic"), MoreExecutors.newDirectExecutorService());

        verify(clientQueuePersistence).add(eq("name/topic1"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong());
        verify(clientQueuePersistence).add(eq("name/topic2"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong());
    }

    private PUBLISH createPublish(final @NotNull QoS qos) {
        return new PUBLISHFactory.Mqtt5Builder().withPacketIdentifier(0)
                .withQoS(qos)
                .withPayload("message".getBytes())
                .withTopic("topic")
                .withHivemqId("hivemqId")
                .build();
    }

}
