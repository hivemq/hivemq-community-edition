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

package com.hivemq.extensions.services.publish;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.RetainedMessageStore;
import com.hivemq.extension.sdk.api.services.publish.RetainedPublish;
import com.hivemq.extensions.ListenableFutureConverter;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.executor.GlobalManagedPluginExecutorService;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.retained.RetainedMessagePersistence;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author Florian Limpöck
 *
 * @since 4.0.0
 */
@LazySingleton
public class RetainedMessageStoreImpl implements RetainedMessageStore {

    @NotNull
    private final RetainedMessagePersistence retainedMessagePersistence;

    @NotNull
    private final GlobalManagedPluginExecutorService globalManagedPluginExecutorService;

    @NotNull
    private final PluginServiceRateLimitService pluginServiceRateLimitService;

    @Inject
    public RetainedMessageStoreImpl(@NotNull final RetainedMessagePersistence retainedMessagePersistence,
                                    @NotNull final GlobalManagedPluginExecutorService globalManagedPluginExecutorService,
                                    @NotNull final PluginServiceRateLimitService pluginServiceRateLimitService) {
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.globalManagedPluginExecutorService = globalManagedPluginExecutorService;
        this.pluginServiceRateLimitService = pluginServiceRateLimitService;
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
        return ListenableFutureConverter.toCompletable(retainedMessageFuture, (r) -> r == null ? Optional.empty() : Optional.of(new RetainedPublishImpl(topic, r)), false, globalManagedPluginExecutorService);
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
        return ListenableFutureConverter.toCompletable(retainedMessagePersistence.remove(topic), globalManagedPluginExecutorService);
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
        return ListenableFutureConverter.toCompletable(retainedMessagePersistence.clear(), globalManagedPluginExecutorService);
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
        final ListenableFuture<Void> persist = retainedMessagePersistence.persist(retainedPublish.getTopic(), RetainedPublishImpl.convert(retainedPublish));

        return ListenableFutureConverter.toCompletable(persist, globalManagedPluginExecutorService);
    }

}
