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
import com.hivemq.mqtt.topic.SubscriptionFlags;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Dominik Obermaier
 */
public class TestAddToTopicTreeImpl {

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD.set(1);
    }

    @Test(expected = NullPointerException.class)
    public void test_subscriber_null() throws Exception {
        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic(null, new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
    }

    @Test(expected = NullPointerException.class)
    public void test_topic_null() throws Exception {
        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("subscriber", null, (byte) 0, null);
    }

    @Test
    public void test_one_firstlevel_node() throws Exception {
        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("topic", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Collection<SegmentRootNode> firstTopicLevel = topicTree.segments.values();
        assertEquals(1, firstTopicLevel.size());

        final Node firstLevelNode = firstTopicLevel.iterator().next();
        assertNull(firstLevelNode.getChildren());
        assertEquals("topic", firstLevelNode.getTopicPart());


        //No wildcard subscribers
        assertNull(firstLevelNode.getWildcardSubscribers());

        //Now test that the subscribers are actually here
        final SubscriberWithQoS[] exactSubscribers = firstLevelNode.getExactSubscribers();
        assertEquals(2, exactSubscribers.length);
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), exactSubscribers[0]);
        assertEquals(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null), exactSubscribers[1]);
    }

    @Test
    public void test_treewildcard_on_first_level() throws Exception {
        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(0, topicTree.segments.values().size());

        assertEquals(1, topicTree.rootWildcardSubscribers.size());
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), topicTree.rootWildcardSubscribers.get(0));
    }

    @Test
    public void test_treewildcard_on_second_level() throws Exception {
        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("topic/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(1, topicTree.segments.size());

        final Node wildcardNode = topicTree.segments.values().iterator().next();
        assertNull(wildcardNode.getChildren());
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), wildcardNode.getWildcardSubscribers()[0]);
        assertNull(wildcardNode.getExactSubscribers());

        assertEquals(0, topicTree.rootWildcardSubscribers.size());
    }

    @Test
    public void test_more_firstlevel_nodes() throws Exception {
        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("a", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub3", new Topic("b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub4", new Topic("c", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Collection<SegmentRootNode> firstTopicLevel = topicTree.segments.values();
        assertEquals(3, firstTopicLevel.size());

        /*
            Test the a Node
        */
        final Node aNode = topicTree.segments.get("a");
        assertEquals("a", aNode.getTopicPart());
        assertNull(aNode.getChildren());


        assertNull(aNode.getWildcardSubscribers());

        final SubscriberWithQoS[] exactSubscribers = aNode.getExactSubscribers();
        assertEquals(1, exactSubscribers.length);
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), exactSubscribers[0]);

        /*
            Test the b Node
        */

        final Node bNode = topicTree.segments.get("b");
        assertEquals("b", bNode.getTopicPart());
        assertNull(bNode.getChildren());

        assertNull(bNode.getWildcardSubscribers());

        final SubscriberWithQoS[] bNodeExactSubscribers = bNode.getExactSubscribers();
        assertEquals(2, bNodeExactSubscribers.length);
        assertEquals(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null), bNodeExactSubscribers[0]);
        assertEquals(new SubscriberWithQoS("sub3", 0, (byte) 0, null, null, null), bNodeExactSubscribers[1]);

        /*
            Test the c Node
        */
        final Node cNode = topicTree.segments.get("c");
        assertEquals("c", cNode.getTopicPart());
        assertNull(cNode.getChildren());


        assertNull(cNode.getWildcardSubscribers());

        final SubscriberWithQoS[] cNodeExactSubscribers = cNode.getExactSubscribers();
        assertEquals(1, cNodeExactSubscribers.length);
        assertEquals(new SubscriberWithQoS("sub4", 0, (byte) 0, null, null, null), cNodeExactSubscribers[0]);
    }

    @Test
    public void test_more_firstlevel_nodes_subscriber_map() throws Exception {
        InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD.set(-1);

        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("a", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub3", new Topic("b", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub4", new Topic("c", QoS.AT_MOST_ONCE), (byte) 0, null);

        final Collection<SegmentRootNode> firstTopicLevel = topicTree.segments.values();
        assertEquals(3, firstTopicLevel.size());

        /*
            Test the a Node
        */
        final Node aNode = topicTree.segments.get("a");
        assertEquals("a", aNode.getTopicPart());
        assertNull(aNode.getChildren());


        assertNull(aNode.getWildcardSubscribers());

        assertEquals(1, aNode.exactSubscriberMap.values().size());
        assertEquals(true, aNode.exactSubscriberMap.values().contains(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null)));

        /*
            Test the b Node
        */

        final Node bNode = topicTree.segments.get("b");
        assertEquals("b", bNode.getTopicPart());
        assertNull(bNode.getChildren());

        assertNull(bNode.getWildcardSubscribers());

        assertEquals(2, bNode.exactSubscriberMap.values().size());
        assertEquals(true, bNode.exactSubscriberMap.values().contains(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null)));
        assertEquals(true, bNode.exactSubscriberMap.values().contains(new SubscriberWithQoS("sub3", 0, (byte) 0, null, null, null)));

        /*
            Test the c Node
        */
        final Node cNode = topicTree.segments.get("c");
        assertEquals("c", cNode.getTopicPart());
        assertNull(cNode.getChildren());


        assertNull(cNode.getWildcardSubscribers());

        assertEquals(1, cNode.exactSubscriberMap.size());
        assertEquals(true, cNode.exactSubscriberMap.values().contains(new SubscriberWithQoS("sub4", 0, (byte) 0, null, null, null)));
    }

    @Test
    public void test_single_level_wildcard() throws Exception {
        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("+/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub3", new Topic("test/+/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub4", new Topic("test/+/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.segments.size());

        /*
            first single level wildcard
         */
        final Node firstWildcardNode = topicTree.segments.get("+");
        assertEquals("+", firstWildcardNode.getTopicPart());
        assertEquals(1, firstWildcardNode.getChildren().length);
        assertEquals(1, firstWildcardNode.getExactSubscribers().length);
        assertEquals(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null), firstWildcardNode.getExactSubscribers()[0]);
        assertNull(firstWildcardNode.getWildcardSubscribers());

        /*
            second single level wildcard
         */
        final Node secondWildcardNode = firstWildcardNode.children[0];
        assertEquals("+", secondWildcardNode.getTopicPart());
        assertNull(secondWildcardNode.getChildren());
        assertEquals(1, secondWildcardNode.getExactSubscribers().length);
        assertEquals(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null), secondWildcardNode.getExactSubscribers()[0]);
        assertNull(secondWildcardNode.getWildcardSubscribers());

        /*
            test root node
         */
        final Node testRootNode = topicTree.segments.get("test");
        assertEquals("test", testRootNode.getTopicPart());
        assertEquals(1, testRootNode.getChildren().length);
        assertNull(testRootNode.getExactSubscribers());
        assertNull(testRootNode.getWildcardSubscribers());

        /*
            test/+/test node
         */
        final Node testWildcard = testRootNode.children[0];
        assertEquals("+", testWildcard.getTopicPart());
        assertEquals(1, testWildcard.getChildren().length);
        assertNull(testWildcard.getExactSubscribers());
        assertEquals(1, testWildcard.getWildcardSubscribers().length);
        assertEquals(new SubscriberWithQoS("sub4", 0, (byte) 0, null, null, null), testWildcard.getWildcardSubscribers()[0]);

         /*
            test/+ node
         */
        final Node testWildcardTestNode = testWildcard.children[0];
        assertEquals("test", testWildcardTestNode.getTopicPart());
        assertNull(testWildcardTestNode.getChildren());
        assertEquals(1, testWildcardTestNode.getExactSubscribers().length);
        assertEquals(new SubscriberWithQoS("sub3", 0, (byte) 0, null, null, null), testWildcardTestNode.getExactSubscribers()[0]);
        assertNull(testWildcardTestNode.getWildcardSubscribers());

    }

    @Test
    public void test_single_level_wildcard_subscriber_map() throws Exception {
        InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD.set(-1);
        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("sub1", new Topic("+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub2", new Topic("+/+", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub3", new Topic("test/+/test", QoS.AT_MOST_ONCE), (byte) 0, null);
        topicTree.addTopic("sub4", new Topic("test/+/#", QoS.AT_MOST_ONCE), (byte) 0, null);

        assertEquals(2, topicTree.segments.size());

        /*
            first single level wildcard
         */
        final Node firstWildcardNode = topicTree.segments.get("+");
        assertEquals("+", firstWildcardNode.getTopicPart());
        assertEquals(1, firstWildcardNode.getChildren().length);
        assertEquals(1, firstWildcardNode.exactSubscriberMap.size());
        assertEquals(true, firstWildcardNode.exactSubscriberMap.values().contains(new SubscriberWithQoS("sub1", 0, (byte) 0, null, null, null)));
        assertNull(firstWildcardNode.wildcardSubscriberMap);

        /*
            second single level wildcard
         */
        final Node secondWildcardNode = firstWildcardNode.children[0];
        assertEquals("+", secondWildcardNode.getTopicPart());
        assertNull(secondWildcardNode.getChildren());
        assertEquals(1, secondWildcardNode.exactSubscriberMap.size());
        assertEquals(true, secondWildcardNode.exactSubscriberMap.values().contains(new SubscriberWithQoS("sub2", 0, (byte) 0, null, null, null)));
        assertNull(secondWildcardNode.wildcardSubscriberMap);

        /*
            test root node
         */
        final Node testRootNode = topicTree.segments.get("test");
        assertEquals("test", testRootNode.getTopicPart());
        assertEquals(1, testRootNode.getChildrenMap().size());
        assertNull(testRootNode.exactSubscriberMap);
        assertNull(testRootNode.wildcardSubscriberMap);

        /*
            test/+/test node
         */
        final Node testWildcard = testRootNode.getChildrenMap().values().iterator().next();
        assertEquals("+", testWildcard.getTopicPart());
        assertEquals(1, testWildcard.getChildren().length);
        assertNull(testWildcard.exactSubscriberMap);
        assertEquals(1, testWildcard.wildcardSubscriberMap.size());
        assertEquals(true, testWildcard.wildcardSubscriberMap.values().contains(new SubscriberWithQoS("sub4", 0, (byte) 0, null, null, null)));

         /*
            test/+ node
         */
        final Node testWildcardTestNode = testWildcard.children[0];
        assertEquals("test", testWildcardTestNode.getTopicPart());
        assertNull(testWildcardTestNode.getChildren());
        assertEquals(1, testWildcardTestNode.exactSubscriberMap.size());
        assertEquals(true, testWildcardTestNode.exactSubscriberMap.values().contains(new SubscriberWithQoS("sub3", 0, (byte) 0, null, null, null)));
        assertNull(testWildcardTestNode.wildcardSubscriberMap);

    }

    @Test
    public void test_topic_key_length() throws Exception {
        final TopicTreeImpl topicTree = new TopicTreeImpl(new MetricsHolder(new MetricRegistry()));
        topicTree.addTopic("subscriber1", new Topic("topic1/1", QoS.AT_LEAST_ONCE), SubscriptionFlags.getDefaultFlags(false, false, false), null);
        topicTree.addTopic("subscriber2", new Topic("topic1/+", QoS.AT_LEAST_ONCE), SubscriptionFlags.getDefaultFlags(false, true, true), null);
        topicTree.addTopic("subscriber3", new Topic("+/1", QoS.AT_LEAST_ONCE), SubscriptionFlags.getDefaultFlags(false, false, false), null);

        final ImmutableSet<SubscriberWithIdentifiers> subscribers = topicTree.getSubscribers("topic1/1");
        assertEquals(3, subscribers.size());
    }
}
