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
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.*;
import com.hivemq.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.exception.InvalidTopicException;
import com.hivemq.extension.sdk.api.services.exception.NoSuchClientIdException;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionStore;
import com.hivemq.extension.sdk.api.services.subscription.TopicSubscription;
import com.hivemq.extensions.ListenableFutureConverter;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;
import com.hivemq.util.Topics;
import com.hivemq.annotations.Nullable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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

    @Inject
    public SubscriptionStoreImpl(final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence,
                                 final @NotNull PluginServiceRateLimitService rateLimitService) {
        this.subscriptionPersistence = subscriptionPersistence;
        this.rateLimitService = rateLimitService;
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
        }, MoreExecutors.directExecutor());

        return ListenableFutureConverter.toCompletable(settableFuture);
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
        }, MoreExecutors.directExecutor());

        return ListenableFutureConverter.toCompletable(settableFuture);
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
        return ListenableFutureConverter.toCompletable(subscriptionPersistence.remove(clientID, topicFilter));
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
            return ListenableFutureConverter.toVoidCompletable(subscriptionPersistence.removeSubscriptions(clientID, ImmutableSet.copyOf(topicFilters)));
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

    private static class ClientSubscriptionsToTopicSubscriptions implements Function<ImmutableSet<Topic>, Set<TopicSubscription>> {

        private static final ClientSubscriptionsToTopicSubscriptions INSTANCE = new ClientSubscriptionsToTopicSubscriptions();

        @Override
        public @NotNull Set<TopicSubscription> apply(final @NotNull ImmutableSet<Topic> clientSubscriptions) {
            return clientSubscriptions
                    .stream()
                    .map(TopicSubscriptionImpl::new).collect(Collectors.toUnmodifiableSet());
        }
    }
}
