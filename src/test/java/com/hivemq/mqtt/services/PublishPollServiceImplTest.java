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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Futures;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishFlowHandler;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.publish.PubrelWithFuture;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.SubscriptionFlags;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;
import util.TestMessageUtil;
import util.TestSingleWriterFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
public class PublishPollServiceImplTest {

    private @NotNull AutoCloseable closeableMock;

    @Rule
    public @NotNull InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    @Mock
    @NotNull
    MessageIDPool messageIDPool;

    @Mock
    @NotNull
    ClientQueuePersistence clientQueuePersistence;

    @Mock
    @NotNull
    ChannelPersistence channelPersistence;

    @Mock
    @NotNull
    PublishPayloadPersistence publishPayloadPersistence;

    @Mock
    @NotNull
    Channel channel;

    @Mock
    @NotNull
    ChannelFuture channelFuture;

    @Mock
    @NotNull
    ChannelPipeline pipeline;

    @Mock
    @NotNull
    MessageDroppedService messageDroppedService;

    @Mock
    @NotNull
    SharedSubscriptionService sharedSubscriptionService;

    @Mock
    @NotNull
    PublishFlushHandler publishFlushHandler;

    private @NotNull PublishPollService publishPollService;

    private @NotNull SingleWriterService singleWriterService;
    private ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        closeableMock = MockitoAnnotations.openMocks(this);
        when(channelPersistence.get(anyString())).thenReturn(channel);
        when(channel.pipeline()).thenReturn(pipeline);

        clientConnection = spy(new ClientConnection(channel, publishFlushHandler));
        when(clientConnection.getMessageIDPool()).thenReturn(messageIDPool);

        final Attribute<ClientConnection> clientConnectionAttribute = mock(Attribute.class);
        when(channel.attr(ChannelAttributes.CLIENT_CONNECTION)).thenReturn(clientConnectionAttribute);
        when(clientConnectionAttribute.get()).thenReturn(clientConnection);

        when(channel.writeAndFlush(any())).thenReturn(channelFuture);

        InternalConfigurations.PUBLISH_POLL_BATCH_SIZE = 50;
        InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE = 50;

        singleWriterService = TestSingleWriterFactory.defaultSingleWriter();

