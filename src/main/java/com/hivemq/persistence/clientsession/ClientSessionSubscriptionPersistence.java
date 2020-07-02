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
package com.hivemq.persistence.clientsession;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.iteration.ChunkCursor;
import com.hivemq.extensions.iteration.MultipleChunkResult;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;

import java.util.Map;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
public interface ClientSessionSubscriptionPersistence {

    /**
     * Add a subscription for a specific client and a specific topic.
     *
     * @param client the client to add the subscription for.
     * @param topic  the topic of the subscription.
     * @return A future containing the {@link SubscriptionResult} which completes as soon as the subscription is persisted.
     */
    @NotNull
    ListenableFuture<SubscriptionResult> addSubscription(@NotNull String client, @NotNull Topic topic);

    /**
     * Get all subscriptions for a specific client.
     *
     * @param client the identifier of the client to get the subscriptions for.
     * @return A set of {@link Topic}s.
     */
    @NotNull
    @ReadOnly
    ImmutableSet<Topic> getSubscriptions(@NotNull String client);

    /**
     * Get a chunk of all the subscriptions from this node
     *
     * @param cursor the cursor returned from the last chunk or a new (empty) cursor to start iterating the persistence
     * @return a result containing the new cursor and a map of clientIds to their subscriptions
     */
    @NotNull
    ListenableFuture<MultipleChunkResult<Map<String, ImmutableSet<Topic>>>> getAllLocalSubscribersChunk(@NotNull ChunkCursor cursor);


    /**
     * Remove a subscription for a specific client and a specific topic.
     *
     * @param client the client to remove the subscription for.
     * @param topic  the topic of the subscription.
     * @return A future which completes as soon as the subscription is removed.
     */
    @NotNull
    ListenableFuture<Void> remove(@NotNull String client, @NotNull String topic);

    /**
     * Close the persistence.
     *
     * @return a future which completes as soon as the persistence is closed.
     */
    @NotNull
    ListenableFuture<Void> closeDB();

    /**
     * Add a set of {@link Topic}s as subscriptions for a specific client.
     *
     * @param clientId the client to add the subscriptions for.
     * @param topics the topics to add.
     * @return A future containing an immutable list of {@link SubscriptionResult}s which completes as soon as the subscriptions are persisted.
     */
    @NotNull
    @ReadOnly
    ListenableFuture<ImmutableList<SubscriptionResult>> addSubscriptions(@NotNull String clientId, @NotNull ImmutableSet<Topic> topics);

    /**
     * Remove a set of topics subscription for a specific client and a specific topic.
     *
     * @param clientId the client to remove the subscriptions for.
     * @param topics the topics of the subscriptions.
     *
     * @return A future which completes as soon as the subscriptions are removed.
     */
    @NotNull
    ListenableFuture<Void> removeSubscriptions(@NotNull String clientId, @NotNull ImmutableSet<String> topics);

    /**
     * Remove all subscriptions for a specific client.
     *
     * @param clientId the client to remove the subscriptions for.
     *
     * @return A future which completes as soon as the subscriptions are removed.
     */
    @NotNull
    ListenableFuture<Void> removeAll(@NotNull String clientId);

    /**
     * Removes all subscriptions for the local persistence. Topic tree entries associated with the subscription will NOT be updated!
     *
     * @param clientId for which the subscriptions should be removed
     */
    @NotNull
    ListenableFuture<Void> removeAllLocally(@NotNull String clientId);

    /**
     * Trigger a cleanup for a specific bucket
     *
     * @param bucketIndex the index of the bucket
     * @return a future which completes as soon as the clean up is done.
     */
    @NotNull
    ListenableFuture<Void> cleanUp(int bucketIndex);

    /**
     * Request all shared subscription for a given client.
     *
     * @param client for which to request the shared subscriptions.
     * @return A immutable list of the shared subscription topics
     */
    @NotNull
    @ReadOnly
    ImmutableSet<Topic> getSharedSubscriptions(@NotNull String client);

    /**
     * Invalidates Shared subscription caches for a client, and starts polling.
     * <p>
     *
     * @param clientId   the client's id.
     * @param sharedSubs the shared subscriptions to invalidate cache for.
     */
    void invalidateSharedSubscriptionCacheAndPoll(@NotNull String clientId, @NotNull ImmutableSet<Subscription> sharedSubs);


}
