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

import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

public class TestTopicTreeImplEdgeCases {

    private LocalTopicTree topicTree;

    @Before
    public void setUp() {
        InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD.set(1);
        topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
    }

    @Test
    public void test_get_empty_topic() {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);

        assertEquals(0, topicTree.findTopicSubscribers("").getSubscribers().size());

        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void remove_before_index_map_creation() {

        topicTree.addTopic("subscriber", new Topic("a/b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("a/c", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.removeSubscriber("subscriber", "a/c", null);
        topicTree.addTopic("subscriber", new Topic("a/d", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("a/e", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(0, topicTree.findTopicSubscribers("a/c").getSubscribers().size());

        assertEquals(3, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_remove_longer_topic() {

        topicTree.addTopic("subscriber", new Topic("/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("/+/b/c", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("/+/c/d", QoS.AT_MOST_ONCE), (byte) 0, null);

        topicTree.removeSubscriber("subscriber", "/+", null);

        assertEquals(0, topicTree.findTopicSubscribers("/a").getSubscribers().size());

        assertEquals(2, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_leading_wild_card_edge_case() {

        topicTree.addTopic("subscriber1", new Topic("+/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("a/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("+/test", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("a/test").getSubscribers();
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_plus_wild_card_edge_case() {

        topicTree.addTopic("subscriber1", new Topic("a/+/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("a/b/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("a/+/test", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("a/b/test").getSubscribers();
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_plus_wild_card_ending_edge_case() {

        topicTree.addTopic("subscriber1", new Topic("a/test/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("a/test/b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("a/test/+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("a/test/b").getSubscribers();
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_edge_case_slash_topic_direct_match() {

        topicTree.addTopic("subscriber", new Topic("/", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("/").getSubscribers();
        assertFalse(subscribers.isEmpty());
        assertThat(subscribers, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));
    }

    @Test
    public void test_edge_case_slash_topic_wildcard_match() {
        topicTree.addTopic("subscriber", new Topic("+/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        final Set<SubscriberWithIdentifiers> subscribers2 = topicTree.findTopicSubscribers("/").getSubscribers();

        assertFalse(subscribers2.isEmpty());
        assertThat(subscribers2, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));
    }

    @Test
    public void test_edge_case_only_slashes_direct_match() {

        topicTree.addTopic("subscriber", new Topic("/////", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("/////").getSubscribers();
        assertFalse(subscribers.isEmpty());
        assertThat(subscribers, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        final Set<SubscriberWithIdentifiers> subscribers2 = topicTree.findTopicSubscribers("////").getSubscribers();
        assertTrue(subscribers2.isEmpty());
    }

    @Test
    public void test_edge_case_only_slashes_wildcard_match() {

        topicTree.addTopic("subscriber", new Topic("+/+/+/+/+/+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("/////").getSubscribers();
        assertFalse(subscribers.isEmpty());
        assertThat(subscribers, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        topicTree.addTopic("subscriber2", new Topic("+/+/+/+/+/", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers2 = topicTree.findTopicSubscribers("/////").getSubscribers();
        assertFalse(subscribers2.isEmpty());
        assertThat(subscribers2, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        topicTree.addTopic("subscriber3", new Topic("/+/+/+/+/", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers3 = topicTree.findTopicSubscribers("/////").getSubscribers();
        assertFalse(subscribers3.isEmpty());
        assertThat(subscribers3, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        assertEquals(3, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_edge_case_matching_level_wildcard_at_end_null_string() {
        //From https://groups.google.com/forum/#!topic/mqtt/LY7xkGKOJaU

        topicTree.addTopic("subscriber", new Topic("a/+/b", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("a//b").getSubscribers();
        assertFalse(subscribers.isEmpty());
        assertThat(subscribers, hasItem(new SubscriberWithIdentifiers("subscriber", 0, (byte) 0, null, ImmutableList.of(), null)));

        topicTree.addTopic("subscriber2", new Topic("a/b/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        final Set<SubscriberWithIdentifiers> subscribers2 = topicTree.findTopicSubscribers("a/b/").getSubscribers();
        assertFalse(subscribers2.isEmpty());
        assertThat(subscribers2, hasItem(new SubscriberWithIdentifiers("subscriber2", 0, (byte) 0, null, ImmutableList.of(), null)));
    }

    @Test
    public void test_edge_case_more_than_1000_segments() {
        String topic = "";
        for (int i = 0; i < 1000; i++) {
            topic += RandomStringUtils.randomAlphanumeric(1) + "/";
        }
        topic += "topic";

        topicTree.addTopic("subscriber", new Topic(topic, QoS.EXACTLY_ONCE), (byte) 0, null);

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers(topic).getSubscribers();

        assertEquals(0, subscribers.size());
    }

    @Test
    public void test_add_same_wildcard_topic_twice() {

        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.findTopicSubscribers("any").getSubscribers().size());

        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_add_same_wildcard_topic_twice_different_qos() {

        topicTree.addTopic("client1", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("#", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.findTopicSubscribers("any").getSubscribers().size());

        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_add_same_wildcard_topic_twice_mulitple_subs() {

        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(3, topicTree.findTopicSubscribers("#").getSubscribers().size());
        assertEquals(3, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.removeSubscriber("client3", "#", null);

        assertEquals(2, topicTree.findTopicSubscribers("#").getSubscribers().size());
        assertEquals(2, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.removeSubscriber("client2", "#", null);

        assertEquals(1, topicTree.findTopicSubscribers("#").getSubscribers().size());
        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.addTopic("client3", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.findTopicSubscribers("#").getSubscribers().size());
        assertEquals(2, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_add_same_topic_twice() {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.findTopicSubscribers("a/b").getSubscribers().size());

        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());
    }


    @Test
    public void test_add_same_topic_twice_different_qos() {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/b", QoS.AT_LEAST_ONCE), (byte) 0, null);

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("a/b").getSubscribers();
        assertEquals(1, subscribers.size());

        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());
        final SubscriberWithIdentifiers next = subscribers.iterator().next();

        assertEquals(1, next.getQos());
    }


    @Test
    public void test_add_same_topic_twice_different_qos_more_than_32_subscribers() {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        for (int i = 0; i < 32; i++) {
            topicTree.addTopic("client" + (i + 2), new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        }

        topicTree.addTopic("client1", new Topic("a/b", QoS.AT_LEAST_ONCE), (byte) 0, null);

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("a/b").getSubscribers();
        assertEquals(33, subscribers.size());

        assertEquals(33, topicTree.counters.getSubscriptionCounter().getCount());

        final UnmodifiableIterator<SubscriberWithIdentifiers> subscribersIterator = subscribers.iterator();
        boolean found = false;
        while (subscribersIterator.hasNext()) {
            final SubscriberWithIdentifiers next = subscribersIterator.next();
            if ("client1".equals(next.getSubscriber())) {
                found = true;
                assertEquals(1, next.getQos());
            }
        }

        assertTrue(found);
    }

    @Test
    public void test_add_same_topic_twice_mulitple_subs() {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(3, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
        assertEquals(3, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.removeSubscriber("client3", "a/b", null);

        assertEquals(2, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
        assertEquals(2, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.removeSubscriber("client2", "a/b", null);

        assertEquals(1, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
        assertEquals(2, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_add_same_plus_topic_twice() {

        topicTree.addTopic("client1", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.findTopicSubscribers("a/+/b").getSubscribers().size());

        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_add_same_plus_topic_twice_mulitple_subs() {

        topicTree.addTopic("client1", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client1", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client3", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(3, topicTree.findTopicSubscribers("a/+/b").getSubscribers().size());
        assertEquals(3, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.removeSubscriber("client3", "a/+/b", null);

        assertEquals(2, topicTree.findTopicSubscribers("a/+/b").getSubscribers().size());
        assertEquals(2, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.removeSubscriber("client2", "a/+/b", null);

        assertEquals(1, topicTree.findTopicSubscribers("a/+/b").getSubscribers().size());
        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.addTopic("client3", new Topic("a/+/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.findTopicSubscribers("a/+/b").getSubscribers().size());
        assertEquals(2, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_add_remove() {

        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.removeSubscriber("client3", "a/b", null);

        assertEquals(0, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
        assertEquals(0, topicTree.counters.getSubscriptionCounter().getCount());

        topicTree.addTopic("client3", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
        assertEquals(1, topicTree.counters.getSubscriptionCounter().getCount());
    }

    @Test
    public void test_remove_wildcard() throws Exception {

        topicTree.addTopic("client", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        assertEquals(1, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
        topicTree.addTopic("client", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null);
        assertEquals(1, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
        topicTree.removeSubscriber("client", "#", null);
        assertEquals(1, topicTree.findTopicSubscribers("a/b").getSubscribers().size());
    }

    @Test
    public void test_topic_overwritten() {

        topicTree.addTopic("client1", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("client2", new Topic("a/b", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.removeSubscriber("client1", "a/b", null);

        topicTree.addTopic("client2", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null);
        assertEquals(QoS.EXACTLY_ONCE.getQosNumber(), topicTree.findTopicSubscribers("a/b").getSubscribers().asList().get(0).getQos());
        assertEquals("client2", topicTree.findTopicSubscribers("a/b").getSubscribers().asList().get(0).getSubscriber());

    }

}
