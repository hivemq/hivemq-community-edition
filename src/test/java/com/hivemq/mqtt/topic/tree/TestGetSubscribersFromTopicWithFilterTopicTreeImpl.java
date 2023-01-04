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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.*;

public class TestGetSubscribersFromTopicWithFilterTopicTreeImpl {

    private LocalTopicTree topicTree;

    @Before
    public void setUp() {
        topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
    }

    @Test
    public void test_empty_topic_tree_get_subscribers() throws Exception {

        final Set<SubscriberWithIdentifiers> any = topicTree.findTopicSubscribers("any").getSubscribers();
        assertTrue(any.isEmpty());
    }

    @Test
    public void test_empty_no_subscriber_for_topic() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("anothertopic", getMatchAllFilter(), false);
        assertTrue(subscribers.isEmpty());
    }

    @Test
    public void test_get_single_subscriber_for_topic() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);

        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_get_single_subscriber_for_topic_with_flags() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE, true, true), (byte) 12, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_multiple_subscribers_for_topic() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(2, subscribers.size());
        assertThat(subscribers, hasItems("subscriber", "subscriber2"));
    }

    @Test
    public void test_same_subscriber_two_times_for_same_topic() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_same_subscriber_two_times_for_same_topic_different_flags() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE, true, true), (byte) 12, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_same_subscriber_two_times_for_same_topic_with_different_qos() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_root_wildcard_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_root_level_wildcard_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_two_level_wildcard_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("+/+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic/level", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_root_level_wildcard_subscriber_with_wildcard() throws Exception {

        topicTree.addTopic("subscriber", new Topic("+/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic/level", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_root_level_wildcard_multiple_subscribers_with_wildcard() throws Exception {

        topicTree.addTopic("subscriber", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_subscriber_add_and_delete() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.removeSubscriber("subscriber", "topic", null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(0, subscribers.size());
    }

    @Test
    public void test_subscriber_add_and_delete_multiple_topics_per_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.removeSubscriber("subscriber", "topic", null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(0, subscribers.size());
    }


    @Test
    public void test_qos_subscriptions_multiple_subscribers() throws Exception {
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(2, subscribers.size());
        assertThat(subscribers, hasItems("subscriber", "subscriber2"));
    }

    @Test
    public void test_multiple_subscribers_for_sys_topic() throws Exception {

        topicTree.addTopic("subscriber", new Topic("$SYS/topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("$SYS/topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("$SYS/topic", getMatchAllFilter(), false);
        assertEquals(2, subscribers.size());
        assertThat(subscribers, hasItems("subscriber", "subscriber2"));
    }

    @Test
    public void test_sys_topic_wildcard_subscriber() throws Exception {

        topicTree.addTopic("subscriber", new Topic("$SYS/+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("$SYS/topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItems("subscriber"));
    }

    @Test
    public void test_same_subscriber_for_same_topic_with_subscriber_map() throws Exception {

        topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));

        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_LEAST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.EXACTLY_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertThat(subscribers, hasItem("subscriber"));
    }

    @Test
    public void test_root_level_wildcard_multiple_subscribers_with_wildcard_with_subscriber_map() throws Exception {

        topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));

        topicTree.addTopic("subscriber", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(3, subscribers.size());
    }

    @Test
    public void test_get_subscriber_from_index_map() throws Exception {
        topicTree.addTopic("subscriber1", new Topic("this/topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("this/topic2", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("this/topic3", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("this/topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertEquals("subscriber1", subscribers.iterator().next());
    }

    @Test
    public void test_get_topc_level_subscriber_from_index_map() throws Exception {
        topicTree.addTopic("subscriber1", new Topic("topic1", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("topic2", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("topic3", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic1", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertEquals("subscriber1", subscribers.iterator().next());

        final Set<String> subscribers2 = topicTree.getSubscribersForTopic("topic2", getMatchAllFilter(), false);
        assertEquals(1, subscribers2.size());
        assertEquals("subscriber2", subscribers2.iterator().next());

        final Set<String> subscribers3 = topicTree.getSubscribersForTopic("topic3", getMatchAllFilter(), false);
        assertEquals(1, subscribers3.size());
        assertEquals("subscriber3", subscribers3.iterator().next());
    }

    @Test
    public void test_get_wildcard_subscriber_from_index_map() throws Exception {
        topicTree.addTopic("subscriber1", new Topic("this/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("this/topic2", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("this/topic3", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("this/topic1", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
        assertEquals("subscriber1", subscribers.iterator().next());
    }

    @Test
    public void test_get_root_level_wildcard_subscriber() throws Exception {
        topicTree.addTopic("subscriber1", new Topic("+/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("+/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber4", new Topic("test/+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("a/test", getMatchAllFilter(), false);
        assertEquals(3, subscribers.size());
    }


    @Test
    public void get_shared_subscriber() {
        final byte sharedFlag = SubscriptionFlag.getDefaultFlags(true, false, false);
        final byte notSharedFlag = SubscriptionFlag.getDefaultFlags(false, false, false);

        topicTree.addTopic("sub1", new Topic("topic", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        topicTree.addTopic("sub2", new Topic("topic", QoS.AT_LEAST_ONCE), notSharedFlag, null);
        topicTree.addTopic("sub3", new Topic("topic", QoS.AT_LEAST_ONCE), sharedFlag, "group2");
        topicTree.addTopic("sub4", new Topic("#", QoS.AT_LEAST_ONCE), sharedFlag, "group");

        final Set<String> subscribers1 = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(4, subscribers1.size());

        final Set<String> subscribers2 = topicTree.getSubscribersForTopic("topic", getSharedSubFilter(), false);
        assertEquals(3, subscribers2.size());

        final Set<String> subscribers2i = topicTree.getSubscribersForTopic("topic", getIndividualSubFilter(), false);
        assertEquals(1, subscribers2i.size());
    }

    @Test
    public void get_shared_subscriber_overlapping() {
        final byte sharedFlag = SubscriptionFlag.getDefaultFlags(true, false, false);
        final byte notSharedFlag = SubscriptionFlag.getDefaultFlags(false, false, false);

        topicTree.addTopic("sub1", new Topic("#", QoS.AT_LEAST_ONCE), sharedFlag, "group1");
        topicTree.addTopic("sub2", new Topic("#", QoS.AT_LEAST_ONCE), notSharedFlag, null);
        topicTree.addTopic("sub1", new Topic("#", QoS.AT_LEAST_ONCE), notSharedFlag, null);


        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(2, subscribers.size());

        final Set<String> subscribers2 = topicTree.getSubscribersForTopic("topic", getSharedSubFilter(), false);
        assertEquals(1, subscribers2.size());

        final Set<String> subscribers3 = topicTree.getSubscribersForTopic("topic", getIndividualSubFilter(), false);
        assertEquals(2, subscribers3.size());
    }

    @Test
    public void get_shared_subscriber_with_same_id() {
        final byte sharedFlag = SubscriptionFlag.getDefaultFlags(true, false, false);

        topicTree.addTopic("client", new Topic("topic/a", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        topicTree.addTopic("client", new Topic("topic/+", QoS.AT_LEAST_ONCE), sharedFlag, "group");
        topicTree.addTopic("client", new Topic("#", QoS.AT_LEAST_ONCE), sharedFlag, "group");

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic", getMatchAllFilter(), false);
        assertEquals(1, subscribers.size());
    }

    @Test
    public void get_subscriber() {
        final byte notSharedFlag = SubscriptionFlag.getDefaultFlags(false, false, false);
        final byte sharedFlag = SubscriptionFlag.getDefaultFlags(true, false, false);

        topicTree.addTopic("client1", new Topic("topic/a", QoS.AT_MOST_ONCE), notSharedFlag, null);
        topicTree.addTopic("client2", new Topic("topic/+", QoS.AT_LEAST_ONCE), notSharedFlag, null);
        topicTree.addTopic("client3", new Topic("#", QoS.EXACTLY_ONCE), notSharedFlag, null);
        topicTree.addTopic("client4", new Topic("topic/a", QoS.AT_MOST_ONCE), sharedFlag, null);

        final Set<String> subscribers = topicTree.getSubscribersForTopic("topic/a", getMatchAllFilter(), false);
        assertEquals(4, subscribers.size());
    }

    @NotNull
    public Predicate<SubscriberWithQoS> getMatchAllFilter() {
        return subscriber -> true;
    }

    @NotNull
    public Predicate<SubscriberWithQoS> getSharedSubFilter() {
        return SubscriberWithQoS::isSharedSubscription;
    }

    @NotNull
    public Predicate<SubscriberWithQoS> getIndividualSubFilter() {
        return subscriber -> !subscriber.isSharedSubscription();
    }
}
