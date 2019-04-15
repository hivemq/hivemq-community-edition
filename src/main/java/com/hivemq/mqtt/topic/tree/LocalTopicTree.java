/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.mqtt.topic.tree;

import com.google.common.collect.ImmutableSet;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.SubscriberWithQoS;

/**
 * @author Lukas Brandl
 */
public interface LocalTopicTree {

    boolean addTopic(@NotNull String subscriber, @NotNull Topic topic, byte flags, @Nullable String sharedGroup);

    @NotNull
    ImmutableSet<SubscriberWithIdentifiers> getSubscribers(@NotNull String topic);

    @NotNull
    ImmutableSet<SubscriberWithIdentifiers> getSubscribers(@NotNull String topic, boolean excludeRootLevelWildcard);

    /**
     * Remove a subscription for a client
     *
     * @param subscriber for which the subscription should be removed
     * @param topic of the subscription
     * @param sharedName of the subscription or null if it is not a shared subscription
     */
    void removeSubscriber(@NotNull String subscriber, @NotNull String topic, @Nullable String sharedName);

    /**
     * Returns all subscriber that share a given subscription.
     *
     * @param group       of the shared subscription
     * @param topicFilter of the shared subscription
     * @return all subscriber that share the given subscription
     */
    @NotNull
    ImmutableSet<SubscriberWithQoS> getSharedSubscriber(@NotNull String group, @NotNull String topicFilter);

    /**
     * Get the subscription for a specific client matching a given topic.
     * The returned subscription has the highest qos of all matching subscriptions and all its identifiers.
     *
     * @param client of the subscription
     * @param topic  matching the subscriptions
     * @return {@link SubscriberWithIdentifiers} with the highest QoS and all identifiers of the matching subscriptions
     * or null if no subscription match for the client
     */
    @Nullable
    SubscriberWithIdentifiers getSubscriber(@NotNull String client, @NotNull String topic);
}
