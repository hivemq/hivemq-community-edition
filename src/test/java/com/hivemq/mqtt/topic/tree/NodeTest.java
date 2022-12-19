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
        assertEquals(0, TopicTreeImpl.getChildrenCount(node));
    }

    @Test
    public void addChildNodeIfAbsent_whenTwoChildrenAreAddedToNode_thenTwoChildrenArePresentInTheNode() {
        node.addChildNodeIfAbsent(new TopicTreeNode("first"), 1);
        node.addChildNodeIfAbsent(new TopicTreeNode("second"), 1);
        assertEquals(2, TopicTreeImpl.getChildrenCount(node));
    }

    @Test
    public void addChildNodeIfAbsent_whenOneOfTwoChildIsNulled_thenOneChildRemains() {
        node.addChildNodeIfAbsent(new TopicTreeNode("first"), 1);
        node.addChildNodeIfAbsent(new TopicTreeNode("second"), 1);
        assertNotNull(node.getChildren());
        assertEquals(2, node.getChildren().length);
        node.getChildren()[0] = null;
        assertEquals(1, TopicTreeImpl.getChildrenCount(node));
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
