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
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static com.hivemq.configuration.service.InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

@SuppressWarnings("Duplicates")
public class TestRemoveSubscriberFromTopicInTopicTreeImpl {

    private LocalTopicTree topicTree;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TOPIC_TREE_MAP_CREATION_THRESHOLD.set(1);
        topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));

    }

    @Test
    public void test_remove_from_empty_tree() {
        topicTree.removeSubscriber("subscriber", "topic", null);

        assertEquals(0, topicTree.segments.size());
        assertEquals(0, topicTree.rootWildcardSubscribers.size());
    }

    /*
        Exact subscriptions
     */

    @Test
    public void test_remove_subscriber_with_one_level_subscription() {
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        topicTree.removeSubscriber("subscriber", "topic", null);

        assertEquals(0, topicTree.segments.size());
        assertEquals(0, topicTree.rootWildcardSubscribers.size());
    }

    @Test
    public void test_remove_subscriber_add_and_remove_again() {
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        topicTree.removeSubscriber("subscriber", "topic", null);
        topicTree.addTopic("subscriber", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.removeSubscriber("subscriber", "topic", null);

        assertEquals(0, topicTree.segments.size());
        assertEquals(0, topicTree.rootWildcardSubscribers.size());
    }

    @Test
    public void test_remove_subscriber_with_one_level_wildcard_subscription() {
        topicTree.addTopic("subscriber", new Topic("+", QoS.AT_MOST_ONCE), (byte) 0, null);

        topicTree.removeSubscriber("subscriber", "+", null);

        assertEquals(0, topicTree.segments.size());
        assertEquals(0, topicTree.rootWildcardSubscribers.size());
    }

    @Test
    public void test_remove_second_subscriber_with_one_level_wildcard_subscription() {
        topicTree.addTopic("subscriber", new Topic("+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("+", QoS.AT_MOST_ONCE), (byte) 0, null);

        final TopicTreeNode firstNode = topicTree.segments.get("+");
        assertEquals(2, firstNode.exactSubscriptions.getSubscriberCount());

        topicTree.removeSubscriber("subscriber2", "+", null);

        assertEquals(1, firstNode.exactSubscriptions.getSubscriberCount());

        assertThat(firstNode.exactSubscriptions.getSubscribers(), hasItem(new SubscriberWithQoS("subscriber", 0, (byte) 0, null, null, null)));
    }

    @Test
    public void test_remove_second_subscriber_with_first_level_wildcard_subscription() {
        topicTree.addTopic("subscriber", new Topic("level0/#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("level0/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        final TopicTreeNode firstNode = topicTree.segments.get("level0");
        assertEquals(2, firstNode.wildcardSubscriptions.getSubscriberCount());

        topicTree.removeSubscriber("subscriber2", "level0/#", null);

        assertEquals(1, firstNode.wildcardSubscriptions.getSubscriberCount());

        assertThat(firstNode.wildcardSubscriptions.getSubscribers(), hasItem(new SubscriberWithQoS("subscriber", 0, (byte) 0, null, null, null)));
    }

    @Test
    public void test_remove_subscriber_subscription_multiple_levels_children_get_deleted() {
        topicTree.addTopic("subscriber", new Topic("my/topic/subscription", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.segments.size());
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my")));
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my").getChildren()[0]));
        assertEquals(1, topicTree.segments.get("my").getChildren()[0].getChildren()[0].exactSubscriptions.getSubscriberCount());

        topicTree.removeSubscriber("subscriber", "my/topic/subscription", null);

        //The the root node deleted the reference to the children since there are no subscribers left
        assertEquals(0, topicTree.segments.size());
    }

    @Test
    public void test_dont_remove_subscriber_subscription_multiple_levels_if_no_match() {
        topicTree.addTopic("subscriber", new Topic("my/topic/subscription", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.segments.size());
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my").getChildren()[0]));
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my").getChildren()[0]));
        assertEquals(1, topicTree.segments.get("my").getChildren()[0].getChildren()[0].exactSubscriptions.getSubscriberCount());

        topicTree.removeSubscriber("subscriber", "my/topic/subscription/test", null);

        //Nothing should happen
        assertEquals(1, topicTree.segments.size());
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my")));
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my").getChildren()[0]));
        assertEquals(1, topicTree.segments.get("my").getChildren()[0].getChildren()[0].exactSubscriptions.getSubscriberCount());
    }

    @Test
    public void test_remove_subscription_multiple_levels_children_get_deleted_only_if_not_other_subscribers_available() {
        topicTree.addTopic("subscriber", new Topic("my/topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("my/second", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.segments.size());
        assertEquals(2, LocalTopicTree.getChildrenCount(topicTree.segments.get("my")));
        assertEquals(1, topicTree.segments.get("my").getChildren()[0].exactSubscriptions.getSubscriberCount());

        topicTree.removeSubscriber("subscriber", "my/topic", null);

        //The root node children are not deleted because there are subscribers left
        assertEquals(1, topicTree.segments.size());
        //The reference for the first subnode was deleted because there are no subscribers left on this path
        assertNull(topicTree.segments.get("my").getChildren()[0]);
        //The second node still has a subscriber
        assertThat(topicTree.segments.get("my").getChildren()[1].exactSubscriptions.getSubscribers(), hasItem(new SubscriberWithQoS("subscriber2", 0, (byte) 0, null, null, null)));
    }

    @Test
    public void test_remove_subscription_multiple_levels_subscriber_does_not_exist() {
        topicTree.addTopic("subscriber", new Topic("my/topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.segments.size());
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my")));
        assertEquals(1, topicTree.segments.get("my").getChildren()[0].exactSubscriptions.getSubscriberCount());

        topicTree.removeSubscriber("subscriber2", "my/topic", null);

        //Nothing changed
        assertEquals(1, topicTree.segments.size());
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my")));
        assertEquals(1, topicTree.segments.get("my").getChildren()[0].exactSubscriptions.getSubscriberCount());
    }


    /*
        Wildcard subscriptions
     */

    @Test
    public void test_remove_subscriber_with_root_wildcard_subscription() {
        topicTree.addTopic("subscriber", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        topicTree.removeSubscriber("subscriber", "#", null);

        assertEquals(0, topicTree.segments.size());
    }

    @Test
    public void test_remove_second_subscriber_with_root_wildcard_subscription() {
        topicTree.addTopic("subscriber", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        topicTree.removeSubscriber("subscriber2", "#", null);

        assertEquals(1, topicTree.rootWildcardSubscribers.size());

        assertThat(topicTree.rootWildcardSubscribers, hasItem(new SubscriberWithQoS("subscriber", 0, (byte) 0, null, null, null)));
    }

    @Test
    public void test_remove_wildcard_subscription_multiple_levels_children_get_deleted_only_if_not_other_subscribers_available() {
        topicTree.addTopic("subscriber", new Topic("my/topic/#", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("my/second/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.segments.size());
        assertEquals(2, LocalTopicTree.getChildrenCount(topicTree.segments.get("my")));
        assertEquals(1, topicTree.segments.get("my").getChildren()[0].wildcardSubscriptions.getSubscriberCount());

        topicTree.removeSubscriber("subscriber", "my/topic/#", null);

        //The root node children are not deleted because there are subscribers left
        assertEquals(1, topicTree.segments.size());
        //The reference for the first subnode was deleted because there are no subscribers left on this path
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my")));
        //The second node still has a subscriber
        assertThat(topicTree.segments.get("my").getChildren()[1].wildcardSubscriptions.getSubscribers(), hasItem(new SubscriberWithQoS("subscriber2", 0, (byte) 0, null, null, null)));
    }

    @Test
    public void test_remove_wildcard_subscription_multiple_levels_subscriber_does_not_exist() {
        topicTree.addTopic("subscriber", new Topic("my/topic/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.segments.size());
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my")));
        assertEquals(1, topicTree.segments.get("my").getChildren()[0].wildcardSubscriptions.getSubscriberCount());

        topicTree.removeSubscriber("subscriber2", "my/topic/#", null);

        //Nothing changed
        assertEquals(1, topicTree.segments.size());
        assertEquals(1, LocalTopicTree.getChildrenCount(topicTree.segments.get("my")));
        assertEquals(1, topicTree.segments.get("my").getChildren()[0].wildcardSubscriptions.getSubscriberCount());
    }

    @Test
    public void test_reuse_array_index_after_remove() {
        topicTree.addTopic("subscriber", new Topic("topic/t", QoS.AT_MOST_ONCE), (byte) 0, null);

        topicTree.removeSubscriber("subscriber", "topic/t", null);

        topicTree.addTopic("subscriber", new Topic("topic/t", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.segments.get("topic").children.length);
    }

    @Test
    public void test_remove_node_using_index_map() {
        topicTree.addTopic("subscriber", new Topic("topic/topic1", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic/topic2", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber", new Topic("topic/topic3", QoS.AT_MOST_ONCE), (byte) 0, null);

        topicTree.removeSubscriber("subscriber", "topic/topic1", null);
        topicTree.removeSubscriber("subscriber", "topic/topic2", null);
        topicTree.removeSubscriber("subscriber", "topic/topic3", null);

        assertEquals(0, topicTree.segments.size());
    }

    @Test
    public void test_remove_from_index_map() {
        topicTree.addTopic("subscriber1", new Topic("topic/topic1", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("topic/topic1", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("topic/topic2", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber4", new Topic("topic/topic3", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(3, topicTree.segments.get("topic").getChildrenMap().size());
        topicTree.removeSubscriber("subscriber1", "topic/topic1", null);
        assertEquals(3, topicTree.segments.get("topic").getChildrenMap().size());
        topicTree.removeSubscriber("subscriber2", "topic/topic1", null);
        assertEquals(2, topicTree.segments.get("topic").getChildrenMap().size());
        topicTree.removeSubscriber("subscriber3", "topic/topic2", null);
        assertEquals(1, topicTree.segments.get("topic").getChildrenMap().size());
        topicTree.removeSubscriber("subscriber4", "topic/topic3", null);
        assertEquals(0, topicTree.segments.size());
    }

    @Test
    public void test_remove_multi_level_topic_from_index_map() {
        topicTree.addTopic("subscriber1", new Topic("topic/topic1/part1", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber2", new Topic("topic/topic1/part1", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber3", new Topic("topic/topic1/part2", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("subscriber4", new Topic("topic/topic1/part3", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(3, topicTree.segments.get("topic").children[0].getChildrenMap().size());
        topicTree.removeSubscriber("subscriber1", "topic/topic1/part1", null);
        assertEquals(3, topicTree.segments.get("topic").children[0].getChildrenMap().size());
        topicTree.removeSubscriber("subscriber2", "topic/topic1/part1", null);
        assertEquals(2, topicTree.segments.get("topic").children[0].getChildrenMap().size());
        topicTree.removeSubscriber("subscriber3", "topic/topic1/part2", null);
        assertEquals(1, topicTree.segments.get("topic").children[0].getChildrenMap().size());
        topicTree.removeSubscriber("subscriber4", "topic/topic1/part3", null);
        assertEquals(0, topicTree.segments.size());
    }
}
