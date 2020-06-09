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

import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.SubscriberWithQoS;

/**
 * @author Lukas Brandl
 * @author Christoph Schäbel
 */
public interface LocalTopicTree {

    boolean addTopic(@NotNull String subscriber, @NotNull Topic topic, byte flags, @Nullable String sharedGroup);

    /**
     * All subscribers for a topic (PUBLISH)
     *
     * @param topic the topic to publish to (no wildcards)
     * @return the subscribers interested in this topic with all their identifiers
     */
    @NotNull
    ImmutableSet<SubscriberWithIdentifiers> getSubscribers(@NotNull String topic);

    /**
     * All subscribers that have subscribed to this exact topic filter
     *
     * @param topicFilter the topic filter (including wildcards)
     * @return the subscribers with a subscription with this topic filter
     */
    @NotNull
    ImmutableSet<String> getSubscribersWithFilter(@NotNull String topicFilter, @NotNull ItemFilter itemFilter);

    /**
     * All subscribers that have a subscription matching this topic
     *
     * @param topic the topic to match (no wildcards)
     * @param itemFilter
     * @param excludeRootLevelWildcard
     * @return the subscribers with a subscription for this topic
     */
    @NotNull
    ImmutableSet<String> getSubscribersForTopic(@NotNull String topic, @NotNull ItemFilter itemFilter, boolean excludeRootLevelWildcard);

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


    interface ItemFilter {
        /**
         * is called for each subscriber that matches the specified topic / topic filter
         *
         * @param subscriber the current subscriber
         * @return true if the current subscriber should be added to the result set, false if not
         */
        boolean checkItem(@NotNull final SubscriberWithQoS subscriber);
    }
}
