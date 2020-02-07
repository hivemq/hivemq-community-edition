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

package com.hivemq.persistence.util;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.ioc.annotation.Persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Lukas Brandl
 */
public class FutureUtilsImpl extends AbstractFutureUtils {

    private final ListeningExecutorService persistenceExecutorService;

    @Inject
    public FutureUtilsImpl(@Persistence final ListeningExecutorService persistenceExecutorService) {
        this.persistenceExecutorService = persistenceExecutorService;
    }

    @Override
    public ListenableFuture<Void> voidFutureFromAnyFuture(final ListenableFuture<?> anyFuture) {
        return super.voidFutureFromAnyFuture(anyFuture, persistenceExecutorService);
    }

    @Override
    public <T> void addPersistenceCallback(final ListenableFuture<T> future, final FutureCallback<? super T> callback) {
        super.addPersistenceCallback(future, callback, persistenceExecutorService);
    }

    @Override
    public <T, S> void multiFutureResult(@NotNull final Map<ListenableFuture<T>, FutureUtils.Function<T>> futures, final S resultSum, final SettableFuture<S> resultSumFuture) {
        super.multiFutureResult(futures, resultSum, resultSumFuture, persistenceExecutorService);
    }

    @Override
    public <T, S> void multiFutureResult(@NotNull final Map<ListenableFuture<T>, FutureUtils.Function<T>> futures, final S resultSum, final SettableFuture<S> resultSumFuture, final ExecutorService executorService) {
        super.multiFutureResult(futures, resultSum, resultSumFuture, executorService);
    }

    @Override
    public <E, C extends Collection<Set<E>>> ListenableFuture<Set<E>> combineSetResults(final ListenableFuture<C> collectionFuture) {
        return super.combineSetResults(collectionFuture, persistenceExecutorService);
    }
}
