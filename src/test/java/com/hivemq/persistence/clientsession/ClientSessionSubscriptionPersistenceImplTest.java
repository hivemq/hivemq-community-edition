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

import com.google.common.collect.ImmutableSet;
import com.hivemq.extensions.iteration.Chunker;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;
import util.TestSingleWriterFactory;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyByte;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
@SuppressWarnings("NullabilityAnnotations")
public class ClientSessionSubscriptionPersistenceImplTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    @Mock
    private ClientSessionSubscriptionLocalPersistence localPersistence;

    @Mock
    private LocalTopicTree topicTree;

    @Mock
    private SharedSubscriptionService sharedSubscriptionService;

    @Mock
    private ChannelPersistence channelPersistence;

    @Mock
    private EventLog eventLog;

    @Mock
    private ClientSessionLocalPersistence clientSessionLocalPersistence;

    @Mock
    private PublishPollService publishPollService;

    private ClientSessionSubscriptionPersistenceImpl persistence;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(topicTree.addTopic(anyString(), any(Topic.class), anyByte(), anyString())).thenReturn(true);
        persistence = new ClientSessionSubscriptionPersistenceImpl(localPersistence, topicTree, sharedSubscriptionService, TestSingleWriterFactory.defaultSingleWriter(), channelPersistence, eventLog, clientSessionLocalPersistence, publishPollService, new Chunker(), mock(MqttServerDisconnector.class));
    }

    @Test(timeout = 60000)
    public void test_add_subscriptions() throws Exception {

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 360));

        final Topic topic1 = new Topic("topic1", QoS.AT_MOST_ONCE);
        final Topic topic2 = new Topic("topic2", QoS.AT_MOST_ONCE);
        persistence.addSubscriptions("client", ImmutableSet.of(topic1, topic2)).get();
        verify(topicTree, times(2)).addTopic(eq("client"), any(Topic.class), anyByte(), any());
        verify(localPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class), anyLong(), anyInt());
    }

    @Test(timeout = 60000)
    public void test_add_subscriptions_session_expired() throws Exception {
        when(clientSessionLocalPersistence.getSession("client")).thenReturn(null);

        final Topic topic1 = new Topic("topic1", QoS.AT_MOST_ONCE);
        final Topic topic2 = new Topic("topic2", QoS.AT_MOST_ONCE);
        persistence.addSubscriptions("client", ImmutableSet.of(topic1, topic2)).get();
        verify(topicTree, never()).addTopic(anyString(), any(Topic.class), anyByte(), anyString());
        verify(localPersistence, never()).addSubscriptions(eq("client"), any(ImmutableSet.class), anyLong(), anyInt());
    }

    @Test(timeout = 60000)
    public void test_add_subscriptions_shared() throws Exception {
        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 360));

        final Topic topic1 = new Topic("topic1", QoS.AT_MOST_ONCE);
        final Topic topic2 = new Topic("topic2", QoS.AT_MOST_ONCE);
        when(sharedSubscriptionService.checkForSharedSubscription("topic1")).thenReturn(new SharedSubscriptionServiceImpl.SharedSubscription("topic1", "group"));
        when(sharedSubscriptionService.checkForSharedSubscription("topic2")).thenReturn(new SharedSubscriptionServiceImpl.SharedSubscription("topic2", "group"));
        persistence.addSubscriptions("client", ImmutableSet.of(topic1, topic2)).get();
        verify(topicTree, times(2)).addTopic(eq("client"), any(Topic.class), anyByte(), anyString());
        verify(localPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class), anyLong(), anyInt());

    }

    @Test
    public void test_invalidate_caches_channel_null() {

        when(channelPersistence.get("client")).thenReturn(null);
        persistence.invalidateSharedSubscriptionCacheAndPoll("client", ImmutableSet.of());

        verify(publishPollService, never()).pollSharedPublishesForClient(anyString(), anyString(), anyInt(), anyInt(), any(Channel.class));

    }

    @Test
    public void test_invalidate_caches_channel_closed() {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.close();

        when(channelPersistence.get("client")).thenReturn(embeddedChannel);
        persistence.invalidateSharedSubscriptionCacheAndPoll("client", ImmutableSet.of());

        verify(publishPollService, never()).pollSharedPublishesForClient(anyString(), anyString(), anyInt(), anyInt(), any(Channel.class));

    }

    @Test
    public void test_invalidate_caches_empty_subs() {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();

        when(channelPersistence.get("client")).thenReturn(embeddedChannel);
        persistence.invalidateSharedSubscriptionCacheAndPoll("client", ImmutableSet.of());

        verify(publishPollService, never()).pollSharedPublishesForClient(anyString(), anyString(), anyInt(), anyInt(), any(Channel.class));

        embeddedChannel.close();

    }

    @Test
    public void test_invalidate_caches_success() {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();

        when(channelPersistence.get("client")).thenReturn(embeddedChannel);
        persistence.invalidateSharedSubscriptionCacheAndPoll("client", ImmutableSet.of(new Subscription(new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 2, "group")));

        verify(publishPollService).pollSharedPublishesForClient(anyString(), anyString(), anyInt(), any(), any(Channel.class));
        verify(sharedSubscriptionService).invalidateSharedSubscriberCache("group/topic");
        verify(sharedSubscriptionService).invalidateSharedSubscriptionCache("client");

        embeddedChannel.close();

    }

    @Test(timeout = 60000)
    public void test_remove_subscriptions_responsible() throws ExecutionException, InterruptedException {

        persistence.removeSubscriptions("client", ImmutableSet.of("topic1", "topic2")).get();
        verify(topicTree, times(2)).removeSubscriber(eq("client"), anyString(), any());
    }

    @Test(timeout = 60000)
    public void test_remove_shared_subscriptions() throws ExecutionException, InterruptedException {

        when(sharedSubscriptionService.checkForSharedSubscription("$share/group/topic1")).thenReturn(new SharedSubscriptionServiceImpl.SharedSubscription("topic1", "group"));
        when(sharedSubscriptionService.checkForSharedSubscription("$share/group/topic2")).thenReturn(new SharedSubscriptionServiceImpl.SharedSubscription("topic2", "group"));

        persistence.removeSubscriptions("client", ImmutableSet.of("$share/group/topic1", "$share/group/topic2")).get();

        verify(topicTree, times(2)).removeSubscriber(eq("client"), anyString(), anyString());
        verify(localPersistence).removeSubscriptions(eq("client"), eq(ImmutableSet.of("$share/group/topic1", "$share/group/topic2")), anyLong(), anyInt());
    }

    @Test(timeout = 60000)
    public void test_addSubscription() throws Exception {

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 350));

        final Topic topic = new Topic("topic/1", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.DO_NOT_SEND, 1);
        persistence.addSubscription("client", topic).get();

        verify(topicTree).addTopic(eq("client"), eq(topic), eq((byte) 12), eq(null));
        verify(localPersistence).addSubscription(eq("client"), eq(topic), anyLong(), anyInt());
    }

    @Test(timeout = 60000)
    public void test_getSubscriptions() throws Exception {

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 350));

        final Topic topic = new Topic("topic/1", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.DO_NOT_SEND, 1);

        when(localPersistence.getSubscriptions("client")).thenReturn(ImmutableSet.of(topic));

        final ImmutableSet<Topic> subscriptions = persistence.getSubscriptions("client");

        assertEquals(1, subscriptions.size());

    }

    @Test(timeout = 60000)
    public void test_addSubscription_no_session_found() throws Exception {

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(null);

        final Topic topic = new Topic("topic/1", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.DO_NOT_SEND, 1);
        final SubscriptionResult subscriptionResult = persistence.addSubscription("client", topic).get();
        assertNull(subscriptionResult);

        verify(topicTree, never()).addTopic(eq("client"), eq(topic), eq((byte) 12), eq(null));
        verify(localPersistence, never()).addSubscription(eq("client"), eq(topic), anyLong(), anyInt());
    }

    @Test(timeout = 60000)
    public void test_addSubscription_shared() throws Exception {

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 350));
        when(sharedSubscriptionService.checkForSharedSubscription(anyString())).thenReturn(new SharedSubscriptionServiceImpl.SharedSubscription("topic/1", "group1"));

        final Topic topicShared = new Topic("topic/1", QoS.AT_LEAST_ONCE);

        final Topic topic = new Topic("$share/group1/topic/1", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.DO_NOT_SEND, 1);
        persistence.addSubscription("client", topic).get();

        verify(topicTree).addTopic(eq("client"), eq(topicShared), eq((byte) 14), eq("group1"));
        verify(localPersistence).addSubscription(eq("client"), eq(topic), anyLong(), anyInt());
    }

    @Test
    public void test_get_shared_subscription() throws ExecutionException, InterruptedException {

        when(localPersistence.getSubscriptions("client")).thenReturn(ImmutableSet.of(new Topic("$share/group/topic", QoS.AT_LEAST_ONCE)));
        when(sharedSubscriptionService.checkForSharedSubscription("$share/group/topic")).thenReturn(new SharedSubscriptionServiceImpl.SharedSubscription("topic", "group"));

        final ImmutableSet<Topic> subscriptions = persistence.getSharedSubscriptions("client");
        assertEquals(1, subscriptions.size());

    }

    @Test(timeout = 60000)
    public void test_addSubscription_shared_qos2() throws Exception {

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 350));
        when(sharedSubscriptionService.checkForSharedSubscription(anyString())).thenReturn(
                new SharedSubscriptionServiceImpl.SharedSubscription("topic/1", "group1"));

        final Topic expectedTopicTree = new Topic("topic/1", QoS.AT_LEAST_ONCE);
        final Topic expectedLocal = new Topic("$share/group1/topic/1", QoS.AT_LEAST_ONCE);

        final Topic topic =
                new Topic("$share/group1/topic/1", QoS.EXACTLY_ONCE, true, true, Mqtt5RetainHandling.DO_NOT_SEND, 1);
        persistence.addSubscription("client", topic).get();
        verify(topicTree).addTopic(eq("client"), eq(expectedTopicTree), eq((byte) 14), eq("group1"));
        verify(localPersistence).addSubscription(eq("client"), eq(expectedLocal), anyLong(), anyInt());
    }
}