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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.SubscriptionFlags;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.*;

/**
 * @author  Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class TestGetSubscribersWithFilterFromTopicTreeImpl {


    private TopicTreeImpl topicTree;

    private Counter subscriptionCounter;
    private Counter staleSubscriptionsCounter;

    @Before
    public void setUp() {
        subscriptionCounter = new Counter();
        staleSubscriptionsCounter = new Counter();
        topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));

    }

    @Test
    public void test_empty_topic_tree_get_subscribers() throws Exception {

        final Set<String> any = topicTree.getSubscribersWithFilter("any", getMatchAllFilter());
        assertEquals(true, any.isEmpty());
    }

    @Test
    public void test_empty_topic_tree_get_subscribers_wildcard() throws Exception {

        final Set<String> any = topicTree.getSubscribersWithFilter("topic/#", getMatchAllFilter());
        assertEquals(true, any.isEmpty());
    }

    @Test
    public void test_empty_topic_tree_get_subscribers_root_wildcard() throws Exception {

        final Set<String> any = topicTree.getSubscribersWithFilter("#", getMatchAllFilter());
        assertEquals(true, any.isEmpty());
    }


    @Test
    public void test_empty_no_subscriber_for_topic_filter() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("anothertopic", getMatchAllFilter());
        assertEquals(true, subscribers.isEmpty());
    }



    @Test
    public void test_get_single_subscriber_for_topic_filter() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_get_single_subscriber_for_long_topic_filter() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic/1/2/3/4/5/6/7/8/9/0", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic/1/2/3/4/5/6/7/8/9/0", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_get_single_subscriber_for_long_topic_filter_wildcard() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic/1/2/3/4/5/6/7/8/9/0/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic/1/2/3/4/5/6/7/8/9/0/#", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_get_single_subscriber_for_long_topic_filter_plus_wildcard() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic/1/2/3/4/+/6/7/8/9/0/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic/1/2/3/4/+/6/7/8/9/0/#", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_get_single_subscriber_for_topic_filter_with_flags() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE, true, true), (byte) 12, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_multiple_subscribers_for_topic_filter() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(2, subscribers.size());
        assertThat(subscribers, hasItems("subscriber", "subscriber2"));
    }

    @Test
    public void test_same_subscriber_two_times_for_same_topic() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_same_subscriber_two_times_for_same_topic_different_flags() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE, true, true), (byte) 12, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_same_subscriber_two_times_for_same_topic_with_different_qos() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_root_wildcard_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("#", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_root_level_wildcard_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("+", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_two_level_wildcard_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("+/+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("+/+", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_root_level_wildcard_subscriber_with_wildcard() throws Exception {

        topicTree.addTopic("subscriber", new Topic("+/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("+/#", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_root_level_wildcard_multiple_subscribers_with_wildcard() throws Exception {

        topicTree.addTopic("subscriber", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("#", getMatchAllFilter());
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_subscriber_add_and_delete() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.removeSubscriber("subscriber", "topic", null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(0, subscribers.size());
    }

    @Test
    public void test_subscriber_add_and_delete_multiple_topics_per_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.removeSubscriber("subscriber", "topic", null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(0, subscribers.size());
    }

    @Test
    public void test_qos_subscriptions_multiple_subscribers() throws Exception {
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(2, subscribers.size());
        assertThat(subscribers, hasItems("subscriber", "subscriber2"));
    }

    @Test
    public void test_multiple_subscribers_for_sys_topic() throws Exception {

        topicTree.addTopic("subscriber", new Topic("$SYS/topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("$SYS/topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("$SYS/topic", getMatchAllFilter());
        assertEquals(2, subscribers.size());
        assertThat(subscribers, hasItems("subscriber", "subscriber2"));
    }

    @Test
    public void test_sys_topic_wildcard_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("$SYS/+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("$SYS/+", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_same_subscriber_for_same_topic_with_subscriber_map() throws Exception {

        topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_root_level_wildcard_multiple_subscribers_with_wildcard_with_subscriber_map() throws Exception {

        topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));

        topicTree.addTopic("subscriber", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("#", getMatchAllFilter());
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_get_subscriber_from_index_map() throws Exception {
        topicTree.addTopic("subscriber1", new Topic("this/topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("this/topic2", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("this/topic3", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("this/topic", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertEquals("subscriber1", subscribers.iterator().next());
    }

    @Test
    public void test_get_topc_level_subscriber_from_index_map() throws Exception {
        topicTree.addTopic("subscriber1", new Topic("topic1", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("topic2", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("topic3", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("topic1", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertEquals("subscriber1", subscribers.iterator().next());
    }

    @Test
    public void test_get_wildcard_subscriber_from_index_map() throws Exception {
        topicTree.addTopic("subscriber1", new Topic("this/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("this/topic2", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("this/topic3", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersWithFilter("this/+", getMatchAllFilter());
        assertEquals(1, subscribers.size());
        assertEquals("subscriber1", subscribers.iterator().next());
    }


    @Test
    public void get_shared_subscriber() {
        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);
        final byte notSharedFlag = SubscriptionFlags.getDefaultFlags(false, false, false);

        topicTree.addTopic("sub1", new Topic("topic", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        topicTree.addTopic("sub2", new Topic("topic", QoS.AT_LEAST_ONCE), notSharedFlag, null);
        topicTree.addTopic("sub3", new Topic("topic", QoS.AT_LEAST_ONCE), sharedFlag, "group2");
        topicTree.addTopic("sub4", new Topic("#", QoS.AT_LEAST_ONCE), sharedFlag, "group");

        final ImmutableSet<String> subscribers1 = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(3, subscribers1.size());
        assertThat(subscribers1, hasItems("sub1", "sub2", "sub3"));

        final ImmutableSet<String> subscribers2 = topicTree.getSubscribersWithFilter("#", getMatchAllFilter());
        assertEquals(1, subscribers2.size());
        assertEquals("sub4", subscribers2.iterator().next());

        topicTree.addTopic("sub5", new Topic("topic/#", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        final ImmutableSet<String> subscribers3 = topicTree.getSubscribersWithFilter("topic/#", getMatchAllFilter());
        assertEquals(1, subscribers3.size());
        assertEquals("sub5", subscribers3.iterator().next());

        topicTree.addTopic("sub6", new Topic("topic/a", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        final ImmutableSet<String> subscribers5 = topicTree.getSubscribersWithFilter("topic/a", getMatchAllFilter());
        assertEquals(1, subscribers5.size());
        assertEquals("sub6", subscribers5.iterator().next());

        topicTree.addTopic("sub7", new Topic("topic/+", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        final ImmutableSet<String> subscribers6 = topicTree.getSubscribersWithFilter("topic/+", getMatchAllFilter());
        assertEquals(1, subscribers6.size());
        assertEquals("sub7", subscribers6.iterator().next());

        topicTree.addTopic("sub8", new Topic("topic/+/b", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        final ImmutableSet<String> subscribers7 = topicTree.getSubscribersWithFilter("topic/+/b", getMatchAllFilter());
        assertEquals(1, subscribers7.size());
        assertEquals("sub8", subscribers7.iterator().next());

        final ImmutableSet<String> subscribers8 = topicTree.getSubscribersWithFilter("topic/a/b", getMatchAllFilter());
        assertEquals(0, subscribers8.size());
    }

    @Test
    public void get_shared_subscriber_overlapping() {
        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);

        topicTree.addTopic("sub1", new Topic("#", QoS.AT_LEAST_ONCE), sharedFlag, "group1");
        topicTree.addTopic("sub2", new Topic("#", QoS.AT_LEAST_ONCE), sharedFlag, "group2");
        topicTree.addTopic("sub3", new Topic("topic", QoS.AT_LEAST_ONCE), sharedFlag, "group3");
        topicTree.addTopic("sub4", new Topic("topic", QoS.AT_LEAST_ONCE), sharedFlag, "group4");

        final ImmutableSet<String> subscribers1 = topicTree.getSubscribersWithFilter("#", getMatchAllFilter());
        assertEquals(2, subscribers1.size());

        final ImmutableSet<String> subscribers3 = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(2, subscribers3.size());
    }

    @Test
    public void get_shared_subscriber_with_same_id() {
        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);

        topicTree.addTopic("client", new Topic("topic/a", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        topicTree.addTopic("client", new Topic("topic/+", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        topicTree.addTopic("client", new Topic("#", QoS.AT_LEAST_ONCE), sharedFlag, "group");

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("topic/a");
        assertEquals(3, subscribers.size());
    }


    @Test
    public void get_subscriber_shared_overlapping() {
        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);

        topicTree.addTopic("client1", new Topic("topic/a", QoS.AT_MOST_ONCE, false, false, Mqtt5RetainHandling.SEND, 1), sharedFlag, null);
        topicTree.addTopic("client1", new Topic("topic/+", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, 2), sharedFlag, "group");

        final SubscriberWithIdentifiers subscribers = topicTree.getSubscriber("client1", "topic/a");

        assertEquals(1, subscribers.getSubscriptionIdentifier().length());
        assertTrue(subscribers.getSubscriptionIdentifier().contains(2));
    }

    @Test
    public void get_subscriber_non_shared_overlapping() {
        final byte notSharedFlag = SubscriptionFlags.getDefaultFlags(false, false, false);

        topicTree.addTopic("client1", new Topic("topic/a", QoS.AT_MOST_ONCE, false, false, Mqtt5RetainHandling.SEND, 1), notSharedFlag, null);
        topicTree.addTopic("client1", new Topic("topic/+", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, 2), notSharedFlag, null);

        final SubscriberWithIdentifiers subscribers = topicTree.getSubscriber("client1", "topic/a");

        assertEquals(2, subscribers.getSubscriptionIdentifier().length());
        assertTrue(subscribers.getSubscriptionIdentifier().contains(1));
        assertTrue(subscribers.getSubscriptionIdentifier().contains(2));
    }

    @Test
    public void get_subscriber_shared_and_non_shared_overlapping() {
        final byte notSharedFlag = SubscriptionFlags.getDefaultFlags(false, false, false);
        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);

        topicTree.addTopic("client1", new Topic("topic/a", QoS.AT_MOST_ONCE, false, false, Mqtt5RetainHandling.SEND, 1), notSharedFlag, null);
        topicTree.addTopic("client1", new Topic("topic/+", QoS.AT_LEAST_ONCE, false, false, Mqtt5RetainHandling.SEND, 2), sharedFlag, "group");

        final SubscriberWithIdentifiers subscribers = topicTree.getSubscriber("client1", "topic/a");

        assertEquals(1, subscribers.getSubscriptionIdentifier().length());
        assertEquals(1, subscribers.getSubscriptionIdentifier().get(0));
    }


    @Test
    public void test_normal_and_shared_subscription() {
        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);
        final byte notSharedFlag = SubscriptionFlags.getDefaultFlags(false, false, false);

        topicTree.addTopic("client", new Topic("topic", QoS.AT_LEAST_ONCE), notSharedFlag, null);
        topicTree.addTopic("client", new Topic("topic", QoS.AT_LEAST_ONCE), sharedFlag, "name");

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("topic");
        assertEquals(2, subscribers.size());
    }

    @Test
    public void test_normal_and_shared_subscription_with_map() {
        topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);
        final byte notSharedFlag = SubscriptionFlags.getDefaultFlags(false, false, false);

        topicTree.addTopic("client1", new Topic("topic", QoS.AT_LEAST_ONCE), notSharedFlag, null);
        topicTree.addTopic("client1", new Topic("topic", QoS.AT_LEAST_ONCE), sharedFlag, "name");
        topicTree.addTopic("client2", new Topic("topic", QoS.AT_LEAST_ONCE), notSharedFlag, null);
        topicTree.addTopic("client2", new Topic("topic", QoS.AT_LEAST_ONCE), sharedFlag, "name");

        final ImmutableSet<String> subscribers = topicTree.getSubscribersWithFilter("topic", getMatchAllFilter());
        assertEquals(2, subscribers.size());


    }

    @Test
    public void test_multiple_root_wildcards() {
        final byte notSharedFlag = SubscriptionFlags.getDefaultFlags(false, false, false);

        for (int i = 0; i < 20; i++) {
            topicTree.addTopic("client" + i, new Topic("#", QoS.AT_LEAST_ONCE), notSharedFlag, null);
        }

        final ImmutableSet<String> subscribers = topicTree.getSubscribersWithFilter("#", getMatchAllFilter());
        assertEquals(20, subscribers.size());

        new Random().nextInt();
    }

    @Test
    public void test_multiple_wildcards() {
        final byte notSharedFlag = SubscriptionFlags.getDefaultFlags(false, false, false);

        for (int i = 0; i < 20; i++) {
            topicTree.addTopic("client" + i, new Topic("topic/#", QoS.AT_LEAST_ONCE), notSharedFlag, null);
        }

        final ImmutableSet<String> subscribers = topicTree.getSubscribersWithFilter("topic/#", getMatchAllFilter());
        assertEquals(20, subscribers.size());

        new Random().nextInt();
    }


    @Test
    public void test_add_shared_wildcard() {
        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);

        topicTree.addTopic("client", new Topic("#", QoS.AT_LEAST_ONCE), sharedFlag, "name1");
        topicTree.addTopic("client", new Topic("#", QoS.AT_LEAST_ONCE), sharedFlag, "name2");

        final ImmutableSet<String> subscribers = topicTree.getSubscribersWithFilter("#", getMatchAllFilter());
        assertEquals(1, subscribers.size());

        new Random().nextInt();
    }


    @NotNull
    public LocalTopicTree.ItemFilter getMatchAllFilter() {
        return new LocalTopicTree.ItemFilter() {
            @Override
            public boolean checkItem(@NotNull final SubscriberWithQoS subscriber) {
                return true;
            }
        };
    }
}
