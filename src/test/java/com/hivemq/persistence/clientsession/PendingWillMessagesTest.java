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
package com.hivemq.persistence.clientsession;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
public class PendingWillMessagesTest {

    @Rule
    public InitFutureUtilsExecutorRule rule = new InitFutureUtilsExecutorRule();

    @Mock
    InternalPublishService publishService;

    @Mock
    ClientSessionPersistence clientSessionPersistence;

    @Mock
    ClientSessionLocalPersistence clientSessionLocalPersistence;

    ListeningScheduledExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());

    private PendingWillMessages pendingWillMessages;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        pendingWillMessages = new PendingWillMessages(publishService, executorService, clientSessionPersistence, clientSessionLocalPersistence);
    }

    @Test
    public void test_add() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES).withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE).withTopic("topic").withDelayInterval(5).build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        pendingWillMessages.addWill("client", clientSession);

        final PendingWillMessages.PendingWill pendingWill = pendingWillMessages.pendingWills.get("client");
        assertEquals(pendingWill.getDelayInterval(), 5);
    }

    @Test
    public void test_add_session_expiry() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES).withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE).withTopic("topic").withDelayInterval(10).build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 5, sessionWill, 123L);
        pendingWillMessages.addWill("client", clientSession);

        final PendingWillMessages.PendingWill pendingWill = pendingWillMessages.pendingWills.get("client");
        assertEquals(pendingWill.getDelayInterval(), 5);
    }

    @Test
    public void test_add_and_send() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES).withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE).withTopic("topic").withDelayInterval(0).build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        pendingWillMessages.addWill("client", clientSession);

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), eq("client"));
        verify(clientSessionPersistence).removeWill(eq("client"));
    }

    @Test
    public void test_add_and_expiry() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES).withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE).withTopic("topic").withDelayInterval(10).build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 0, sessionWill, 123L);
        pendingWillMessages.addWill("client", clientSession);

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), eq("client"));
        verify(clientSessionPersistence).removeWill(eq("client"));
    }

    @Test
    public void reset() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES).withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE).withTopic("topic").withDelayInterval(5).build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        pendingWillMessages.addWill("client", clientSession);

        when(clientSessionPersistence.pendingWills()).thenReturn(Futures.immediateFuture(ImmutableMap.of("client1",
                new PendingWillMessages.PendingWill(3, System.currentTimeMillis()))));

        pendingWillMessages.reset();

        assertEquals(1, pendingWillMessages.pendingWills.size());
        assertEquals(3, pendingWillMessages.pendingWills.get("client1").getDelayInterval());
    }

    @Test
    public void test_check_dont_send() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES).withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE).withTopic("topic").withDelayInterval(5).build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        pendingWillMessages.addWill("client", clientSession);

        final PendingWillMessages.CheckWillsTask checkWillsTask = pendingWillMessages.new CheckWillsTask();
        checkWillsTask.run();

        verify(publishService, never()).publish(any(PUBLISH.class), any(ExecutorService.class), anyString());

    }

    @Test
    public void test_check_send() {

        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt5Builder().withHivemqId("hivemqId")
                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES).withPayload("message".getBytes())
                .withQos(QoS.AT_MOST_ONCE).withTopic("topic").withDelayInterval(5).build();
        final ClientSessionWill sessionWill = new ClientSessionWill(mqttWillPublish, 1L);
        final ClientSession clientSession = new ClientSession(false, 10, sessionWill, 123L);
        when(clientSessionLocalPersistence.getSession("client", false)).thenReturn(clientSession);
        pendingWillMessages.pendingWills.put("client", new PendingWillMessages.PendingWill(3, System.currentTimeMillis() - 5000));

        final PendingWillMessages.CheckWillsTask checkWillsTask = pendingWillMessages.new CheckWillsTask();
        checkWillsTask.run();

        verify(publishService).publish(any(PUBLISH.class), any(ExecutorService.class), eq("client"));

    }
}