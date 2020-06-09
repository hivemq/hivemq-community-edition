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
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Dominik Obermaier
 */
public class NodeUtilsTest {

    private Counter subscriptionCounter;
    private AtomicInteger segmentSubscriptionCounter;

    @Before
    public void before() {
        subscriptionCounter = new Counter();
        segmentSubscriptionCounter = new AtomicInteger();
    }

    @Test
    public void test_count_node_no_children() throws Exception {
        final Node node = new Node("node", 1, 16, subscriptionCounter, segmentSubscriptionCounter);
        assertEquals(0, NodeUtils.getChildrenCount(node));
    }

    @Test
    public void test_count_two_children() throws Exception {
        final Node node = new Node("node", 1, 16, subscriptionCounter, segmentSubscriptionCounter);
        node.addIfAbsent(new Node("first", 1, 16, subscriptionCounter, segmentSubscriptionCounter));
        node.addIfAbsent(new Node("second", 1, 16, subscriptionCounter, segmentSubscriptionCounter));
        assertEquals(2, NodeUtils.getChildrenCount(node));
    }

    @Test
    public void test_count_children_deleted() throws Exception {
        final Node node = new Node("node", 1, 16, subscriptionCounter, segmentSubscriptionCounter);
        node.addIfAbsent(new Node("first", 1, 16, subscriptionCounter, segmentSubscriptionCounter));
        node.addIfAbsent(new Node("second", 1, 16, subscriptionCounter, segmentSubscriptionCounter));
        node.getChildren()[0] = null;
        assertEquals(1, NodeUtils.getChildrenCount(node));
    }

    @Test(expected = NullPointerException.class)
    public void test_count_children_null() throws Exception {
        NodeUtils.getChildrenCount(null);
    }


    @Test
    public void test_exact_subscribers_no_subscribers() throws Exception {
        final Node node = new Node("node", 1, 16, subscriptionCounter, segmentSubscriptionCounter);
        assertEquals(0, NodeUtils.getExactSubscriberCount(node));
        assertEquals(0, NodeUtils.getExactSubscribers(node).size());
    }

    @Test
    public void test_two_exact_subscribers() throws Exception {
        final Node node = new Node("node", 1, 16, subscriptionCounter, segmentSubscriptionCounter);
        node.addExactSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null));
        node.addExactSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null));
        assertEquals(2, NodeUtils.getExactSubscriberCount(node));
        assertThat(NodeUtils.getExactSubscribers(node), hasItems(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null),
                new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test
    public void test_two_exact_subscribers_map() throws Exception {
        final Node node = new Node("node", 1, 0, subscriptionCounter, segmentSubscriptionCounter);
        node.addExactSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null));
        node.addExactSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null));
        assertEquals(2, NodeUtils.getExactSubscriberCount(node));
        assertThat(NodeUtils.getExactSubscribers(node), hasItems(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null),
                new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test
    public void test_exact_subscribers_sub_deleted() throws Exception {
        final Node node = new Node("node", 1, 16, subscriptionCounter, segmentSubscriptionCounter);
        node.addExactSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null));
        node.addExactSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null));
        node.removeExactSubscriber("sub1", null);
        assertEquals(1, NodeUtils.getExactSubscriberCount(node));
        assertThat(NodeUtils.getExactSubscribers(node), hasItem(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test
    public void test_exact_subscribers_sub_deleted_map() throws Exception {
        final Node node = new Node("node", 1, 0, subscriptionCounter, segmentSubscriptionCounter);
        node.addExactSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null));
        node.addExactSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null));
        node.removeExactSubscriber("sub1", null);
        assertEquals(1, NodeUtils.getExactSubscriberCount(node));
    }

    @Test(expected = NullPointerException.class)
    public void test_count_exact_subscribers_null() throws Exception {
        NodeUtils.getExactSubscriberCount(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_get_exact_subscribers_null() throws Exception {
        NodeUtils.getExactSubscribers(null);
    }


    @Test
    public void test_wildcard_subscribers_no_subscribers() throws Exception {
        final Node node = new Node("node", 1, 16, subscriptionCounter, segmentSubscriptionCounter);
        assertEquals(0, NodeUtils.getWildcardSubscriberCount(node));
        assertEquals(0, NodeUtils.getWildcardSubscribers(node).size());
    }

    @Test
    public void test_two_wildcard_subscribers() throws Exception {
        final Node node = new Node("node", 1, 16, subscriptionCounter, segmentSubscriptionCounter);
        node.addWildcardSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null));
        node.addWildcardSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null));
        assertEquals(2, NodeUtils.getWildcardSubscriberCount(node));
        assertThat(NodeUtils.getWildcardSubscribers(node), hasItems(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null),
                new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test
    public void test_two_wildcard_subscribers_map() throws Exception {
        final Node node = new Node("node", 1, 0, subscriptionCounter, segmentSubscriptionCounter);
        node.addWildcardSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null));
        node.addWildcardSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null));
        assertEquals(2, NodeUtils.getWildcardSubscriberCount(node));
        assertThat(NodeUtils.getWildcardSubscribers(node), hasItems(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null),
                new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test
    public void test_wildcard_subscribers_sub_deleted() throws Exception {
        final Node node = new Node("node", 1, 16, subscriptionCounter, segmentSubscriptionCounter);
        node.addWildcardSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null));
        node.addWildcardSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null));
        node.removeWildcardSubscriber("sub1", null);
        assertEquals(1, NodeUtils.getWildcardSubscriberCount(node));
        assertThat(NodeUtils.getWildcardSubscribers(node), hasItem(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test
    public void test_wildcard_subscribers_sub_deleted_map() throws Exception {
        final Node node = new Node("node", 1, 0, subscriptionCounter, segmentSubscriptionCounter);
        node.addWildcardSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null));
        node.addWildcardSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null));
        node.removeWildcardSubscriber("sub1", null);
        assertEquals(1, NodeUtils.getWildcardSubscriberCount(node));
        assertThat(NodeUtils.getWildcardSubscribers(node), hasItem(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test(expected = NullPointerException.class)
    public void test_count_wildcard_subscribers_null() throws Exception {
        NodeUtils.getWildcardSubscriberCount(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_get_wildcard_subscribers_null() throws Exception {
        NodeUtils.getWildcardSubscribers(null);
    }
}