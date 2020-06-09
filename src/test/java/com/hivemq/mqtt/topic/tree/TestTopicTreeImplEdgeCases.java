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
import com.google.common.collect.UnmodifiableIterator;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

/**
 * @author Dominik Obermaier
 */
public class TestTopicTreeImplEdgeCases {

    private TopicTreeImpl topicTree;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD.set(1);
        topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));

    }

    @Test
    public void test_get_empty_topic() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);

        assertEquals(0, topicTree.getSubscribers("").size());

        assertEquals(1, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void remove_before_index_map_creation() throws Exception {

        topicTree.addTopic("subscriber", new Topic("a/b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("a/c", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.removeSubscriber("subscriber", "a/c", null);
        topicTree.addTopic("subscriber", new Topic("a/d", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("a/e", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(0, topicTree.getSubscribers("a/c").size());

        assertEquals(3, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_remove_longer_topic() throws Exception {

        topicTree.addTopic("subscriber", new Topic("/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("/+/b/c", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("/+/c/d", QoS.AT_MOST_ONCE), (byte) 0, null);

        topicTree.removeSubscriber("subscriber", "/+", null);

        assertEquals(0, topicTree.getSubscribers("/a").size());

        assertEquals(2, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_leading_wild_card_edge_case() throws Exception {

        topicTree.addTopic("subscriber1", new Topic("+/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("a/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("+/test", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("a/test");
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_plus_wild_card_edge_case() throws Exception {

        topicTree.addTopic("subscriber1", new Topic("a/+/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("a/b/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("a/+/test", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("a/b/test");
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_plus_wild_card_ending_edge_case() throws Exception {

        topicTree.addTopic("subscriber1", new Topic("a/test/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("a/test/b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("a/test/+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("a/test/b");
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_edge_case_slash_topic_direct_match() throws Exception {

        topicTree.addTopic("subscriber", new Topic("/", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("/");
        assertEquals(false, subscribers.isEmpty());
        assertThat(subscribers, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));
    }

    @Test
    public void test_edge_case_slash_topic_wildcard_match() throws Exception {
        topicTree.addTopic("subscriber", new Topic("+/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        final Set<SubscriberWithIdentifiers> subscribers2 = topicTree.getSubscribers("/");

        assertEquals(false, subscribers2.isEmpty());
        assertThat(subscribers2, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));
    }

    @Test
    public void test_edge_case_only_slashes_direct_match() throws Exception {

        topicTree.addTopic("subscriber", new Topic("/////", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("/////");
        assertEquals(false, subscribers.isEmpty());
        assertThat(subscribers, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        final Set<SubscriberWithIdentifiers> subscribers2 = topicTree.getSubscribers("////");
        assertEquals(true, subscribers2.isEmpty());
    }

    @Test
    public void test_edge_case_only_slashes_wildcard_match() throws Exception {

        topicTree.addTopic("subscriber", new Topic("+/+/+/+/+/+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("/////");
        assertEquals(false, subscribers.isEmpty());
        assertThat(subscribers, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        topicTree.addTopic("subscriber2", new Topic("+/+/+/+/+/", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers2 = topicTree.getSubscribers("/////");
        assertEquals(false, subscribers2.isEmpty());
        assertThat(subscribers2, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        topicTree.addTopic("subscriber3", new Topic("/+/+/+/+/", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers3 = topicTree.getSubscribers("/////");
        assertEquals(false, subscribers3.isEmpty());
        assertThat(subscribers3, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        assertEquals(3, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_edge_case_matching_level_wildcard_at_end_null_string() throws Exception {
        //From https://groups.google.com/forum/#!topic/mqtt/LY7xkGKOJaU

        topicTree.addTopic("subscriber", new Topic("a/+/b", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("a//b");
        assertEquals(false, subscribers.isEmpty());
        assertThat(subscribers, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        topicTree.addTopic("subscriber2", new Topic("a/b/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        final Set<SubscriberWithIdentifiers> subscribers2 = topicTree.getSubscribers("a/b/");
        assertEquals(false, subscribers2.isEmpty());
        assertThat(subscribers2, hasItem(new SubscriberWithIdentifiers("subscriber2", 0, (byte) 0, null, ImmutableList.of(), null)));
    }

    @Test
    public void test_edge_case_more_than_1000_segments() throws Exception {
        String topic = "";
        for (int i = 0; i < 1000; i++) {
            topic += RandomStringUtils.randomAlphanumeric(1) + "/";
        }
        topic += "topic";

        topicTree.addTopic("subscriber", new Topic(topic, QoS.EXACTLY_ONCE), (byte) 0, null);

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers(topic);

        assertEquals(0, subscribers.size());
    }

    @Test
    public void test_add_same_wildcard_topic_twice() throws Exception {

        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.getSubscribers("any").size());

        assertEquals(1, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_add_same_wildcard_topic_twice_different_qos() throws Exception {

        topicTree.addTopic("client1", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("#", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.getSubscribers("any").size());

        assertEquals(1, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_add_same_wildcard_topic_twice_mulitple_subs() throws Exception {

        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(3, topicTree.getSubscribers("#").size());
        assertEquals(3, topicTree.subscriptionCounter.getCount());

        topicTree.removeSubscriber("client3", "#", null);

        assertEquals(2, topicTree.getSubscribers("#").size());
        assertEquals(2, topicTree.subscriptionCounter.getCount());

        topicTree.removeSubscriber("client2", "#", null);

        assertEquals(1, topicTree.getSubscribers("#").size());
        assertEquals(1, topicTree.subscriptionCounter.getCount());

        topicTree.addTopic("client3", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.getSubscribers("#").size());
        assertEquals(2, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_add_same_topic_twice() throws Exception {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.getSubscribers("a/b").size());

        assertEquals(1, topicTree.subscriptionCounter.getCount());
    }


    @Test
    public void test_add_same_topic_twice_different_qos() throws Exception {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/b", QoS.AT_LEAST_ONCE), (byte) 0, null);

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("a/b");
        assertEquals(1, subscribers.size());

        assertEquals(1, topicTree.subscriptionCounter.getCount());
        final SubscriberWithIdentifiers next = subscribers.iterator().next();

        assertEquals(1, next.getQos());
    }


    @Test
    public void test_add_same_topic_twice_different_qos_more_than_32_subscribers() throws Exception {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        for (int i = 0; i < 32; i++) {
            topicTree.addTopic("client" + (i + 2), new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        }

        topicTree.addTopic("client1", new Topic("a/b", QoS.AT_LEAST_ONCE), (byte) 0, null);

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("a/b");
        assertEquals(33, subscribers.size());

        assertEquals(33, topicTree.subscriptionCounter.getCount());

        final UnmodifiableIterator<SubscriberWithIdentifiers> subscribersIterator = subscribers.iterator();
        boolean found = false;
        while (subscribersIterator.hasNext()) {
            final SubscriberWithIdentifiers next = subscribersIterator.next();
            if (next.getSubscriber().equals("client1")) {
                found = true;
                assertEquals(1, next.getQos());
            }
        }

        assertTrue(found);
    }

    @Test
    public void test_add_same_topic_twice_mulitple_subs() throws Exception {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(3, topicTree.getSubscribers("a/b").size());
        assertEquals(3, topicTree.subscriptionCounter.getCount());

        topicTree.removeSubscriber("client3", "a/b", null);

        assertEquals(2, topicTree.getSubscribers("a/b").size());
        assertEquals(2, topicTree.subscriptionCounter.getCount());

        topicTree.removeSubscriber("client2", "a/b", null);

        assertEquals(1, topicTree.getSubscribers("a/b").size());
        assertEquals(1, topicTree.subscriptionCounter.getCount());

        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.getSubscribers("a/b").size());
        assertEquals(2, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_add_same_plus_topic_twice() throws Exception {

        topicTree.addTopic("client1", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.getSubscribers("a/+/b").size());

        assertEquals(1, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_add_same_plus_topic_twice_mulitple_subs() throws Exception {

        topicTree.addTopic("client1", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(3, topicTree.getSubscribers("a/+/b").size());
        assertEquals(3, topicTree.subscriptionCounter.getCount());

        topicTree.removeSubscriber("client3", "a/+/b", null);

        assertEquals(2, topicTree.getSubscribers("a/+/b").size());
        assertEquals(2, topicTree.subscriptionCounter.getCount());

        topicTree.removeSubscriber("client2", "a/+/b", null);

        assertEquals(1, topicTree.getSubscribers("a/+/b").size());
        assertEquals(1, topicTree.subscriptionCounter.getCount());

        topicTree.addTopic("client3", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.getSubscribers("a/+/b").size());
        assertEquals(2, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_add_remove() throws Exception {

        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.getSubscribers("a/b").size());
        assertEquals(1, topicTree.subscriptionCounter.getCount());

        topicTree.removeSubscriber("client3", "a/b", null);

        assertEquals(0, topicTree.getSubscribers("a/b").size());
        assertEquals(0, topicTree.subscriptionCounter.getCount());

        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.getSubscribers("a/b").size());
        assertEquals(1, topicTree.subscriptionCounter.getCount());
    }

    @Test
    public void test_remove_wildcard() throws Exception {

        topicTree.addTopic("client", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        assertEquals(1, topicTree.getSubscribers("a/b").size());
        topicTree.addTopic("client", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        assertEquals(1, topicTree.getSubscribers("a/b").size());
        topicTree.removeSubscriber("client", "#", null);
        assertEquals(1, topicTree.getSubscribers("a/b").size());
    }

    @Test
    public void test_topic_overwritten() {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/b", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.removeSubscriber("client1", "a/b", null);

        topicTree.addTopic("client2", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        assertEquals(QoS.EXACTLY_ONCE.getQosNumber(), topicTree.getSubscribers("a/b").asList().get(0).getQos());
        assertEquals("client2", topicTree.getSubscribers("a/b").asList().get(0).getSubscriber());

    }

}
