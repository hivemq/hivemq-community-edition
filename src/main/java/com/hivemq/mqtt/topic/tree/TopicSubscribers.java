package com.hivemq.mqtt.topic.tree;

import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;

/**
 * @author Till Seeberger
 */
public class TopicSubscribers {

    private final ImmutableSet<SubscriberWithIdentifiers> subscriber;
    private final ImmutableSet<String> sharedSubscriptions;

    public TopicSubscribers(@NotNull final ImmutableSet<SubscriberWithIdentifiers> subscriber,
                            @NotNull final ImmutableSet<String> sharedSubscriptions) {
        this.subscriber = subscriber;
        this.sharedSubscriptions = sharedSubscriptions;
    }

    @NotNull
    public ImmutableSet<SubscriberWithIdentifiers> getSubscribers() { return subscriber; }

    @NotNull
    public ImmutableSet<String> getSharedSubscriptions() { return sharedSubscriptions; }
}