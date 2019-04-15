/*
 * Copyright 2018 dc-square GmbH
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

package com.hivemq.extension.sdk.api.services.subscription;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.exception.InvalidTopicException;
import com.hivemq.extension.sdk.api.services.exception.NoSuchClientIdException;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * This service allows extensions to manage the Subscriptions for client session programmatically.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface SubscriptionStore {

    /**
     * This method adds a subscription for a certain client to a certain topic.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     * <p>
     * {@link CompletableFuture} fails with a {@link DoNotImplementException} if TopicSubscription is implemented by the
     * extension.
     * <p>
     * {@link CompletableFuture} fails with a {@link NoSuchClientIdException} if no session for this client exists.
     *
     * @param clientID     The client for which the new subscription should be added.
     * @param subscription The subscription to which the client should be subscribed.
     * @return A {@link CompletableFuture} object that will succeed, as soon as the subscription was added
     * @throws NullPointerException     If clientID or subscription is null.
     * @throws IllegalArgumentException If clientID is empty.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Void> addSubscription(@NotNull String clientID, @NotNull TopicSubscription subscription);

    /**
     * This method adds multiple subscriptions for a certain client.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     * <p>
     * {@link CompletableFuture} fails with a {@link DoNotImplementException} if any of the TopicSubscription is
     * implemented by the extension.
     * <p>
     * {@link CompletableFuture} fails with a {@link NoSuchClientIdException} if no session for this client exists.
     *
     * @param clientID      The client for which the new subscriptions should be added.
     * @param subscriptions The subscriptions to which the client should be subscribed.
     * @return A {@link CompletableFuture} object that will succeed, as soon as the subscriptions were added.
     * @throws NullPointerException     If clientID, subscriptions or one of the subscription in the set is null.
     * @throws IllegalArgumentException If clientID or subscriptions is empty.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Void> addSubscriptions(@NotNull String clientID, @NotNull Set<TopicSubscription> subscriptions);

    /**
     * This method removes a subscription for a certain client and a certain topic.
     * <p>
     * When the client or the subscription for the client does not exist, nothing happens.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     * <p>
     * {@link CompletableFuture} fails with a {@link InvalidTopicException} if the topic filter is invalid.
     *
     * @param clientID    The client for which the subscription should be removed.
     * @param topicFilter The topic from which the client should get unsubscribed.
     * @return A {@link CompletableFuture} object that will succeed, as soon as the subscription was removed.
     * @throws NullPointerException     If clientID or topicFilter is null.
     * @throws IllegalArgumentException If clientID is empty.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Void> removeSubscription(@NotNull String clientID, @NotNull String topicFilter);

    /**
     * This method removes multiple subscriptions for a certain client.
     * <p>
     * When the client does not exist, nothing happens. This also applies for subscriptions that should be removed for
     * the client, but the client has no subscription for.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     * <p>
     * {@link CompletableFuture} fails with a {@link InvalidTopicException} if any topic filter is invalid.
     *
     * @param clientID     The client for which the subscriptions should be removed.
     * @param topicFilters The topics from which the client should get unsubscribed.
     * @return A {@link CompletableFuture} object that will succeed, as soon as the subscriptions were removed.
     * @throws NullPointerException     If clientID, topics or one of the topics in the set is null.
     * @throws IllegalArgumentException If clientID or topics is empty.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Void> removeSubscriptions(@NotNull String clientID, @NotNull Set<String> topicFilters);

    /**
     * Returns all subscriptions a client is subscribed to.
     * <p>
     * The returned Set is read-only and must not be modified.
     * If the client does not exist, an empty Set is returned.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param clientID The client from where to get all subscriptions.
     * @return A {@link CompletableFuture} which contains all subscriptions the client subscribed to.
     * @throws NullPointerException     If clientID is null.
     * @throws IllegalArgumentException If clientID is empty.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Set<TopicSubscription>> getSubscriptions(@NotNull String clientID);

}
 