        publishPollService = new PublishPollServiceImpl(clientQueuePersistence, channelPersistence,
                publishPayloadPersistence, messageDroppedService, sharedSubscriptionService, singleWriterService);
    }

    @After
    public void tearDown() throws Exception {
        singleWriterService.stop();
        closeableMock.close();
    }

    @Test
    public void test_new_messages() throws NoMessageIdAvailableException {

        when(messageIDPool.takeNextId()).thenReturn(1);
        when(clientQueuePersistence.readNew(eq("client"), eq(false), any(ImmutableIntArray.class), anyLong())).thenReturn(Futures.immediateFuture(ImmutableList.of(createPublish(1), createPublish(1))));
        when(channel.isActive()).thenReturn(true);
        clientConnection.setInFlightMessages(new AtomicInteger(0));

        publishPollService.pollNewMessages("client");

        verify(messageIDPool, times(48)).returnId(anyInt());
        verify(publishFlushHandler, times(1)).sendPublishes(any(List.class));
    }


    @Test
    public void test_new_messages_inflight_batch_size() throws NoMessageIdAvailableException {

        InternalConfigurations.PUBLISH_POLL_BATCH_SIZE = 1;

        clientConnection.setClientReceiveMaximum(10);

        when(messageIDPool.takeNextId()).thenReturn(1);
        when(clientQueuePersistence.readNew(eq("client"), eq(false), any(ImmutableIntArray.class), anyLong())).thenReturn(Futures.immediateFuture(ImmutableList.of(createPublish(1))));
        when(channel.isActive()).thenReturn(true);
        clientConnection.setInFlightMessages(new AtomicInteger(0));
        clientConnection.setInFlightMessagesSent(true);


        publishPollService.pollNewMessages("client");

        verify(messageIDPool, times(9)).returnId(anyInt()); // 10 messages are polled because the client receive max is 10
        verify(publishFlushHandler, times(1)).sendPublishes(any(List.class));
    }

    @Test
    public void test_new_messages_channel_inactive() throws NoMessageIdAvailableException {

        when(messageIDPool.takeNextId()).thenReturn(1);
        when(clientQueuePersistence.readNew(eq("client"), eq(false), any(ImmutableIntArray.class), anyLong())).thenReturn(Futures.immediateFuture(ImmutableList.of(createPublish(1))));
        when(channel.isActive()).thenReturn(false);
        clientConnection.setInFlightMessages(new AtomicInteger(0));
        clientConnection.setInFlightMessagesSent(true);

        publishPollService.pollNewMessages("client");
        final ArgumentCaptor<List<PublishWithFuture>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(publishFlushHandler, times(1)).sendPublishes(argumentCaptor.capture());
        argumentCaptor.getValue().get(0).getFuture().set(PublishStatus.NOT_CONNECTED);
        verify(messageIDPool, times(50)).returnId(anyInt()); // The id must be returned
    }

    @Test
    public void test_inflight_messages() throws NoMessageIdAvailableException {
        when(messageIDPool.takeIfAvailable(1)).thenReturn(1);
        when(messageIDPool.takeIfAvailable(2)).thenReturn(2);
        when(clientQueuePersistence.readInflight(eq("client"), anyLong(), anyInt()))
                .thenReturn(Futures.immediateFuture(ImmutableList.of(createPublish(1), new PUBREL(2))));

        when(channel.isActive()).thenReturn(true);
        when(channel.newPromise()).thenReturn(mock(ChannelPromise.class));
        clientConnection.setInFlightMessages(new AtomicInteger(0));

        publishPollService.pollInflightMessages("client", channel);

        verify(messageIDPool, times(2)).takeIfAvailable(anyInt());
        verify(publishFlushHandler, times(1)).sendPublishes(any(List.class));
        verify(channel).writeAndFlush(any(PubrelWithFuture.class));
    }

    @Test
    public void test_inflight_messages_packet_id_not_available() throws NoMessageIdAvailableException {
        when(messageIDPool.takeIfAvailable(1)).thenReturn(2);
        when(clientQueuePersistence.readInflight(eq("client"), anyLong(), anyInt()))
                .thenReturn(Futures.immediateFuture(ImmutableList.of(createPublish(1))));

        when(channel.isActive()).thenReturn(true);
        clientConnection.setInFlightMessages(new AtomicInteger(0));

        publishPollService.pollInflightMessages("client", channel);

        verify(messageIDPool, times(1)).takeIfAvailable(anyInt());
        verify(publishFlushHandler, times(1)).sendPublishes(any(List.class));
        verify(messageIDPool).returnId(2);
    }

    @Test
    public void test_inflight_messages_empty() throws NoMessageIdAvailableException {
        clientConnection.setInFlightMessagesSent(true);

        when(clientQueuePersistence.readInflight(eq("client"), anyLong(), anyInt())).thenReturn(Futures.immediateFuture(ImmutableList.of()));
        publishPollService.pollInflightMessages("client", channel);

        verify(messageIDPool, never()).takeIfAvailable(anyInt());
    }

    @Test
    public void test_poll_shared_publishes() throws NoMessageIdAvailableException {
        final PublishFlowHandler pubflishFlowHandler = mock(PublishFlowHandler.class);
        final byte flags = SubscriptionFlags.getDefaultFlags(true, false, false);
        when(sharedSubscriptionService.getSharedSubscriber(anyString())).thenReturn(ImmutableSet.of(
                new SubscriberWithQoS("client1", 2, flags, 1),
                new SubscriberWithQoS("client2", 2, flags, 2)));
        when(channelPersistence.get("client1")).thenReturn(channel);
        when(channelPersistence.get("client2")).thenReturn(null);

        when(clientQueuePersistence.readShared(eq("group/topic"), anyInt(), anyLong())).thenReturn(Futures.immediateFuture(
                ImmutableList.of(createPublish(1), createPublish(1), TestMessageUtil.createMqtt3Publish(QoS.AT_MOST_ONCE))));

        when(messageIDPool.takeNextId()).thenReturn(2).thenReturn(3);
        when(channel.isActive()).thenReturn(true);
        final AtomicInteger inFlightCount = new AtomicInteger(0);
        clientConnection.setInFlightMessages(inFlightCount);
        clientConnection.setInFlightMessagesSent(true);

        when(pipeline.get(PublishFlowHandler.class)).thenReturn(pubflishFlowHandler);

        publishPollService.pollSharedPublishes("group/topic");

        final ArgumentCaptor<List<PublishWithFuture>> captor = ArgumentCaptor.forClass(List.class);
        verify(publishFlushHandler, times(1)).sendPublishes(captor.capture());
        verify(messageIDPool, times(2)).takeNextId();

        final List<PublishWithFuture> values = captor.getValue();
        assertEquals(2, values.get(0).getPacketIdentifier());
        assertEquals(QoS.AT_LEAST_ONCE, values.get(0).getQoS());
        assertEquals(1, values.get(0).getSubscriptionIdentifiers().get(0));

        assertEquals(3, values.get(1).getPacketIdentifier());
        assertEquals(QoS.AT_LEAST_ONCE, values.get(1).getQoS());
        assertEquals(1, values.get(1).getSubscriptionIdentifiers().get(0));
        assertEquals(3, inFlightCount.get());
    }

    @Test
    public void test_poll_shared_publishes_messages_in_flight() throws NoMessageIdAvailableException {
        final byte flags = SubscriptionFlags.getDefaultFlags(true, false, false);
        when(sharedSubscriptionService.getSharedSubscriber(anyString())).thenReturn(ImmutableSet.of(
                new SubscriberWithQoS("client1", 2, flags, 1)));
        when(channelPersistence.get("client1")).thenReturn(channel);

        when(messageIDPool.takeNextId()).thenReturn(2).thenReturn(3);
        when(channel.isActive()).thenReturn(true);
        clientConnection.setInFlightMessages(new AtomicInteger(1));
        clientConnection.setInFlightMessagesSent(true);

        publishPollService.pollSharedPublishes("group/topic");

        verify(clientQueuePersistence, never()).readShared(anyString(), anyInt(), anyLong());
    }

    @Test
    public void test_poll_shared_publishes_messages_qos0_in_flight() throws NoMessageIdAvailableException {
        final PublishFlowHandler pubflishFlowHandler = mock(PublishFlowHandler.class);
        final byte flags = SubscriptionFlags.getDefaultFlags(true, false, false);
        when(sharedSubscriptionService.getSharedSubscriber(anyString())).thenReturn(ImmutableSet.of(
                new SubscriberWithQoS("client1", 2, flags, 1)));
        when(channelPersistence.get("client1")).thenReturn(channel);

        when(messageIDPool.takeNextId()).thenReturn(2).thenReturn(3);
        when(channel.isActive()).thenReturn(true);

        when(pipeline.get(PublishFlowHandler.class)).thenReturn(pubflishFlowHandler);
        clientConnection.setInFlightMessages(new AtomicInteger(1));
        clientConnection.setInFlightMessagesSent(true);

        publishPollService.pollSharedPublishes("group/topic");

        verify(clientQueuePersistence, never()).readShared(anyString(), anyInt(), anyLong());
    }

    @Test
    public void test_remove_shared_qos0_downgrade() throws NoMessageIdAvailableException {
        final PublishFlowHandler pubflishFlowHandler = mock(PublishFlowHandler.class);

        when(channel.isActive()).thenReturn(true);
        clientConnection.setInFlightMessages(new AtomicInteger(0));
        clientConnection.setInFlightMessagesSent(true);
        when(pipeline.get(PublishFlowHandler.class)).thenReturn(pubflishFlowHandler);

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish(QoS.AT_LEAST_ONCE);
        when(clientQueuePersistence.readShared(eq("group/topic"), anyInt(), anyLong())).thenReturn(Futures.immediateFuture(
                ImmutableList.of(publish)));

        when(messageIDPool.takeNextId()).thenReturn(1);

        publishPollService.pollSharedPublishesForClient("client", "group/topic", 0, false, null, channel);

        // Poll and remove
        verify(clientQueuePersistence).removeShared("group/topic", publish.getUniqueId());
    }

    private PUBLISH createPublish(final int packetId) {
        return TestMessageUtil.createMqtt5Publish(packetId);
    }
}
