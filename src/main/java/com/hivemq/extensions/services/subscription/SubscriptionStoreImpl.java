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
package com.hivemq.extensions.services.subscription;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.exception.InvalidTopicException;
import com.hivemq.extension.sdk.api.services.exception.NoSuchClientIdException;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extension.sdk.api.services.subscription.*;
import com.hivemq.extensions.ListenableFutureConverter;
import com.hivemq.extensions.iteration.*;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.extensions.services.general.IterationContextImpl;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.mqtt.topic.tree.SubscriptionTypeItemFilter;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;
import com.hivemq.util.Topics;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@LazySingleton
public class SubscriptionStoreImpl implements SubscriptionStore {

    private final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence;
    private final @NotNull PluginServiceRateLimitService rateLimitService;
    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull GlobalManagedExtensionExecutorService managedExtensionExecutorService;
    private final @NotNull AsyncIteratorFactory asyncIteratorFactory;

    @Inject
    public SubscriptionStoreImpl(
            final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence,
            final @NotNull PluginServiceRateLimitService rateLimitService,
            final @NotNull LocalTopicTree topicTree,
            final @NotNull GlobalManagedExtensionExecutorService managedExtensionExecutorService,
            final @NotNull AsyncIteratorFactory asyncIteratorFactory) {
        this.subscriptionPersistence = subscriptionPersistence;
        this.rateLimitService = rateLimitService;
        this.topicTree = topicTree;
        this.managedExtensionExecutorService = managedExtensionExecutorService;
        this.asyncIteratorFactory = asyncIteratorFactory;
    }

