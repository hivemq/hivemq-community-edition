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
package com.hivemq.persistence.clientqueue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestSingleWriterFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("NullabilityAnnotations")
public class ClientQueuePersistenceImplTest {

    private AutoCloseable closeableMock;

    @Mock
    ClientQueueXodusLocalPersistence localPersistence;

    @Mock
    PublishPayloadPersistence payloadPersistence;

    @Mock
    MqttConfigurationService mqttConfigurationService;

    @Mock
    ClientSessionLocalPersistence clientSessionLocalPersistence;

    @Mock
    MessageDroppedService messageDroppedService;

    @Mock
    LocalTopicTree topicTree;

    @Mock
    private ConnectionPersistence connectionPersistence;
    @Mock
    private PublishPollService publishPollService;

    private ClientQueuePersistenceImpl clientQueuePersistence;

    final int bucketSize = 64;

    private SingleWriterService singleWriterService;

    @Before
    public void setUp() throws Exception {
        closeableMock = MockitoAnnotations.openMocks(this);
        singleWriterService = TestSingleWriterFactory.defaultSingleWriter();
        when(mqttConfigurationService.maxQueuedMessages()).thenReturn(1000L);
        when(mqttConfigurationService.getQueuedMessagesStrategy()).thenReturn(QueuedMessagesStrategy.DISCARD);
        clientQueuePersistence = new ClientQueuePersistenceImpl(localPersistence,
                singleWriterService,
                mqttConfigurationService,
                clientSessionLocalPersistence,
                messageDroppedService,
                topicTree,
                connectionPersistence,
                publishPollService);
    }

    @After
    public void tearDown() throws Exception {
        clientQueuePersistence.closeDB();
        singleWriterService.stop();
        closeableMock.close();
    }

    @Test(timeout = 5000)
    public void test_add() throws ExecutionException, InterruptedException {
        clientQueuePersistence.add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topic"), false, 1000L).get();
        verify(localPersistence).add(eq("client"),
                eq(false),
                any(PUBLISH.class),
                eq(1000L),
                eq(QueuedMessagesStrategy.DISCARD),
                anyBoolean(),
                anyInt());
        verify(messageDroppedService, never()).queueFull("client", "topic", 1);
    }

    @Test(timeout = 5000)
    public void test_add_shared() throws ExecutionException, InterruptedException {
        clientQueuePersistence.add("name/topic", true, createPublish(1, QoS.AT_LEAST_ONCE, "topic"), false, 1000L)
                .get();
        verify(localPersistence).add(eq("name/topic"),
                eq(true),
                any(PUBLISH.class),
                eq(1000L),
                eq(QueuedMessagesStrategy.DISCARD),
                anyBoolean(),
                anyInt());
    }

    @Test(timeout = 5000)
    public void test_publish_avaliable() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessagesSent(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessageCount(new AtomicInteger(0));

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(clientConnection);
        clientQueuePersistence.publishAvailable("client");
        channel.runPendingTasks();

        verify(publishPollService, timeout(2000)).pollNewMessages("client", channel);
    }

    @Test(timeout = 5000)
    public void test_publish_avaliable_channel_inactive() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessagesSent(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessageCount(new AtomicInteger(0));

        channel.close();

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(clientConnection);
        clientQueuePersistence.publishAvailable("client");
        channel.runPendingTasks();
        verify(publishPollService, never()).pollNewMessages("client", channel);
    }

    @Test(timeout = 5000)
    public void test_publish_avaliable_inflight_messages_not_sent() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessageCount(new AtomicInteger(0));

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(clientConnection);

