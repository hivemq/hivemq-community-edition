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

package com.hivemq.persistence.local;

import com.google.common.collect.ImmutableSet;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.LocalPersistence;

import java.util.Set;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
public interface ClientSessionSubscriptionLocalPersistence extends LocalPersistence {

    /**
     * Add a subscription of specific topic for a specific client to a persistence bucket.
     *
     * @param client      The client identifier of the subscriber.
     * @param topic       The {@link Topic} to add.
     * @param timestamp   The timestamp when the subscription is added.
     * @param bucketIndex The index of the bucket in which the subscription is stored.
     */
    void addSubscription(@NotNull final String client, @NotNull final Topic topic, long timestamp, int bucketIndex);

    /**
     * Add subscriptions of specific topics for a specific client to a persistence bucket.
     *
     * @param clientId    The client identifier of the subscriber.
     * @param topics      The {@link Topic}s to add.
     * @param timestamp   The timestamp when the subscriptions are added.
     * @param bucketIndex The index of the bucket in which the subscriptions are stored.
     */
    void addSubscriptions(@NotNull String clientId, @NotNull Set<Topic> topics, long timestamp, int bucketIndex);

    /**
     * Remove a subscription of specific topic for a specific client from a persistence bucket.
     *
     * @param client      The client identifier of the subscriber.
     * @param topic       The {@link Topic} to remove.
     * @param timestamp   The timestamp when the subscription is removed.
     * @param bucketIndex The index of the bucket in which the subscription is stored.
     */
    void remove(@NotNull final String client, @NotNull final String topic, long timestamp, int bucketIndex);

    /**
     * Remove subscriptions of specific topics for a specific client from a persistence bucket.
     *
     * @param clientId    The client identifier of the subscriber.
     * @param topics      The {@link Topic}s to remove.
     * @param timestamp   The timestamp when the subscriptions are removed.
     * @param bucketIndex The index of the bucket in which the subscriptions are stored.
     */
    void removeSubscriptions(@NotNull String clientId, @NotNull ImmutableSet<String> topics, long timestamp, int bucketIndex);

    /**
     * Remove all subscriptions for a specific client from a persistence bucket.
     *
     * @param client      The client identifier of the subscriber.
     * @param timestamp   The timestamp when the subscriptions are removed.
     * @param bucketIndex The index of the bucket in which the subscriptions are stored.
     */
    void removeAll(@NotNull final String client, long timestamp, int bucketIndex);

    /**
     * Trigger a cleanup for a specific persistence bucket.
     *
     * @param bucket The index of the bucket in which the subscriptions are stored.
     */
    void cleanUp(int bucket);

    /**
     * Get all subscriptions for a specific client.
     *
     * @param client The client identifier of the subscriber.
     * @return A read only set of {@link Topic}s.
     */
    @ReadOnly
    @NotNull
    ImmutableSet<Topic> getSubscriptions(@NotNull final String client);

}
