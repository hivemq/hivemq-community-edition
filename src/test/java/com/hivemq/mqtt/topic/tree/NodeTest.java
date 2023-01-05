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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NodeTest {

    private TopicTreeNode node;

    @Before
    public void setUp() {
        node = new TopicTreeNode("node");
    }

    @Test
    public void constructor_whenNodeCreated_thenItHasNoChildren() {
        assertEquals(0, LocalTopicTree.getChildrenCount(node));
    }

    @Test
    public void addChildNodeIfAbsent_whenTwoChildrenAreAddedToNode_thenTwoChildrenArePresentInTheNode() {
        node.addChildNodeIfAbsent("first", 1);
        node.addChildNodeIfAbsent("second", 1);
        assertEquals(2, LocalTopicTree.getChildrenCount(node));
    }

    @Test
    public void addChildNodeIfAbsent_whenOneOfTwoChildIsNulled_thenOneChildRemains() {
        node.addChildNodeIfAbsent("first", 1);
        node.addChildNodeIfAbsent("second", 1);
        assertNotNull(node.getChildren());
        assertEquals(2, node.getChildren().length);
        node.getChildren()[0] = null;
        assertEquals(1, LocalTopicTree.getChildrenCount(node));
    }

    @Test
    public void constructor_whenNodeCreated_thenItHasNoExactSubscriptions() {
        assertEquals(0, node.exactSubscriptions.getSubscriberCount());
        assertEquals(0, node.exactSubscriptions.getSubscribers().size());
    }

    @Test
    public void constructor_whenNodeCreated_thenItHasNoWildcardSubscriptions() {
        assertEquals(0, node.wildcardSubscriptions.getSubscriberCount());
        assertEquals(0, node.wildcardSubscriptions.getSubscribers().size());
    }
}
