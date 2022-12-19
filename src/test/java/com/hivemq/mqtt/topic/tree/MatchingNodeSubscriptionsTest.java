package com.hivemq.mqtt.topic.tree;

import com.codahale.metrics.Counter;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.Assert.assertEquals;

public class MatchingNodeSubscriptionsTest {

    private final SubscriptionCounters counters = new SubscriptionCounters(new Counter(), new Counter());

    @Test
    public void addSubscriber_whenTwoSubscribersAddedWithDefaultMapCreationThreshold_thenBothArePresent() {
        final MatchingNodeSubscriptions subscriptions = new MatchingNodeSubscriptions();
        subscriptions.addSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null), "topic", counters, 16);
        subscriptions.addSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null), "topic", counters, 16);
        assertEquals(2, subscriptions.getSubscriberCount());
        assertThat(subscriptions.getSubscribers(), hasItems(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null),
                new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test
    public void addSubscriber_whenTwoSubscribersAddedWithMapCreated_thenBothArePresent() {
        final MatchingNodeSubscriptions subscriptions = new MatchingNodeSubscriptions();
        subscriptions.addSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null), "topic", counters, 0);
        subscriptions.addSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null), "topic", counters, 0);
        assertEquals(2, subscriptions.getSubscriberCount());
        assertThat(subscriptions.getSubscribers(), hasItems(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null),
                new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test
    public void addSubscriber_whenTwoSubscribersAddedAndOneDeletedWithDefaultMapCreationThreshold_thenOneIsPresent() {
        final MatchingNodeSubscriptions subscriptions = new MatchingNodeSubscriptions();
        subscriptions.addSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null), "topic", counters, 16);
        subscriptions.addSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null), "topic", counters, 16);
        subscriptions.removeSubscriber("sub1", null, null, counters);
        assertEquals(1, subscriptions.getSubscriberCount());
        assertThat(subscriptions.getSubscribers(), hasItem(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
    }

    @Test
    public void addSubscriber_whenTwoSubscribersAddedAndOneDeleteWithMapCreated_thenOneIsPresent() {
        final MatchingNodeSubscriptions subscriptions = new MatchingNodeSubscriptions();
        subscriptions.addSubscriber(new SubscriberWithQoS("sub1", 0, (byte) 0, null, 0, null), "topic", counters, 0);
        subscriptions.addSubscriber(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null), "topic", counters, 0);
        subscriptions.removeSubscriber("sub1", null, null, counters);
        assertEquals(1, subscriptions.getSubscriberCount());
    }

}