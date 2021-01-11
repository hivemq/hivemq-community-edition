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

package com.hivemq.persistence.util;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * @author Lukas Brandl
 */
public abstract class AbstractFutureUtils {

    private static final Logger log = LoggerFactory.getLogger(AbstractFutureUtils.class);
    private static final AnyToVoidFunction ANY_TO_VOID_FUNCTION = new AnyToVoidFunction();

    public @NotNull ListenableFuture<Void> mergeVoidFutures(
            final @NotNull ListenableFuture<Void> future1, final @NotNull ListenableFuture<Void> future2) {
        return voidFutureFromList(ImmutableList.of(future1, future2));
    }

    public <T> @NotNull ListenableFuture<Void> voidFutureFromList(final @NotNull ImmutableList<ListenableFuture<T>> futureList) {

        if (futureList.isEmpty()) {
            return Futures.immediateFuture(null);
        }

        final SettableFuture<Void> resultFuture = SettableFuture.create();
        final FutureCallback<T> futureCallback = new VoidFutureCombiningCallback<>(futureList.size(), resultFuture);

        for (final ListenableFuture<T> future : futureList) {
            Futures.addCallback(future, futureCallback, MoreExecutors.directExecutor());
        }

        return resultFuture;
    }

    public void addExceptionLogger(final @NotNull ListenableFuture<?> listenableFuture) {
        Futures.addCallback(listenableFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(final @Nullable Object o) {
                //no op
            }

            @Override
            public void onFailure(final @NotNull Throwable throwable) {
                log.error("Uncaught exception", throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    public <T> void addPersistenceCallback(
            final @NotNull ListenableFuture<T> future,
            final @NotNull FutureCallback<? super T> callback,
            final @NotNull ExecutorService executorService) {
        if (!executorService.isShutdown()) {
            Futures.addCallback(future, callback, executorService);
        }
    }

    public abstract <T> void addPersistenceCallback(
            @NotNull ListenableFuture<T> future, @NotNull FutureCallback<? super T> callback);

    public abstract <E, C extends Collection<Set<E>>> @NotNull ListenableFuture<Set<E>> combineSetResults(
            @NotNull ListenableFuture<C> collectionFuture);

    public <E, C extends Collection<Set<E>>> @NotNull ListenableFuture<Set<E>> combineSetResults(
            final @NotNull ListenableFuture<C> collectionFuture, final @NotNull Executor executor) {

        final SettableFuture<Set<E>> resultFuture = SettableFuture.create();
        Futures.addCallback(collectionFuture, new FutureCallback<C>() {
            @Override
            public void onSuccess(final @Nullable C result) {
                if (result == null) {
                    resultFuture.set(null);
                    return;
                }
                final Set<E> resultSet = new HashSet<>();
                for (final Set<E> set : result) {
                    resultSet.addAll(set);
                }
                resultFuture.set(resultSet);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                resultFuture.setException(t);
            }
        }, executor);
        return resultFuture;
    }

    private static class AnyToVoidFunction implements Function<Object, Void> {

        @Override
        public Void apply(final @Nullable Object input) {
            return null;
        }
    }
}
