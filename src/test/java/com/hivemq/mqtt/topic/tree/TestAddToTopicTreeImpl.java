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
import com.google.common.collect.ImmutableSet;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class TestAddToTopicTreeImpl {

    @Before
    public void setUp() throws Exception {
        InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD.set(1);
    }

    @Test(expected = NullPointerException.class)
    public void test_subscriber_null() {
        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic(null, new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
    }

    @Test(expected = NullPointerException.class)
    public void test_topic_null() throws Exception {
        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("subscriber", null, (byte) 0, null);
    }

    @Test
    public void addTopic_whenAddedFirstLevelSubscriptions_thenSubscriptionsArePresentInTree() throws Exception {
        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Collection<TopicTreeNode> firstTopicLevel = topicTree.segments.values();
        assertEquals(1, firstTopicLevel.size());

        final TopicTreeNode firstLevelNode = firstTopicLevel.iterator().next();
        assertNull(firstLevelNode.getChildren());
        assertEquals("topic", firstLevelNode.getTopicPart());


        //No wildcard subscribers
        assertNull(firstLevelNode.wildcardSubscriptions.nonSharedSubscribersArray);

        //Now test that the subscribers are actually here
        final SubscriberWithQoS[] exactSubscribers = firstLevelNode.exactSubscriptions.nonSharedSubscribersArray;
        assertEquals(2, exactSubscribers.length);
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), exactSubscribers[0]);
        assertEquals(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null), exactSubscribers[1]);
    }

    @Test
    public void addTopic_whenAddedWildcardSubscriptionOnFirstLevel_thenWildcardSubscriptionIsPresentInTree() {
        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(0, topicTree.segments.values().size());

        assertEquals(1, topicTree.rootWildcardSubscribers.size());
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), topicTree.rootWildcardSubscribers.get(0));
    }

    @Test
    public void addTopic_whenAddedWildcardSubscriptionOnSecondLevel_thenWildcardSubscriptionIsPresentInTree() {
        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("topic/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.segments.size());

        final TopicTreeNode wildcardNode = topicTree.segments.values().iterator().next();
        assertNull(wildcardNode.getChildren());
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), wildcardNode.wildcardSubscriptions.nonSharedSubscribersArray[0]);
        assertNull(wildcardNode.exactSubscriptions.nonSharedSubscribersArray);

        assertEquals(0, topicTree.rootWildcardSubscribers.size());
    }

    @Test
    public void addTopic_whenAddedMultipleFirstLevelSubscriptions_thenSubscriptionsArePresentInTreeOnFirstLevelInArray() {
        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("a", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub3", new Topic("b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub4", new Topic("c", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Collection<TopicTreeNode> firstTopicLevel = topicTree.segments.values();
        assertEquals(3, firstTopicLevel.size());

        /*
            Test the a Node
        */
        final TopicTreeNode aNode = topicTree.segments.get("a");
        assertEquals("a", aNode.getTopicPart());
        assertNull(aNode.getChildren());


        assertNull(aNode.wildcardSubscriptions.nonSharedSubscribersArray);

        final SubscriberWithQoS[] exactSubscribers = aNode.exactSubscriptions.nonSharedSubscribersArray;
        assertEquals(1, exactSubscribers.length);
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), exactSubscribers[0]);

        /*
            Test the b Node
        */

        final TopicTreeNode bNode = topicTree.segments.get("b");
        assertEquals("b", bNode.getTopicPart());
        assertNull(bNode.getChildren());

        assertNull(bNode.wildcardSubscriptions.nonSharedSubscribersArray);

        final SubscriberWithQoS[] bNodeExactSubscribers = bNode.exactSubscriptions.nonSharedSubscribersArray;
        assertEquals(2, bNodeExactSubscribers.length);
        assertEquals(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null), bNodeExactSubscribers[0]);
        assertEquals(new SubscriberWithQoS("sub3", 0, (byte) 0, null, null, null), bNodeExactSubscribers[1]);

        /*
            Test the c Node
        */
        final TopicTreeNode cNode = topicTree.segments.get("c");
        assertEquals("c", cNode.getTopicPart());
        assertNull(cNode.getChildren());


        assertNull(cNode.wildcardSubscriptions.nonSharedSubscribersArray);

        final SubscriberWithQoS[] cNodeExactSubscribers = cNode.exactSubscriptions.nonSharedSubscribersArray;
        assertEquals(1, cNodeExactSubscribers.length);
        assertEquals(new SubscriberWithQoS("sub4", 0, (byte) 0, null, null, null), cNodeExactSubscribers[0]);
    }

    @Test
    public void addTopic_whenAddedMultipleFirstLevelSubscriptions_thenSubscriptionsArePresentInTreeOnFirstLevelInMap() {
        InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD.set(-1);

        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("a", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub3", new Topic("b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub4", new Topic("c", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Collection<TopicTreeNode> firstTopicLevel = topicTree.segments.values();
        assertEquals(3, firstTopicLevel.size());

        /*
            Test the a Node
        */
        final TopicTreeNode aNode = topicTree.segments.get("a");
        assertEquals("a", aNode.getTopicPart());
        assertNull(aNode.getChildren());


        assertNull(aNode.wildcardSubscriptions.nonSharedSubscribersArray);

        assertEquals(1, aNode.exactSubscriptions.nonSharedSubscribersMap.values().size());
        assertTrue(aNode.exactSubscriptions.nonSharedSubscribersMap.containsValue(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null)));

        /*
            Test the b Node
        */

        final TopicTreeNode bNode = topicTree.segments.get("b");
        assertEquals("b", bNode.getTopicPart());
        assertNull(bNode.getChildren());

        assertNull(bNode.wildcardSubscriptions.nonSharedSubscribersArray);

        assertEquals(2, bNode.exactSubscriptions.nonSharedSubscribersMap.values().size());
        assertTrue(bNode.exactSubscriptions.nonSharedSubscribersMap.containsValue(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null)));
        assertTrue(bNode.exactSubscriptions.nonSharedSubscribersMap.containsValue(new SubscriberWithQoS("sub3", 0, (byte) 0, null, null, null)));

        /*
            Test the c Node
        */
        final TopicTreeNode cNode = topicTree.segments.get("c");
        assertEquals("c", cNode.getTopicPart());
        assertNull(cNode.getChildren());


        assertNull(cNode.wildcardSubscriptions.nonSharedSubscribersArray);

        assertEquals(1, cNode.exactSubscriptions.nonSharedSubscribersMap.size());
        assertTrue(cNode.exactSubscriptions.nonSharedSubscribersMap.containsValue(new SubscriberWithQoS("sub4", 0, (byte) 0, null, null, null)));
    }

    @Test
    public void addTopic_whenAddedMultipleSingleLevelWildcardSubscriptions_thenSubscriptionsArePresentInTreeOnCorrespondingLevelsInArray() {
        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("+/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub3", new Topic("test/+/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub4", new Topic("test/+/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.segments.size());

        /*
            first single level wildcard
         */
        final TopicTreeNode firstWildcardNode = topicTree.segments.get("+");
        assertEquals("+", firstWildcardNode.getTopicPart());
        assertEquals(1, firstWildcardNode.getChildren().length);
        assertEquals(1, firstWildcardNode.exactSubscriptions.nonSharedSubscribersArray.length);
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), firstWildcardNode.exactSubscriptions.nonSharedSubscribersArray[0]);
        assertNull(firstWildcardNode.wildcardSubscriptions.nonSharedSubscribersArray);

        /*
            second single level wildcard
         */
        final TopicTreeNode secondWildcardNode = firstWildcardNode.children[0];
        assertEquals("+", secondWildcardNode.getTopicPart());
        assertNull(secondWildcardNode.getChildren());
        assertEquals(1, secondWildcardNode.exactSubscriptions.nonSharedSubscribersArray.length);
        assertEquals(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null), secondWildcardNode.exactSubscriptions.nonSharedSubscribersArray[0]);
        assertNull(secondWildcardNode.wildcardSubscriptions.nonSharedSubscribersArray);

        /*
            test root node
         */
        final TopicTreeNode testRootNode = topicTree.segments.get("test");
        assertEquals("test", testRootNode.getTopicPart());
        assertEquals(1, testRootNode.getChildren().length);
        assertNull(testRootNode.exactSubscriptions.nonSharedSubscribersArray);
        assertNull(testRootNode.wildcardSubscriptions.nonSharedSubscribersArray);

        /*
            test/+/test node
         */
        final TopicTreeNode testWildcard = testRootNode.children[0];
        assertEquals("+", testWildcard.getTopicPart());
        assertEquals(1, testWildcard.getChildren().length);
        assertNull(testWildcard.exactSubscriptions.nonSharedSubscribersArray);
        assertEquals(1, testWildcard.wildcardSubscriptions.nonSharedSubscribersArray.length);
        assertEquals(new SubscriberWithQoS("sub4", 0, (byte) 0, null, null, null), testWildcard.wildcardSubscriptions.nonSharedSubscribersArray[0]);

         /*
            test/+ node
         */
        final TopicTreeNode testWildcardTestNode = testWildcard.children[0];
        assertEquals("test", testWildcardTestNode.getTopicPart());
        assertNull(testWildcardTestNode.getChildren());
        assertEquals(1, testWildcardTestNode.exactSubscriptions.nonSharedSubscribersArray.length);
        assertEquals(new SubscriberWithQoS("sub3", 0, (byte) 0, null, null, null), testWildcardTestNode.exactSubscriptions.nonSharedSubscribersArray[0]);
        assertNull(testWildcardTestNode.wildcardSubscriptions.nonSharedSubscribersArray);

    }

    @Test
    public void addTopic_whenAddedMultipleSingleLevelWildcardSubscriptions_thenSubscriptionsArePresentInTreeOnCorrespondingLevelsInMap() {
        InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD.set(-1);
        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("+/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub3", new Topic("test/+/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub4", new Topic("test/+/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.segments.size());

        /*
            first single level wildcard
         */
        final TopicTreeNode firstWildcardNode = topicTree.segments.get("+");
        assertEquals("+", firstWildcardNode.getTopicPart());
        assertEquals(1, firstWildcardNode.getChildren().length);
        assertEquals(1, firstWildcardNode.exactSubscriptions.nonSharedSubscribersMap.size());
        assertTrue(firstWildcardNode.exactSubscriptions.nonSharedSubscribersMap.containsValue(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null)));
        assertNull(firstWildcardNode.wildcardSubscriptions.nonSharedSubscribersMap);

        /*
            second single level wildcard
         */
        final TopicTreeNode secondWildcardNode = firstWildcardNode.children[0];
        assertEquals("+", secondWildcardNode.getTopicPart());
        assertNull(secondWildcardNode.getChildren());
        assertEquals(1, secondWildcardNode.exactSubscriptions.nonSharedSubscribersMap.size());
        assertTrue(secondWildcardNode.exactSubscriptions.nonSharedSubscribersMap.containsValue(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null)));
        assertNull(secondWildcardNode.wildcardSubscriptions.nonSharedSubscribersMap);

        /*
            test root node
         */
        final TopicTreeNode testRootNode = topicTree.segments.get("test");
        assertEquals("test", testRootNode.getTopicPart());
        assertEquals(1, testRootNode.getChildrenMap().size());
        assertNull(testRootNode.exactSubscriptions.nonSharedSubscribersMap);
        assertNull(testRootNode.wildcardSubscriptions.nonSharedSubscribersMap);

        /*
            test/+/test node
         */
        final TopicTreeNode testWildcard = testRootNode.getChildrenMap().values().iterator().next();
        assertEquals("+", testWildcard.getTopicPart());
        assertEquals(1, testWildcard.getChildren().length);
        assertNull(testWildcard.exactSubscriptions.nonSharedSubscribersMap);
        assertEquals(1, testWildcard.wildcardSubscriptions.nonSharedSubscribersMap.size());
        assertTrue(testWildcard.wildcardSubscriptions.nonSharedSubscribersMap.containsValue(new SubscriberWithQoS("sub4", 0, (byte) 0, null, null, null)));

         /*
            test/+ node
         */
        final TopicTreeNode testWildcardTestNode = testWildcard.children[0];
        assertEquals("test", testWildcardTestNode.getTopicPart());
        assertNull(testWildcardTestNode.getChildren());
        assertEquals(1, testWildcardTestNode.exactSubscriptions.nonSharedSubscribersMap.size());
        assertTrue(testWildcardTestNode.exactSubscriptions.nonSharedSubscribersMap.containsValue(new SubscriberWithQoS("sub3", 0, (byte) 0, null, null, null)));
        assertNull(testWildcardTestNode.wildcardSubscriptions.nonSharedSubscribersMap);

    }

    @Test
    public void addTopic_whenSegmentKeyLengthIsTwo_thenSubscriptionsArePresentInTree() {
        final LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("subscriber1", new Topic("topic1/1", QoS.AT_LEAST_ONCE), SubscriptionFlag.getDefaultFlags(false, false, false), null);
        topicTree.addTopic("subscriber2", new Topic("topic1/+", QoS.AT_LEAST_ONCE), SubscriptionFlag.getDefaultFlags(false, true, true), null);
        topicTree.addTopic("subscriber3", new Topic("+/1", QoS.AT_LEAST_ONCE), SubscriptionFlag.getDefaultFlags(false, false, false), null);

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.findTopicSubscribers("topic1/1").getSubscribers();
        assertEquals(3, subscribers.size());
    }
}
