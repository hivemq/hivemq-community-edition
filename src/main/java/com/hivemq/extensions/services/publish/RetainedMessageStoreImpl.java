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
package com.hivemq.extensions.services.publish;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extension.sdk.api.services.publish.RetainedMessageStore;
import com.hivemq.extension.sdk.api.services.publish.RetainedPublish;
import com.hivemq.extensions.ListenableFutureConverter;
import com.hivemq.extensions.iteration.*;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.retained.RetainedMessagePersistence;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author Florian Limpöck
 * @author Georg Held
 * @since 4.0.0
 */
@LazySingleton
public class RetainedMessageStoreImpl implements RetainedMessageStore {

    private final @NotNull RetainedMessagePersistence retainedMessagePersistence;
    private final @NotNull GlobalManagedExtensionExecutorService globalManagedExtensionExecutorService;
    private final @NotNull PluginServiceRateLimitService pluginServiceRateLimitService;
    private final @NotNull AsyncIteratorFactory asyncIteratorFactory;

    @Inject
    public RetainedMessageStoreImpl(final @NotNull RetainedMessagePersistence retainedMessagePersistence,
                                    final @NotNull GlobalManagedExtensionExecutorService managedExtensionExecurotrService,
                                    final @NotNull PluginServiceRateLimitService pluginServiceRateLimitService,
                                    final @NotNull AsyncIteratorFactory asyncIteratorFactory) {
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.globalManagedExtensionExecutorService = managedExtensionExecurotrService;
        this.pluginServiceRateLimitService = pluginServiceRateLimitService;
        this.asyncIteratorFactory = asyncIteratorFactory;
    }

    /**
     * @inheritDoc
     */
    @NotNull
    @Override
    public CompletableFuture<Optional<RetainedPublish>> getRetainedMessage(@NotNull final String topic) {
        Preconditions.checkNotNull(topic, "A topic must never be null");
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        final ListenableFuture<RetainedMessage> retainedMessageFuture = retainedMessagePersistence.get(topic);
        return ListenableFutureConverter.toCompletable(retainedMessageFuture, (r) -> r == null ? Optional.empty() : Optional.of(new RetainedPublishImpl(topic, r)), false, globalManagedExtensionExecutorService);
    }

    /**
     * @inheritDoc
     */
    @NotNull
    @Override
    public CompletableFuture<Void> remove(@NotNull final String topic) {
        Preconditions.checkNotNull(topic, "A topic must never be null");
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        return ListenableFutureConverter.toCompletable(retainedMessagePersistence.remove(topic), globalManagedExtensionExecutorService);
    }

    /**
     * @inheritDoc
     */
    @NotNull
    @Override
    public CompletableFuture<Void> clear() {
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        return ListenableFutureConverter.toCompletable(retainedMessagePersistence.clear(), globalManagedExtensionExecutorService);
    }

    /**
     * @inheritDoc
     */
    @NotNull
    @Override
    public CompletableFuture<Void> addOrReplace(@NotNull final RetainedPublish retainedPublish) {
        Preconditions.checkNotNull(retainedPublish, "A retained publish must never be null");
        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }
        if (!(retainedPublish instanceof RetainedPublishImpl)) {
            return CompletableFuture.failedFuture(new DoNotImplementException(RetainedPublish.class.getSimpleName()));
        }
        final ListenableFuture<Void> persist = retainedMessagePersistence.persist(
                retainedPublish.getTopic(),
                RetainedPublishImpl.convert((RetainedPublishImpl) retainedPublish));

        return ListenableFutureConverter.toCompletable(persist, globalManagedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllRetainedMessages(final @NotNull IterationCallback<RetainedPublish> callback) {
        return iterateAllRetainedMessages(callback, globalManagedExtensionExecutorService);
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllRetainedMessages(final @NotNull IterationCallback<RetainedPublish> callback, final @NotNull Executor callbackExecutor) {
        Preconditions.checkNotNull(callback, "Callback cannot be null");
        Preconditions.checkNotNull(callbackExecutor, "Callback executor cannot be null");

        if (pluginServiceRateLimitService.rateLimitExceeded()) {
            return CompletableFuture.failedFuture(PluginServiceRateLimitService.RATE_LIMIT_EXCEEDED_EXCEPTION);
        }

        final FetchCallback<RetainedPublish> fetchCallback = new AllRetainedPublishesFetchCallBack(retainedMessagePersistence);
        final AsyncIterator<RetainedPublish> asyncIterator = asyncIteratorFactory.createIterator(fetchCallback, new AllItemsItemCallback<>(callbackExecutor, callback));

        asyncIterator.fetchAndIterate();

        final SettableFuture<Void> settableFuture = SettableFuture.create();
        asyncIterator.getFinishedFuture().whenComplete((aVoid, throwable) -> {
            if (throwable != null) {
                settableFuture.setException(throwable);
            } else {
                settableFuture.set(null);
            }
        });

        return ListenableFutureConverter.toCompletable(settableFuture, globalManagedExtensionExecutorService);
    }

    static class AllRetainedPublishesFetchCallBack extends AllItemsFetchCallback<RetainedPublish, Map<String, RetainedMessage>> {

        private final @NotNull RetainedMessagePersistence retainedMessagePersistence;

        AllRetainedPublishesFetchCallBack(final @NotNull RetainedMessagePersistence retainedMessagePersistence) {
            this.retainedMessagePersistence = retainedMessagePersistence;
        }

        @Override
        protected @NotNull ListenableFuture<MultipleChunkResult<Map<String, RetainedMessage>>> persistenceCall(final @NotNull ChunkCursor chunkCursor) {
            return retainedMessagePersistence.getAllLocalRetainedMessagesChunk(chunkCursor);
        }

        @Override
        protected @NotNull Collection<RetainedPublish> transform(final @NotNull Map<String, RetainedMessage> stringRetainedMessageMap) {
            return stringRetainedMessageMap.entrySet().stream().map(entry -> new RetainedPublishImpl(entry.getKey(), entry.getValue())).collect(Collectors.toUnmodifiableList());
        }
    }
}