        clientQueuePersistence.publishAvailable("client");
        channel.runPendingTasks();
        verify(publishPollService, never()).pollNewMessages("client", channel);
    }

    @Test(timeout = 5000)
    public void test_publish_avaliable_inflight_messages_sending() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessagesSent(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessageCount(new AtomicInteger(10));

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(clientConnection);

        clientQueuePersistence.publishAvailable("client");
        channel.runPendingTasks();
        verify(publishPollService, never()).pollNewMessages("client", channel);
    }

    @Test(timeout = 5000)
    public void test_publish_avaliable_channel_null() {

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(null);
        clientQueuePersistence.publishAvailable("client");
        verify(publishPollService, never()).pollNewMessages(eq("client"), any(Channel.class));
    }

    @Test(timeout = 5000)
    public void test_publish_avaliable_not_connected() {
        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(false, 1000L));
        clientQueuePersistence.publishAvailable("client");
        verify(publishPollService, never()).pollNewMessages(eq("client"), any(Channel.class));
    }

    @Test(timeout = 5000)
    public void test_read_new() throws ExecutionException, InterruptedException {

        when(localPersistence.readNew(anyString(),
                anyBoolean(),
                any(ImmutableIntArray.class),
                anyLong(),
                anyInt())).thenReturn(ImmutableList.of(createPublish(1, QoS.AT_MOST_ONCE, "topic"),
                createPublish(2, QoS.AT_LEAST_ONCE, "topic")));

        final ImmutableList<PUBLISH> publishes =
                clientQueuePersistence.readNew("client", false, ImmutableIntArray.of(1, 2), 1000).get();

        assertEquals(2, publishes.size());

    }

    @Test(timeout = 5000)
    public void test_clear() throws ExecutionException, InterruptedException {

        clientQueuePersistence.clear("client", false).get();
        verify(localPersistence).clear("client", false, BucketUtils.getBucket("client", bucketSize));

    }

    @Test(timeout = 5000)
    public void test_read_inflight() throws ExecutionException, InterruptedException {
        when(localPersistence.readInflight(anyString(), anyBoolean(), anyInt(), anyLong(), anyInt())).thenReturn(
                ImmutableList.of(createPublish(1, QoS.AT_LEAST_ONCE, "topic")));
        final ImmutableList<MessageWithID> messages = clientQueuePersistence.readInflight("client", 10, 11).get();
        assertEquals(1, messages.size());
        verify(localPersistence).readInflight(eq("client"), eq(false), eq(11), eq(10L), anyInt());
    }

    @Test(timeout = 5000)
    public void test_clean_up() throws ExecutionException, InterruptedException {

        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of("group/topic"));
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenReturn(ImmutableSet.of());

        clientQueuePersistence.cleanUp(0).get();

        verify(topicTree).getSharedSubscriber(anyString(), anyString());
    }

    @Test(timeout = 50000)
    public void test_shared_publish_available() {
        clientQueuePersistence.sharedPublishAvailable("group/topic");
        verify(publishPollService).pollSharedPublishes("group/topic");
    }

    @Test(timeout = 5000)
    public void test_remove_all_qos0() throws ExecutionException, InterruptedException {
        clientQueuePersistence.removeAllQos0Messages("client", false).get();
        verify(localPersistence).removeAllQos0Messages(eq("client"), eq(false), anyInt());
    }

    @Test(timeout = 5000)
    public void test_batched_add_no_new_message() throws ExecutionException, InterruptedException {
        when(localPersistence.size(eq("client"), anyBoolean(), anyInt())).thenReturn(1);
        final ImmutableList<PUBLISH> publishes = ImmutableList.of(createPublish(1, QoS.AT_LEAST_ONCE, "topic1"),
                createPublish(2, QoS.AT_LEAST_ONCE, "topic2"));
        clientQueuePersistence.add("client", false, publishes, false, 1000L).get();
        verify(localPersistence).add(eq("client"),
                eq(false),
                eq(publishes),
                eq(1000L),
                eq(QueuedMessagesStrategy.DISCARD),
                anyBoolean(),
                anyInt());
        verify(clientSessionLocalPersistence,
                never()).getSession("client"); // Get session because new publishes are available
        verify(messageDroppedService, never()).queueFull("client", "topic", 1);
    }

    @Test(timeout = 5000)
    public void test_batched_add_new_message() throws ExecutionException, InterruptedException {
        when(localPersistence.size(eq("client"), anyBoolean(), anyInt())).thenReturn(0);
        final ImmutableList<PUBLISH> publishes = ImmutableList.of(createPublish(1, QoS.AT_LEAST_ONCE, "topic1"),
                createPublish(2, QoS.AT_LEAST_ONCE, "topic2"));
        clientQueuePersistence.add("client", false, publishes, false, 1000L).get();
        verify(localPersistence).add(eq("client"),
                eq(false),
                eq(publishes),
                eq(1000L),
                eq(QueuedMessagesStrategy.DISCARD),
                anyBoolean(),
                anyInt());
        verify(clientSessionLocalPersistence).getSession("client"); // Get session because new publishes are available
        verify(messageDroppedService, never()).queueFull("client", "topic", 1);
    }

    private PUBLISH createPublish(final int packetId, final QoS qos, final String topic) {
        return new PUBLISHFactory.Mqtt5Builder().withPacketIdentifier(packetId)
                .withQoS(qos)
                .withOnwardQos(qos)
                .withPublishId(1L)
                .withPayload("message".getBytes())
                .withTopic(topic)
                .withHivemqId("hivemqId")
                .withPersistence(payloadPersistence)
                .build();
    }
}
