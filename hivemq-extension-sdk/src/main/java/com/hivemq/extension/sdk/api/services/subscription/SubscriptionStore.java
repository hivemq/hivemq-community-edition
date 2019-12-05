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
package com.hivemq.extension.sdk.api.services.subscription;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.ManagedExtensionExecutorService;
import com.hivemq.extension.sdk.api.services.exception.*;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extension.sdk.api.services.general.IterationContext;

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
     * @return A {@link CompletableFuture} object that will succeed, as soon as the subscriptions were added by all
     * Cluster Nodes.
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
     * @return A {@link CompletableFuture} object that will succeed, as soon as the subscription was removed by all
     * cluster nodes.
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
     * @return A {@link CompletableFuture} object that will succeed, as soon as the subscriptions were removed by all
     * Cluster Nodes.
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
     * Iterate over all subscribers that have a subscription that matches the passed topic.
     * Includes subscribers with direct matches of the topic or a match via a wildcard topic.
     * <p>
     * Example: For topic <code>example/topic</code> we would iterate over all clients with a subscription for
     * <code>example/topic</code> or <code>example/#</code> and other wildcard matches.
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
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topic    The topic to check for (no wildcards allowed). Same topic that is used in an MQTT PUBLISH
     *                 message.
     * @param callback An {@link IterationCallback} that is called for every returned result.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found
     * for the topic or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException     If the passed topic or callback are null.
     * @throws IllegalArgumentException If the passed topic is not a valid topic or contains wildcards.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(@NotNull String topic, @NotNull IterationCallback<SubscriberForTopicResult> callback);

    /**
     * Iterate over all subscribers that have a subscription that matches the passed topic.
     * Includes subscribers with direct matches of the topic or a match via a wildcard topic.
     * <p>
     * Example: For topic <code>example/topic</code> we would iterate over all clients with a subscription for
     * <code>example/topic</code> or <code>example/#</code> and other wildcard matches.
     * <p>
     * This method will iterate all subscribers according to the passed {@link SubscriptionType}.
     * <p>
     * The callback is executed in the {@link ManagedExtensionExecutorService} per default.
     * Use the overloaded methods to pass a custom executor for the callback.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread of
     * this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topic            The topic to check for (no wildcards allowed). Same topic that is used in an MQTT PUBLISH
     *                         message.
     * @param subscriptionType A {@link SubscriptionType} to filter only individual or shared subscriptions, or both.
     * @param callback         An {@link IterationCallback} that is called for every returned result.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found
     * for the topic or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException     If the passed topic, subscriptionType or callback are null.
     * @throws IllegalArgumentException If the passed topic is not a valid topic or contains wildcards.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(@NotNull String topic, @NotNull SubscriptionType subscriptionType, @NotNull IterationCallback<SubscriberForTopicResult> callback);

    /**
     * Iterate over all subscribers that have a subscription that matches the passed topic.
     * Includes subscribers with direct matches of the topic or a match via a wildcard topic.
     * <p>
     * Example: For topic <code>example/topic</code> we would iterate over all clients with a subscription for
     * <code>example/topic</code> or <code>example/#</code> and other wildcard matches.
     * <p>
     * This method will iterate all subscribers including individual and shared subscriptions.
     * To filter the result use the overloaded methods and pass a {@link SubscriptionType}.
     * <p>
     * The callback is executed in the passed {@link Executor}.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread of
     * this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topic            The topic to check for (no wildcards allowed). Same topic that is used in an MQTT PUBLISH
     *                         message.
     * @param callback         An {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor An {@link Executor} in which the callback for each iteration is executed.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found
     * for the topic or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException     If the passed topic, callback or callbackExecutor are null.
     * @throws IllegalArgumentException If the passed topic is not a valid topic or contains wildcards.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(@NotNull String topic, @NotNull IterationCallback<SubscriberForTopicResult> callback, @NotNull Executor callbackExecutor);

    /**
     * Iterate over all subscribers that have a subscription that matches the passed topic.
     * Includes subscribers with direct matches of the topic or a match via a wildcard topic.
     * <p>
     * Example: For topic <code>example/topic</code> we would iterate over all clients with a subscription for
     * <code>example/topic</code> or <code>example/#</code> and other wildcard matches.
     * <p>
     * This method will iterate all subscribers according to the passed {@link SubscriptionType}.
     * <p>
     * The callback is executed in the passed {@link Executor}.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread of
     * this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topic            The topic to check for (no wildcards allowed). Same topic that is used in an MQTT PUBLISH
     *                         message.
     * @param subscriptionType A {@link SubscriptionType} to filter only individual or shared subscriptions, or both.
     * @param callback         An {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor An {@link Executor} in which the callback for each iteration is executed.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found
     * for the topic or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException     If the passed topic, subscriptionType, callback or callbackExecutor are null.
     * @throws IllegalArgumentException If the passed topic is not a valid topic or contains wildcards.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(@NotNull String topic, @NotNull SubscriptionType subscriptionType, @NotNull IterationCallback<SubscriberForTopicResult> callback, @NotNull Executor callbackExecutor);

    /**
     * Iterate over all subscribers that have a subscription that equals the passed topic filter.
     * Only includes subscribers with direct matches of the topic.
     * <p>
     * Example 1: For topic filter <code>example/topic</code> we would iterate over all clients with a subscription for
     * <code>example/topic</code>, but not for <code>example/#</code> or other wildcard matches.
     * <p>
     * Example 2: For topic filter <code>example/#</code> we would iterate over all clients with a subscription for
     * <code>example/#</code>, but not for <code>example/+</code> or other wildcard matches and also not for topic
     * filters like <code>example/topic</code>.
     * <p>
     * This method will iterate all subscribers including individual and shared subscriptions.
     * To filter the result use the overloaded methods and pass a {@link SubscriptionType}.
     * <p>
     * The callback is executed in the {@link ManagedExtensionExecutorService} per default.
     * Use the overloaded methods to pass a custom executor for the callback.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread
     * of this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topicFilter The topic filter to search for (wildcards allowed). Same topic that is used in an MQTT
     *                    SUBSCRIBE message. Wildcards in the topic filter are not expanded, only exact matches are
     *                    contained in the result.
     * @param callback    An {@link IterationCallback} that is called for every returned result.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found
     * for the topic filter or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException     If the passed topicFilter or callback are null.
     * @throws IllegalArgumentException If the passed topicFilter is not a valid topic.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(@NotNull String topicFilter, @NotNull IterationCallback<SubscriberWithFilterResult> callback);

    /**
     * Iterate over all subscribers that have a subscription that equals the passed topic filter.
     * Only includes subscribers with direct matches of the topic.
     * <p>
     * Example 1: For topic filter <code>example/topic</code> we would iterate over all clients with a subscription for
     * <code>example/topic</code>, but not for <code>example/#</code> or other wildcard matches.
     * <p>
     * Example 2: For topic filter <code>example/#</code> we would iterate over all clients with a subscription for
     * <code>example/#</code>, but not for <code>example/+</code> or other wildcard matches and also not for topic
     * filters like <code>example/topic</code>.
     * <p>
     * This method will iterate all subscribers according to the passed {@link SubscriptionType}.
     * <p>
     * The callback is executed in the {@link ManagedExtensionExecutorService} per default.
     * Use the overloaded methods to pass a custom executor for the callback.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread
     * of this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topicFilter      The topic filter to search for (wildcards allowed). Same topic that is used in an MQTT
     *                         SUBSCRIBE message. Wildcards in the topic filter are not expanded, only exact matches are
     *                         contained in the result.
     * @param callback         An {@link IterationCallback} that is called for every returned result.
     * @param subscriptionType A {@link SubscriptionType} to filter only individual or shared subscriptions, or both.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found
     * for the topic filter or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException     If the passed topicFilter, subscriptionType or callback are null.
     * @throws IllegalArgumentException If the passed topicFilter is not a valid topic.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(@NotNull String topicFilter, @NotNull SubscriptionType subscriptionType, @NotNull IterationCallback<SubscriberWithFilterResult> callback);

    /**
     * Iterate over all subscribers that have a subscription that equals the passed topic filter.
     * Only includes subscribers with direct matches of the topic.
     * <p>
     * Example 1: For topic filter <code>example/topic</code> we would iterate over all clients with a subscription for
     * <code>example/topic</code>, but not for <code>example/#</code> or other wildcard matches.
     * <p>
     * Example 2: For topic filter <code>example/#</code> we would iterate over all clients with a subscription for
     * <code>example/#</code>, but not for <code>example/+</code> or other wildcard matches and also not for topic
     * filters like <code>example/topic</code>.
     * <p>
     * This method will iterate all subscribers including individual and shared subscriptions.
     * To filter the result use the overloaded methods and pass a {@link SubscriptionType}.
     * <p>
     * The callback is executed in the passed {@link Executor}.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection, as the callback might be executed in another thread as the calling thread of this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topicFilter      The topic filter to search for (wildcards allowed). Same topic that is used in an MQTT
     *                         SUBSCRIBE message. Wildcards in the topic filter are not expanded, only exact matches are
     *                         contained in the result.
     * @param callback         An {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor An {@link Executor} in which the callback for each iteration is executed.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found
     * for the topic filter or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException     If the passed topicFilter, callback or callbackExecutor are null.
     * @throws IllegalArgumentException If the passed topicFilter is not a valid topic.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(@NotNull String topicFilter, @NotNull IterationCallback<SubscriberWithFilterResult> callback, @NotNull Executor callbackExecutor);

    /**
     * Iterate over all subscribers that have a subscription that equals the passed topic filter.
     * Only includes subscribers with direct matches of the topic.
     * <p>
     * Example 1: For topic filter <code>example/topic</code> we would iterate over all clients with a subscription for
     * <code>example/topic</code>, but not for <code>example/#</code> or other wildcard matches.
     * <p>
     * Example 2: For topic filter <code>example/#</code> we would iterate over all clients with a subscription for
     * <code>example/#</code>, but not for <code>example/+</code> or other wildcard matches and also not for topic
     * filters like <code>example/topic</code>.
     * <p>
     * This method will iterate all subscribers according to the passed {@link SubscriptionType}.
     * <p>
     * The callback is executed in the passed {@link Executor}.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection, as the callback might be executed in another thread as the calling thread of this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topicFilter      The topic filter to search for (wildcards allowed). Same topic that is used in an MQTT
     *                         SUBSCRIBE message. Wildcards in the topic filter are not expanded, only exact matches are
     *                         contained in the result.
     * @param subscriptionType A {@link SubscriptionType} to filter only individual or shared subscriptions, or both.
     * @param callback         An {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor An {@link Executor} in which the callback for each iteration is executed.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no match is found
     * for the topic filter or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException     If the passed topicFilter, subscriptionType, callback or callbackExecutor are
     *                                  null.
     * @throws IllegalArgumentException If the passed topicFilter is not a valid topic.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(@NotNull String topicFilter, @NotNull SubscriptionType subscriptionType, @NotNull IterationCallback<SubscriberWithFilterResult> callback, @NotNull Executor callbackExecutor);

    /**
     * Iterate over all subscribers and their subscriptions.
     * <p>
     * The callback is called once for each client.
     * Passed to each execution of the callback are the client identifier and a set of all its subscriptions.
     * Clients without subscriptions are not included.
     * <p>
     * The callback is executed in the {@link ManagedExtensionExecutorService} per default.
     * Use the overloaded methods to pass a custom executor for the callback.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread
     * of this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     * {@link CompletableFuture} fails with a {@link IterationFailedException} if the cluster topology changed
     * during the iteration (e.g. a network-split, node leave or node join).
     *
     * @param callback An {@link IterationCallback} that is called for every returned result.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no subscriptions exist
     * or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException If the passed callback is null.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscriptions(@NotNull IterationCallback<SubscriptionsForClientResult> callback);

    /**
     * Iterate over all subscribers and their subscriptions.
     * <p>
     * The callback is called once for each client.
     * Passed to each execution of the callback are the client identifier and a set of all its subscriptions.
     * Clients without subscriptions are not included.
     * <p>
     * The callback is executed in the passed {@link Executor}.
     * If you want to collect the results of each execution of the callback in a collection please make sure to use a
     * concurrent collection (thread-safe), as the callback might be executed in another thread as the calling thread
     * of this method.
     * <p>
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     * <p>
     * CAUTION: This method can be used in large scale deployments, but it is a very expensive operation.
     * Do not call this method in short time intervals.
     * <p>
     * If you are searching for a specific entry in the results and have found what you are looking for, you can abort
     * further iteration and save resources by calling {@link IterationContext#abortIteration()}.
     * <p>
     * {@link CompletableFuture} fails with an {@link IncompatibleHiveMQVersionException} if not all
     * HiveMQ nodes in the cluster have at least version 4.2.0.
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit
     * was
     * exceeded.
     * {@link CompletableFuture} fails with a {@link IterationFailedException} if the cluster topology changed
     * during the iteration (e.g. a network-split, node leave or node join).
     *
     * @param callback         An {@link IterationCallback} that is called for every returned result.
     * @param callbackExecutor An {@link Executor} in which the callback for each iteration is executed.
     * @return A {@link CompletableFuture} that is completed after all iterations are executed, no subscriptions exist
     * or the iteration is aborted manually with the {@link IterationContext}.
     * @throws NullPointerException If the passed callback or callbackExecutor are null.
     * @since 4.2.0
     */
    @NotNull CompletableFuture<Void> iterateAllSubscriptions(@NotNull IterationCallback<SubscriptionsForClientResult> callback, @NotNull Executor callbackExecutor);


}