    @Override
    public @NotNull CompletableFuture<Void> addSubscription(final @NotNull String clientID, final @NotNull TopicSubscription subscription) {
        Preconditions.checkNotNull(clientID, "Client id must never be null");
        Preconditions.checkArgument(!clientID.isEmpty(), "Client id must never be empty");
        Preconditions.checkNotNull(subscription, "Topic subscription must never be null");

        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        if (!(subscription instanceof TopicSubscriptionImpl)) {
            return CompletableFuture.failedFuture(new DoNotImplementException(TopicSubscription.class.getSimpleName()));
        }

        final ListenableFuture<SubscriptionResult> addSubscriptionFuture =
                subscriptionPersistence.addSubscription(clientID, TopicSubscriptionImpl.convertToTopic(subscription));

        final SettableFuture<Void> settableFuture = SettableFuture.create();

        Futures.addCallback(addSubscriptionFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final SubscriptionResult result) {
                if (result == null) {
                    settableFuture.setException(new NoSuchClientIdException(clientID));
                    return;
                }
                settableFuture.set(null);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                settableFuture.setException(t);
            }
        }, managedExtensionExecutorService);

        return ListenableFutureConverter.toCompletable(settableFuture, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> addSubscriptions(final @NotNull String clientID, final @NotNull Set<TopicSubscription> subscriptions) {

        Preconditions.checkNotNull(clientID, "Client id must never be null");
        Preconditions.checkArgument(!clientID.isEmpty(), "Client id must never be empty");
        Preconditions.checkNotNull(subscriptions, "Subscriptions must never be null");
        Preconditions.checkArgument(!subscriptions.isEmpty(), "Subscriptions must never be empty");

        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }

        final ImmutableSet.Builder<Topic> topicsToProcess = new ImmutableSet.Builder<>();
        for (final TopicSubscription topicSubscription : subscriptions) {
            Preconditions.checkNotNull(topicSubscription, "Topic subscription must never be null");
            if (!(topicSubscription instanceof TopicSubscriptionImpl)) {
                return CompletableFuture.failedFuture(new DoNotImplementException(TopicSubscription.class.getSimpleName()));
            }

            topicsToProcess.add(TopicSubscriptionImpl.convertToTopic(topicSubscription));
        }

        return processAddSubscriptions(clientID, topicsToProcess.build());

    }

    @NotNull
    private CompletableFuture<Void> processAddSubscriptions(final @NotNull String clientID, final @NotNull ImmutableSet<Topic> successTopics) {
        final ListenableFuture<ImmutableList<SubscriptionResult>> addSubscriptionFuture = subscriptionPersistence.addSubscriptions(clientID, successTopics);

        final SettableFuture<Void> settableFuture = SettableFuture.create();

        Futures.addCallback(addSubscriptionFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final ImmutableList<SubscriptionResult> result) {
                if (result == null) {
                    settableFuture.setException(new NoSuchClientIdException(clientID));
                    return;
                }
                settableFuture.set(null);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                settableFuture.setException(t);
            }
        }, managedExtensionExecutorService);

        return ListenableFutureConverter.toCompletable(settableFuture, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> removeSubscription(final @NotNull String clientID, final @NotNull String topicFilter) {
        Preconditions.checkNotNull(clientID, "Client id must never be null");
        Preconditions.checkArgument(!clientID.isEmpty(), "Client id must never be empty");
        Preconditions.checkNotNull(topicFilter, "Topic filter must never be null");

        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        if (!Topics.isValidToSubscribe(topicFilter)) {
            return CompletableFuture.failedFuture(new InvalidTopicException(topicFilter));
        }
        return ListenableFutureConverter.toCompletable(subscriptionPersistence.remove(clientID, topicFilter), managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> removeSubscriptions(final @NotNull String clientID, final @NotNull Set<String> topicFilters) {

        Preconditions.checkNotNull(clientID, "Client id must never be null");
        Preconditions.checkArgument(!clientID.isEmpty(), "Client id must never be empty");
        Preconditions.checkNotNull(topicFilters, "Topic-filters must never be null");
        Preconditions.checkArgument(!topicFilters.isEmpty(), "Topics-filters must never be empty");

        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }

        final List<String> failedTopics = new ArrayList<>();
        for (final String topicFilter : topicFilters) {
            Preconditions.checkNotNull(topicFilter, "Topic filter must never be null");
            if (!Topics.isValidToSubscribe(topicFilter)) {
                failedTopics.add(topicFilter);
            }
        }

        if (failedTopics.isEmpty()) {
            return ListenableFutureConverter.toVoidCompletable(subscriptionPersistence.removeSubscriptions(clientID, ImmutableSet.copyOf(topicFilters)), managedExtensionExecutorService);
        } else {
            return CompletableFuture.failedFuture(new InvalidTopicException("Topics not valid: " + failedTopics));
        }
    }

    @Override
    public @NotNull CompletableFuture<Set<TopicSubscription>> getSubscriptions(final @NotNull String clientID) {
        Preconditions.checkNotNull(clientID, "Client id must never be null");
        Preconditions.checkArgument(!clientID.isEmpty(), "Client id must never be empty");

        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        return CompletableFuture.completedFuture(ClientSubscriptionsToTopicSubscriptions.INSTANCE.apply(subscriptionPersistence.getSubscriptions(clientID)));
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(final @NotNull String topic,
                                                                          final @NotNull IterationCallback<SubscriberForTopicResult> callback) {
        return iterateAllSubscribersForTopic(topic, SubscriptionType.ALL, callback, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(final @NotNull String topic,
                                                                          final @NotNull SubscriptionType subscriptionType,
                                                                          final @NotNull IterationCallback<SubscriberForTopicResult> callback) {
        return iterateAllSubscribersForTopic(topic, subscriptionType, callback, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(final @NotNull String topic,
                                                                          final @NotNull IterationCallback<SubscriberForTopicResult> callback,
                                                                          final @NotNull Executor callbackExecutor) {
        return iterateAllSubscribersForTopic(topic, SubscriptionType.ALL, callback, callbackExecutor);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscribersForTopic(final @NotNull String topic,
                                                                          final @NotNull SubscriptionType subscriptionType,
                                                                          final @NotNull IterationCallback<SubscriberForTopicResult> callback,
                                                                          final @NotNull Executor callbackExecutor) {

        Preconditions.checkNotNull(topic, "Topic cannot be null");
        Preconditions.checkNotNull(callback, "Callback cannot be null");
        Preconditions.checkNotNull(callbackExecutor, "Executor cannot be null");
        Preconditions.checkArgument(
                Topics.isValidTopicToPublish(topic),
                "Topic must be a valid topic and cannot contain wildcard characters, got '" + topic + "'");

        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }

        final ImmutableSet<String> subscribers = topicTree.getSubscribersForTopic(topic, new SubscriptionTypeItemFilter(subscriptionType), false);

        final SettableFuture<Void> iterationFinishedFuture = SettableFuture.create();

        callbackExecutor.execute(() -> {

            final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
            final IterationContextImpl iterationContext = new IterationContextImpl();
            try {
                Thread.currentThread().setContextClassLoader(callback.getClass().getClassLoader());

                for (final String subscriber : subscribers) {
                    try {
                        callback.iterate(iterationContext, new SubscriberForTopicResultImpl(subscriber));
                        if (iterationContext.isAborted()) {
                            iterationFinishedFuture.set(null);
                            return;
                        }

                    } catch (final Exception e) {
                        iterationFinishedFuture.setException(e);
                        return;
                    }
                }
                iterationFinishedFuture.set(null);
            } finally {
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }
        });

        return ListenableFutureConverter.toCompletable(iterationFinishedFuture, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(final @NotNull String topicFilter,
                                                                                 final @NotNull IterationCallback<SubscriberWithFilterResult> callback) {
        return iterateAllSubscribersWithTopicFilter(
                topicFilter, SubscriptionType.ALL, callback, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(final @NotNull String topicFilter,
                                                                                 final @NotNull SubscriptionType subscriptionType,
                                                                                 final @NotNull IterationCallback<SubscriberWithFilterResult> callback) {
        return iterateAllSubscribersWithTopicFilter(
                topicFilter, subscriptionType, callback, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(final @NotNull String topicFilter,
                                                                                 final @NotNull IterationCallback<SubscriberWithFilterResult> callback,
                                                                                 final @NotNull Executor callbackExecutor) {
        return iterateAllSubscribersWithTopicFilter(topicFilter, SubscriptionType.ALL, callback, callbackExecutor);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscribersWithTopicFilter(final @NotNull String topicFilter,
                                                                                 final @NotNull SubscriptionType subscriptionType,
                                                                                 final @NotNull IterationCallback<SubscriberWithFilterResult> callback,
                                                                                 final @NotNull Executor callbackExecutor) {
        Preconditions.checkNotNull(topicFilter, "Topic filter cannot be null");
        Preconditions.checkNotNull(callback, "Callback cannot be null");
        Preconditions.checkNotNull(callbackExecutor, "Executor cannot be null");
        Preconditions.checkNotNull(subscriptionType, "SubscriptionType cannot be null");
        Preconditions.checkArgument(Topics.isValidToSubscribe(topicFilter), "Topic filter must be a valid MQTT topic filter, got '" + topicFilter + "'");

        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }

        final ImmutableSet<String> subscribers =
                topicTree.getSubscribersWithFilter(topicFilter, new SubscriptionTypeItemFilter(subscriptionType));

        final SettableFuture<Void> iterationFinishedFuture = SettableFuture.create();
        callbackExecutor.execute(() -> {

            final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
            final IterationContextImpl iterationContext = new IterationContextImpl();
            try {
                Thread.currentThread().setContextClassLoader(callback.getClass().getClassLoader());

                for (final String subscriber : subscribers) {
                    try {
                        callback.iterate(iterationContext, new SubscriberWithFilterResultImpl(subscriber));
                        if (iterationContext.isAborted()) {
                            iterationFinishedFuture.set(null);
                            return;
                        }

                    } catch (final Exception e) {
                        iterationFinishedFuture.setException(e);
                        return;
                    }
                }
                iterationFinishedFuture.set(null);
            } finally {
                Thread.currentThread().setContextClassLoader(previousClassLoader);
            }
        });

        return ListenableFutureConverter.toCompletable(iterationFinishedFuture, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscriptions(@NotNull final IterationCallback<SubscriptionsForClientResult> callback) {
        return iterateAllSubscriptions(callback, managedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllSubscriptions(@NotNull final IterationCallback<SubscriptionsForClientResult> callback,
                                                                    @NotNull final Executor callbackExecutor) {

        Preconditions.checkNotNull(callback, "Callback cannot be null");
        Preconditions.checkNotNull(callback, "Callback executor cannot be null");

        if (rateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }

        final FetchCallback<SubscriptionsForClientResult> fetchCallback = new AllSubscribersFetchCallback(subscriptionPersistence);
        final AsyncIterator<SubscriptionsForClientResult> asyncIterator = asyncIteratorFactory.createIterator(fetchCallback, new AllItemsItemCallback<>(callbackExecutor, callback));

        asyncIterator.fetchAndIterate();

        final SettableFuture<Void> iterationFinishedFuture = SettableFuture.create();

        asyncIterator.getFinishedFuture().whenComplete((aVoid, throwable) -> {
            if (throwable != null) {
                iterationFinishedFuture.setException(throwable);
            } else {
                iterationFinishedFuture.set(null);
            }
        });

        return ListenableFutureConverter.toCompletable(iterationFinishedFuture, managedExtensionExecutorService);
    }

    private static class ClientSubscriptionsToTopicSubscriptions implements Function<ImmutableSet<Topic>, Set<TopicSubscription>> {

        private static final ClientSubscriptionsToTopicSubscriptions INSTANCE = new ClientSubscriptionsToTopicSubscriptions();

        @Override
        public @NotNull Set<TopicSubscription> apply(final @NotNull ImmutableSet<Topic> clientSubscriptions) {
            return clientSubscriptions
                    .stream()
                    .map(TopicSubscriptionImpl::new).collect(Collectors.toUnmodifiableSet());
        }
    }

    static class AllSubscribersFetchCallback extends AllItemsFetchCallback<SubscriptionsForClientResult, Map<String, ImmutableSet<Topic>>> {

        @NotNull
        private final ClientSessionSubscriptionPersistence subscriptionPersistence;

        AllSubscribersFetchCallback(@NotNull final ClientSessionSubscriptionPersistence subscriptionPersistence) {
            this.subscriptionPersistence = subscriptionPersistence;
        }

        @Override
        protected @NotNull ListenableFuture<MultipleChunkResult<Map<String, ImmutableSet<Topic>>>> persistenceCall(final @NotNull ChunkCursor chunkCursor) {
            return subscriptionPersistence.getAllLocalSubscribersChunk(chunkCursor);
        }

        @Override
        protected @NotNull Collection<SubscriptionsForClientResult> transform(final @NotNull Map<String, ImmutableSet<Topic>> stringSetMap) {
            return stringSetMap.entrySet().stream()
                    .map(entry -> new SubscriptionsForClientResultImpl(entry.getKey(),
                            entry.getValue().stream().map(TopicSubscriptionImpl::new).collect(Collectors.toSet()))).collect(Collectors.toUnmodifiableList());
        }
    }
}
