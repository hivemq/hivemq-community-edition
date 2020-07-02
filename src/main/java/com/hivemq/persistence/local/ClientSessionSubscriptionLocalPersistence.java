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
package com.hivemq.persistence.local;

import com.google.common.collect.ImmutableSet;
import com.hivemq.annotations.ExecuteInSingleWriter;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.LocalPersistence;

import java.util.Map;

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
    @ExecuteInSingleWriter
    void addSubscription(@NotNull final String client, @NotNull final Topic topic, long timestamp, int bucketIndex);

    /**
     * Add subscriptions of specific topics for a specific client to a persistence bucket.
     *
     * @param clientId    The client identifier of the subscriber.
     * @param topics      The {@link Topic}s to add.
     * @param timestamp   The timestamp when the subscriptions are added.
     * @param bucketIndex The index of the bucket in which the subscriptions are stored.
     */
    @ExecuteInSingleWriter
    void addSubscriptions(@NotNull String clientId, @NotNull ImmutableSet<Topic> topics, long timestamp, int bucketIndex);

    /**
     * Remove a subscription of specific topic for a specific client from a persistence bucket.
     *
     * @param client      The client identifier of the subscriber.
     * @param topic       The {@link Topic} to remove.
     * @param timestamp   The timestamp when the subscription is removed.
     * @param bucketIndex The index of the bucket in which the subscription is stored.
     */
    @ExecuteInSingleWriter
    void remove(@NotNull final String client, @NotNull final String topic, long timestamp, int bucketIndex);

    /**
     * Remove subscriptions of specific topics for a specific client from a persistence bucket.
     *
     * @param clientId    The client identifier of the subscriber.
     * @param topics      The {@link Topic}s to remove.
     * @param timestamp   The timestamp when the subscriptions are removed.
     * @param bucketIndex The index of the bucket in which the subscriptions are stored.
     */
    @ExecuteInSingleWriter
    void removeSubscriptions(@NotNull String clientId, @NotNull ImmutableSet<String> topics, long timestamp, int bucketIndex);

    /**
     * Remove all subscriptions for a specific client from a persistence bucket.
     *
     * @param client      The client identifier of the subscriber.
     * @param timestamp   The timestamp when the subscriptions are removed.
     * @param bucketIndex The index of the bucket in which the subscriptions are stored.
     */
    @ExecuteInSingleWriter
    void removeAll(@NotNull final String client, long timestamp, int bucketIndex);

    /**
     * Trigger a cleanup for a specific persistence bucket.
     *
     * @param bucket The index of the bucket in which the subscriptions are stored.
     */
    @ExecuteInSingleWriter
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

    /**
     * Get a chunk of subscriptions.
     *
     * @param bucketIndex  the bucket index
     * @param lastClientId the last client identifier for this chunk. Pass <code>null</code> to start at the beginning.
     * @param maxResults   the max amount of results contained in the chunk (can be exceeded). Subscriptions (not
     *                     clientids) are counted here. When the subscription count for one client
     *                     exceeds the limit, it will contain all subscriptions for this client anyway.
     * @return a {@link BucketChunkResult} with the entries and the information if more chunks are available
     */
    @NotNull
    BucketChunkResult<Map<String, ImmutableSet<Topic>>> getAllSubscribersChunk(int bucketIndex, @Nullable String lastClientId, int maxResults);
}
