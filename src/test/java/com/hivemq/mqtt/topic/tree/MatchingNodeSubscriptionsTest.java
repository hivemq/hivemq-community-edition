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
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.Assert.assertEquals;

public class MatchingNodeSubscriptionsTest {

    private final SubscriptionCounters counters = new SubscriptionCounters(new Counter());

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