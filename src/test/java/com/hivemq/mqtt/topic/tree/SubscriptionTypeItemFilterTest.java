package com.hivemq.mqtt.topic.tree;

import com.hivemq.extension.sdk.api.services.subscription.SubscriptionType;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.SubscriptionFlags;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubscriptionTypeItemFilterTest {

    @Test
    public void test_mode_all() {

        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);
        final byte individualFlag = SubscriptionFlags.getDefaultFlags(false, false, false);

        final SubscriptionTypeItemFilter itemFilter = new SubscriptionTypeItemFilter(SubscriptionType.ALL);

        assertTrue(itemFilter.checkItem(new SubscriberWithQoS("client", 0, individualFlag, 0)));
        assertTrue(itemFilter.checkItem(new SubscriberWithQoS("client", 0, sharedFlag, 0)));
    }

    @Test
    public void test_mode_individual() {

        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);
        final byte individualFlag = SubscriptionFlags.getDefaultFlags(false, false, false);

        final SubscriptionTypeItemFilter itemFilter = new SubscriptionTypeItemFilter(SubscriptionType.INDIVIDUAL);

        assertTrue(itemFilter.checkItem(new SubscriberWithQoS("client", 0, individualFlag, 0)));
        assertFalse(itemFilter.checkItem(new SubscriberWithQoS("client", 0, sharedFlag, 0)));
    }

    @Test
    public void test_mode_shared() {

        final byte sharedFlag = SubscriptionFlags.getDefaultFlags(true, false, false);
        final byte individualFlag = SubscriptionFlags.getDefaultFlags(false, false, false);

        final SubscriptionTypeItemFilter itemFilter = new SubscriptionTypeItemFilter(SubscriptionType.SHARED);

        assertFalse(itemFilter.checkItem(new SubscriberWithQoS("client", 0, individualFlag, 0)));
        assertTrue(itemFilter.checkItem(new SubscriberWithQoS("client", 0, sharedFlag, 0)));
    }

}
