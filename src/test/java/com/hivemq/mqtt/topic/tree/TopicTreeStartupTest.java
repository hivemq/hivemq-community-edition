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
package com.hivemq.mqtt.topic.tree;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.SubscriptionFlags;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;

import java.util.Set;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dominik Obermaier
 */
public class TopicTreeStartupTest {

    @Rule
    public InitFutureUtilsExecutorRule executorRule = new InitFutureUtilsExecutorRule();

    @Mock
    ClientSessionPersistence clientSessionPersistence;

    @Mock
    ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;

    @Mock
    SharedSubscriptionService sharedSubscriptionService;


    private LocalTopicTree topicTree;
    private TopicTreeStartup topicTreeStartup;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));

        topicTreeStartup =
                new TopicTreeStartup(topicTree, clientSessionPersistence, clientSessionSubscriptionPersistence,
                        sharedSubscriptionService);
    }

    @Test
    public void test_populate_topic_tree() throws Exception {

        final ListenableFuture<Set<String>> future = Futures.immediateFuture(Sets.newHashSet("client1", "client2", "client3"));
        when(clientSessionPersistence.getAllClients()).thenReturn(future);

        when(clientSessionPersistence.getSession(anyString(), anyBoolean())).thenReturn(new ClientSession(false, SESSION_EXPIRY_MAX));

        when(clientSessionSubscriptionPersistence.getSubscriptions(eq("client1"))).thenReturn(ImmutableSet.of(new Topic("topic1", QoS.AT_LEAST_ONCE)));
        when(clientSessionSubscriptionPersistence.getSubscriptions(eq("client2"))).thenReturn(ImmutableSet.of(new Topic("topic1", QoS.AT_LEAST_ONCE), new Topic("topic2", QoS.EXACTLY_ONCE)));
        when(clientSessionSubscriptionPersistence.getSubscriptions(eq("client3"))).thenReturn(ImmutableSet.of(new Topic("topic3", QoS.AT_MOST_ONCE, true, true, Mqtt5RetainHandling.DO_NOT_SEND, null)));

        topicTreeStartup.postConstruct();

        final Set<SubscriberWithIdentifiers> subscribersForTopic1 = topicTree.getSubscribers("topic1");
        final Set<SubscriberWithIdentifiers> subscribersForTopic2 = topicTree.getSubscribers("topic2");
        final Set<SubscriberWithIdentifiers> subscribersForTopic3 = topicTree.getSubscribers("topic3");

        assertThat(subscribersForTopic1, hasItems(new SubscriberWithIdentifiers("client1", 1, (byte) 0, null, ImmutableList.of(), null),
                new SubscriberWithIdentifiers("client2", 1, (byte) 0, null, ImmutableList.of(), null)));
        assertThat(subscribersForTopic2, hasItems(new SubscriberWithIdentifiers("client2", 2, SubscriptionFlags.getDefaultFlags(false, false, false), null, ImmutableList.of(), null)));
        assertThat(subscribersForTopic3, hasItems(new SubscriberWithIdentifiers("client3", 0, SubscriptionFlags.getDefaultFlags(false, true, true), null, ImmutableList.of(), null)));
    }

    @Test
    public void test_remove_clean_session_subs() throws Exception {

        final ListenableFuture<Set<String>> future = Futures.immediateFuture(Sets.newHashSet("client1", "client2"));
        when(clientSessionPersistence.getAllClients()).thenReturn(future);

        when(clientSessionPersistence.getSession(anyString(), anyBoolean())).thenReturn(new ClientSession(false, 0));

        topicTreeStartup.postConstruct();

        verify(clientSessionSubscriptionPersistence).removeAllLocally("client1");
        verify(clientSessionSubscriptionPersistence).removeAllLocally("client2");

        final Set<SubscriberWithIdentifiers> subscribersForTopic1 = topicTree.getSubscribers("topic1");
        final Set<SubscriberWithIdentifiers> subscribersForTopic2 = topicTree.getSubscribers("topic2");

        assertTrue(subscribersForTopic1.isEmpty());
        assertTrue(subscribersForTopic2.isEmpty());
    }

}
