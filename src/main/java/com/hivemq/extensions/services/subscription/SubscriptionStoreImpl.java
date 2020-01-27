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

package com.hivemq.extensions.services.subscription;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.exception.InvalidTopicException;
import com.hivemq.extension.sdk.api.services.exception.NoSuchClientIdException;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extension.sdk.api.services.subscription.*;
import com.hivemq.extensions.ListenableFutureConverter;
import com.hivemq.extensions.iteration.AsyncIterator;
import com.hivemq.extensions.iteration.AsyncIteratorFactory;
import com.hivemq.extensions.iteration.ChunkResult;
import com.hivemq.extensions.iteration.FetchCallback;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.executor.GlobalManagedPluginExecutorService;
import com.hivemq.extensions.services.general.IterationContextImpl;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.mqtt.topic.tree.SubscriptionTypeItemFilter;
import com.hivemq.persistence.clientsession.ChunkCursor;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;
import com.hivemq.persistence.local.xodus.BucketChunkResult;
import com.hivemq.persistence.local.xodus.MultipleChunkResult;
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
    private final @NotNull GlobalManagedPluginExecutorService managedExtensionExecutorService;
    private final @NotNull AsyncIteratorFactory asyncIteratorFactory;

    @Inject
    public SubscriptionStoreImpl(
            final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence,
            final @NotNull PluginServiceRateLimitService rateLimitService,
            final @NotNull LocalTopicTree topicTree,
            final @NotNull GlobalManagedPluginExecutorService managedExtensionExecutorService,
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

        final FetchCallback<ChunkCursor, SubscriptionsForClientResult> fetchCallback = new AllSubscribersFetchCallback(subscriptionPersistence);
        final AsyncIterator<ChunkCursor, SubscriptionsForClientResult> asyncIterator = asyncIteratorFactory.createIterator(fetchCallback, new AllSubscribersResultItemCallback(callbackExecutor, callback));

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

    static class AllSubscribersResultItemCallback implements AsyncIterator.ItemCallback<SubscriptionsForClientResult> {
        private @NotNull
        final Executor callbackExecutor;
        private @NotNull
        final IterationCallback<SubscriptionsForClientResult> callback;

        AllSubscribersResultItemCallback(@NotNull final Executor callbackExecutor, @NotNull final IterationCallback<SubscriptionsForClientResult> callback) {
            this.callbackExecutor = callbackExecutor;
            this.callback = callback;
        }

        @Override
        public @NotNull ListenableFuture<Boolean> onItems(@NotNull final Collection<SubscriptionsForClientResult> items) {

            final IterationContextImpl iterationContext = new IterationContextImpl();
            final SettableFuture<Boolean> resultFuture = SettableFuture.create();

            callbackExecutor.execute(new Runnable() {
                @Override
                public void run() {

                    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(callback.getClass().getClassLoader());
                        for (final SubscriptionsForClientResult subscriptionsForClientResult : items) {
                            callback.iterate(iterationContext, subscriptionsForClientResult);

                            if (iterationContext.isAborted()) {
                                resultFuture.set(false);
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        resultFuture.setException(e);
                    } finally {
                        Thread.currentThread().setContextClassLoader(contextClassLoader);
                    }
                    resultFuture.set(true);
                }
            });

            return resultFuture;
        }
    }

    static class AllSubscribersFetchCallback implements FetchCallback<ChunkCursor, SubscriptionsForClientResult> {

        @NotNull
        private final ClientSessionSubscriptionPersistence subscriptionPersistence;

        AllSubscribersFetchCallback(@NotNull final ClientSessionSubscriptionPersistence subscriptionPersistence) {
            this.subscriptionPersistence = subscriptionPersistence;
        }

        @Override
        public @NotNull ListenableFuture<ChunkResult<ChunkCursor, SubscriptionsForClientResult>> fetchNextResults(@Nullable final ChunkCursor cursor) {

            final ListenableFuture<MultipleChunkResult<Map<String, Set<Topic>>>> persistenceFuture =
                    subscriptionPersistence.getAllLocalSubscribersChunk(cursor != null ? cursor : new ChunkCursor());

            return Futures.transform(persistenceFuture, input -> {
                Preconditions.checkNotNull(input, "Chunk result cannot be null");
                return convertToChunkResult(input);
            }, MoreExecutors.directExecutor());
        }

        @NotNull
        ChunkResult<ChunkCursor, SubscriptionsForClientResult> convertToChunkResult(@NotNull final MultipleChunkResult<Map<String, Set<Topic>>> input) {
            final ImmutableList.Builder<SubscriptionsForClientResult> results = ImmutableList.builder();
            final ImmutableMap.Builder<Integer, String> lastKeys = ImmutableMap.builder();
            final ImmutableSet.Builder<Integer> finishedBuckets = ImmutableSet.builder();

            boolean finished = true;
            for (final Map.Entry<Integer, BucketChunkResult<Map<String, Set<Topic>>>> bucketResult : input.getValues().entrySet()) {
                final BucketChunkResult<Map<String, Set<Topic>>> bucketChunkResult = bucketResult.getValue();

                if (bucketChunkResult.isFinished()) {
                    finishedBuckets.add(bucketChunkResult.getBucketIndex());
                } else {
                    finished = false;
                }

                if (bucketChunkResult.getLastKey() != null) {
                    lastKeys.put(bucketChunkResult.getBucketIndex(), bucketChunkResult.getLastKey());
                }

                for (final Map.Entry<String, Set<Topic>> entry : bucketChunkResult.getValue().entrySet()) {
                    results.add(new SubscriptionsForClientResultImpl(entry.getKey(),
                            entry.getValue().stream().map(topic -> new TopicSubscriptionImpl(topic)).collect(Collectors.toSet())));
                }
            }

            return new ChunkResult<>(results.build(), new ChunkCursor(lastKeys.build(), finishedBuckets.build()), finished);
        }

    }
}
