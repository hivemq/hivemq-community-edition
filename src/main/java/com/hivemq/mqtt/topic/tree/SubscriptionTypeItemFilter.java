package com.hivemq.mqtt.topic.tree;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionType;
import com.hivemq.mqtt.topic.SubscriberWithQoS;

public class SubscriptionTypeItemFilter implements LocalTopicTree.ItemFilter {

    @NotNull
    private final SubscriptionType subscriptionType;

    public SubscriptionTypeItemFilter(@NotNull final SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    @Override
    public boolean checkItem(@NotNull final SubscriberWithQoS subscriber) {
        switch (subscriptionType) {
            case ALL:
                return true;
            case INDIVIDUAL:
                return !subscriber.isSharedSubscription();
            case SHARED:
                return subscriber.isSharedSubscription();
        }
        //to support potential new types
        return false;
    }
}
