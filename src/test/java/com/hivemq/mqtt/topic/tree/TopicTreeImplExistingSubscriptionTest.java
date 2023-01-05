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
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.Topic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

/**
 * @author Christoph Schäbel
 */
public class TopicTreeImplExistingSubscriptionTest {

    private LocalTopicTree topicTree;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD.set(1);
        topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));

    }

    @Test
    public void test_existing_root_wildcard() {

        assertFalse(topicTree.addTopic("client", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null));

        assertTrue(topicTree.addTopic("client2", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertTrue(topicTree.addTopic("client", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null));

    }

    @Test
    public void test_existing_wildcard() {

        assertFalse(topicTree.addTopic("client", new Topic("a/#", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("topic/#", QoS.EXACTLY_ONCE), (byte) 0, null));

        assertTrue(topicTree.addTopic("client2", new Topic("topic/#", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("a/#", QoS.EXACTLY_ONCE), (byte) 0, null));

        assertTrue(topicTree.addTopic("client", new Topic("a/#", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("a", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("a/", QoS.EXACTLY_ONCE), (byte) 0, null));
    }

    @Test
    public void test_existing_plus_wildcard() {

        assertFalse(topicTree.addTopic("client", new Topic("+/a", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("topic/+/a", QoS.EXACTLY_ONCE), (byte) 0, null));

        assertFalse(topicTree.addTopic("client2", new Topic("topic/+/b", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertTrue(topicTree.addTopic("client2", new Topic("topic/+/a", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("a/#", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null));

        assertTrue(topicTree.addTopic("client", new Topic("+/a", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("b/a", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("b/#", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null));
    }

    @Test
    public void test_existing_no_wildcard() {

        assertFalse(topicTree.addTopic("client", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("topic/a", QoS.EXACTLY_ONCE), (byte) 0, null));

        assertTrue(topicTree.addTopic("client2", new Topic("topic/a", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("topic/+/b", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("a/#", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client2", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null));

        assertTrue(topicTree.addTopic("client", new Topic("a/b", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("+/a", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("b/a", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("b/#", QoS.EXACTLY_ONCE), (byte) 0, null));
        assertFalse(topicTree.addTopic("client", new Topic("#", QoS.EXACTLY_ONCE), (byte) 0, null));
    }

}