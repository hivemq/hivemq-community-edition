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
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutionException;

import static com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.splitTopicAndGroup;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Dominik Obermaier
 */
@SuppressWarnings("deprecation")
public class SharedSubscriptionServiceTest {

    @Mock
    LocalTopicTree topicTree;

    @Mock
    ClientSessionSubscriptionPersistence subscriptionPersistence;

    private SharedSubscriptionServiceImpl service;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InternalConfigurations.SHARED_SUBSCRIBER_CACHE_CONCURRENCY_LEVEL.set(1);
        InternalConfigurations.SHARED_SUBSCRIPTION_CACHE_CONCURRENCY_LEVEL.set(1);

        service = new SharedSubscriptionServiceImpl(topicTree, subscriptionPersistence);
    }


    @Test
    public void test_check_for_shared_subscription() {
        final String share = "$share";
        final String oldDelimiter = "/";
        final String group = "group";
        final String topic = "topic";
        final String subtopic = "/subtopic";

        SharedSubscriptionServiceImpl.SharedSubscription sharedSubscription;

        sharedSubscription = service.checkForSharedSubscription(share + oldDelimiter + group + oldDelimiter + topic + subtopic);
        assertNotNull(sharedSubscription);
        assertEquals(group, sharedSubscription.getShareName());
        assertEquals(topic + subtopic, sharedSubscription.getTopicFilter());

        sharedSubscription = service.checkForSharedSubscription(topic + subtopic);
        assertNull(sharedSubscription);
    }

    @Test
    public void test_createSubscription_shared() throws Exception {

        final Subscription subscription = service.createSubscription(new Topic("$share/group1/topic/1", QoS.AT_LEAST_ONCE,
                true, true, Mqtt5RetainHandling.DO_NOT_SEND, 1));

        assertEquals("topic/1", subscription.getTopic().getTopic());
        assertEquals(QoS.AT_LEAST_ONCE, subscription.getTopic().getQoS());
        assertTrue(subscription.getTopic().isNoLocal());
        assertTrue(subscription.getTopic().isRetainAsPublished());
        assertEquals(Mqtt5RetainHandling.DO_NOT_SEND, subscription.getTopic().getRetainHandling());
    }

    @Test
    public void test_createSubscription_non_shared() throws Exception {

        final Subscription subscription = service.createSubscription(new Topic("share/group1/topic/2", QoS.AT_LEAST_ONCE,
                true, true, Mqtt5RetainHandling.DO_NOT_SEND, 1));

        assertEquals("share/group1/topic/2", subscription.getTopic().getTopic());
        assertEquals(QoS.AT_LEAST_ONCE, subscription.getTopic().getQoS());
        assertTrue(subscription.getTopic().isNoLocal());
        assertTrue(subscription.getTopic().isRetainAsPublished());
        assertEquals(Mqtt5RetainHandling.DO_NOT_SEND, subscription.getTopic().getRetainHandling());
    }

    @Test
    public void test_split_shared_subscriptions() {
        final SharedSubscriptionServiceImpl.SharedSubscription sharedSubscription1 = splitTopicAndGroup("group/topic/a");
        assertEquals("group", sharedSubscription1.getShareName());
        assertEquals("topic/a", sharedSubscription1.getTopicFilter());

        final SharedSubscriptionServiceImpl.SharedSubscription sharedSubscription2 = splitTopicAndGroup("group/");
        assertEquals("group", sharedSubscription2.getShareName());
        assertEquals("", sharedSubscription2.getTopicFilter());

        final SharedSubscriptionServiceImpl.SharedSubscription sharedSubscription3 = splitTopicAndGroup("group//a");
        assertEquals("group", sharedSubscription3.getShareName());
        assertEquals("/a", sharedSubscription3.getTopicFilter());
    }

    @Test
    public void test_get_shared_subscriber() throws ExecutionException, InterruptedException {
        service.postConstruct();
        final ImmutableSet<SubscriberWithQoS> result1 = ImmutableSet.of();
        when(topicTree.getSharedSubscriber("group", "topic")).thenReturn(ImmutableSet.of());
        final ImmutableSet<SubscriberWithQoS> result2 = service.getSharedSubscriber("group/topic");
        final ImmutableSet<SubscriberWithQoS> result3 = service.getSharedSubscriber("group/topic");

        verify(topicTree, times(1)).getSharedSubscriber(anyString(), anyString());

        assertSame(result1, result2);
        assertSame(result1, result3);

        assertTrue(result3.isEmpty());
    }

    @Test
    public void test_get_shared_subscriptions() throws ExecutionException, InterruptedException {
        service.postConstruct();

        final ImmutableSet<Topic> topics1 = ImmutableSet.of();

        when(subscriptionPersistence.getSharedSubscriptions("client")).thenReturn(topics1);
        final ImmutableSet<Topic> topics2 = service.getSharedSubscriptions("client");
        final ImmutableSet<Topic> topics3 = service.getSharedSubscriptions("client");

        verify(subscriptionPersistence, times(1)).getSharedSubscriptions("client");
        assertSame(topics1, topics2);
        assertSame(topics1, topics3);

        assertTrue(topics3.isEmpty());
    }

    @Test
    public void test_remove_prefix() {
        assertEquals("group/topic", service.removePrefix("$share/group/topic"));
        assertEquals("topic/a", service.removePrefix("topic/a"));
    }
}