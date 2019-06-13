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
import com.hivemq.extension.sdk.api.services.ManagedExtensionExecutorService;
import com.hivemq.extension.sdk.api.services.exception.*;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
     * @return A {@link CompletableFuture} object that will succeed, as soon as the subscription was added by all cluster nodes.
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

    /**
     * Iterate over all subscribers that have a subscription that matches the the passed topic.
     * <p>
     * This will iterate all subscribers including individual and shared subscriptions.
     * To filter the result use the overloaded methods and pass a {@link SubscriptionType}.
     * <p>
     * The callback is executed in the {@link ManagedExtensionExecutorService} per default.
     * Use the overloaded methods to pass a custom executor for the callback.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread of
     * this method.
     * <p>
     *
     * @param topic    the topic to check for (no wildcards allowed). Same topic that is used in a MQTT PUBLISH message
     * @param callback a {@link IterationCallback} that is called for every returned result.
     * @return a {@link CompletableFuture} that is completed after all iterations are executed
     * @throws NullPointerException     if the passed topic or callback are null
     * @throws IllegalArgumentException if the passed topic is not a valid topic or contains wildcards
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(@NotNull String topic, @NotNull IterationCallback<SubscriberForTopicResult> callback);

    /**
     * Iterate over all subscribers that have a subscription that matches the the passed topic.
     * <p>
     * This method will iterate all subscribers according to the passed {@link SubscriptionType}.
     * <p>
     * The callback is executed in the {@link ManagedExtensionExecutorService} per default.
     * Use the overloaded methods to pass a custom executor for the callback.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread of
     * this method.
     * <p>
     *
     * @param topic            the topic to check for (no wildcards allowed). Same topic that is used in a MQTT PUBLISH
     *                         message
     * @param subscriptionType a {@link SubscriptionType} to filter only individual or shared subscriptions, or both.
     * @param callback         a {@link IterationCallback} that is called for every returned result.
     * @return a {@link CompletableFuture} that is completed after all iterations are executed
     * @throws NullPointerException     if the passed topic, subscriptionType or callback are null
     * @throws IllegalArgumentException if the passed topic is not a valid topic or contains wildcards
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(@NotNull String topic, @NotNull SubscriptionType subscriptionType, @NotNull IterationCallback<SubscriberForTopicResult> callback);


    /**
     * Iterate over all subscribers that have a subscription that matches the the passed topic.
     * <p>
     * This method will iterate all subscribers including individual and shared subscriptions.
     * To filter the result use the overloaded methods and pass a {@link SubscriptionType}.
     * <p>
     * The callback is executed in the passed {@link Executor}
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread of
     * this method.
     * <p>
     * @param topic            the topic to check for (no wildcards allowed). Same topic that is used in a MQTT PUBLISH
     *                         message
     * @param callback         a {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor a {@link Executor} in which the callback for each iteration is executed
     * @return a {@link CompletableFuture} that is completed after all iterations are executed
     * @throws NullPointerException     if the passed topic, subscriptionType or callback are null
     * @throws IllegalArgumentException if the passed topic is not a valid topic or contains wildcards
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(@NotNull String topic, @NotNull IterationCallback<SubscriberForTopicResult> callback, @NotNull Executor callbackExecutor);

    /**
     * Iterate over all subscribers that have a subscription that matches the the passed topic.
     * <p>
     * This method will iterate all subscribers according to the passed {@link SubscriptionType}.
     * <p>
     * The callback is executed in the passed {@link Executor}
     * If you want to collect the results of each execution of the callback in a collectio please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread of
     * this method.
     * <p>
     *
     * @param topic            the topic to check for (no wildcards allowed). Same topic that is used in a MQTT PUBLISH
     *                         message
     * @param subscriptionType a {@link SubscriptionType} to filter only individual or shared subscriptions, or both.
     * @param callback         a {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor a {@link Executor} in which the callback for each iteration is executed
     * @return a {@link CompletableFuture} that is completed after all iterations are executed
     * @throws NullPointerException     if the passed topic, subscriptionType, callback or callbackExecutor are null
     * @throws IllegalArgumentException if the passed topic is not a valid topic or contains wildcards
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(@NotNull String topic, @NotNull SubscriptionType subscriptionType, @NotNull IterationCallback<SubscriberForTopicResult> callback, @NotNull Executor callbackExecutor);

    /**
     * Iterate over all subscribers that have a subscription that equals the passed topic filter.
     * <p>
     * This method will iterate all subscribers including individual and shared subscriptions.
     * To filter the result use the overloaded methods and pass a {@link SubscriptionType}.
     * <p>
     * The callback is executed in the {@link ManagedExtensionExecutorService} per default.
     * Use the overloaded methods to pass a custom executor for the callback.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread of
     * this method.
     * <p>
     *
     * @param topicFilter the topic filter to search for (wildcards allowed). Same topic that is used in a MQTT
     *                    SUBSCRIBE
     *                    message. Wildcards in the topic filter are not expanded, only exact matches are contained in
     *                    the result.
     * @param callback    a {@link IterationCallback} that is called for every returned result.
     * @return a {@link CompletableFuture} that is completed after all iterations are executed
     * @throws NullPointerException if the passed topicFilter or callback are null
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(@NotNull String topicFilter, @NotNull IterationCallback<SubscriberWithFilterResult> callback);

    /**
     * Iterate over all subscribers that have a subscription that equals the passed topic filter.
     * <p>
     * This method will iterate all subscribers according to the passed {@link SubscriptionType}.
     * <p>
     * The callback is executed in the {@link ManagedExtensionExecutorService} per default.
     * Use the overloaded methods to pass a custom executor for the callback.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe),  as the callback might be executed in another thread as the calling thread
     * of this method.
     * <p>
     *
     * @param topicFilter      the topic filter to search for (wildcards allowed). Same topic that is used in a MQTT
     *                         SUBSCRIBE
     *                         message. Wildcards in the topic filter are not expanded, only exact matches are contained
     *                         in
     *                         the result.
     * @param callback         a {@link IterationCallback} that is called for every returned result.
     * @param subscriptionType a {@link SubscriptionType} to filter only individual or shared subscriptions, or both.
     * @return a {@link CompletableFuture} that is completed after all iterations are executed
     * @throws NullPointerException if the passed topicFilter, subscriptionType or callback are null
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(@NotNull String topicFilter, @NotNull SubscriptionType subscriptionType, @NotNull IterationCallback<SubscriberWithFilterResult> callback);

    /**
     * Iterate over all subscribers that have a subscription that equals the passed topic filter.
     * <p>
     * This method will iterate all subscribers including individual and shared subscriptions.
     * To filter the result use the overloaded methods and pass a {@link SubscriptionType}.
     * <p>
     * The callback is executed in the passed {@link Executor}
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection,
     * as the callback might be executed in another thread as the calling thread of this method.
     * <p>
     *
     * @param topicFilter      the topic filter to search for (wildcards allowed). Same topic that is used in a MQTT
     *                         SUBSCRIBE
     *                         message. Wildcards in the topic filter are not expanded, only exact matches are contained
     *                         in
     *                         the result.
     * @param callback         a {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor a {@link Executor} in which the callback for each iteration is executed
     * @return a {@link CompletableFuture} that is completed after all iterations are executed
     * @throws NullPointerException if the passed topicFilter, callback or callbackExecutor are null
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(@NotNull String topicFilter, @NotNull IterationCallback<SubscriberWithFilterResult> callback, @NotNull Executor callbackExecutor);

    /**
     * Iterate over all subscribers that have a subscription that equals the passed topic filter.
     * <p>
     * This method will iterate all subscribers according to the passed {@link SubscriptionType}.
     * <p>
     * The callback is executed in the passed {@link Executor}
     * If you want to collect the results of each execution of the callback in a collectio please make sure to use a
     * concurrent collection,
     * as the callback might be executed in another thread as the calling thread of this method.
     * <p>
     * nodes in the cluster have at least version 4.2.0
     *
     * @param topicFilter      the topic filter to search for (wildcards allowed). Same topic that is used in a MQTT
     *                         SUBSCRIBE
     *                         message. Wildcards in the topic filter are not expanded, only exact matches are contained
     *                         in
     *                         the result.
     * @param subscriptionType a {@link SubscriptionType} to filter only individual or shared subscriptions, or both.
     * @param callback         a {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor a {@link Executor} in which the callback for each iteration is executed
     * @return a {@link CompletableFuture} that is completed after all iterations are executed
     * @throws NullPointerException if the passed topicFilter, subscriptionType, callback or callbackExecutor are null
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(@NotNull String topicFilter, @NotNull SubscriptionType subscriptionType, @NotNull IterationCallback<SubscriberWithFilterResult> callback, @NotNull Executor callbackExecutor);

}
