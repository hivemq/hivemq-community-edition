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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Lukas Brandl
 */
public abstract class AbstractFutureUtils {

    private static final Logger log = LoggerFactory.getLogger(AbstractFutureUtils.class);


    public abstract ListenableFuture<Void> voidFutureFromAnyFuture(final ListenableFuture<?> anyFuture);


    public ListenableFuture<Void> voidFutureFromAnyFuture(final ListenableFuture<?> anyFuture, final ExecutorService executorService) {
        return Futures.transformAsync(anyFuture, new AnyToVoidAsyncFunction(), executorService);
    }

    public ListenableFuture<Void> mergeVoidFutures(final ListenableFuture<Void> future1, final ListenableFuture<Void> future2) {
        return voidFutureFromList(ImmutableList.of(future1, future2));
    }

    public ListenableFuture<Void> voidFutureFromList(final ImmutableList<ListenableFuture<Void>> futureList) {

        if (futureList.isEmpty()) {
            return Futures.immediateFuture(null);
        }

        final SettableFuture<Void> resultFuture = SettableFuture.create();
        final FutureCallback<Void> futureCallback = new VoidFutureCombiningCallback(futureList.size(), resultFuture);
        for (final ListenableFuture<Void> voidListenableFuture : futureList) {

            Futures.addCallback(voidListenableFuture, futureCallback, MoreExecutors.directExecutor());
        }

        return resultFuture;
    }

    public ListenableFuture<Void> voidFutureFromAnyFutureList(final ImmutableList<ListenableFuture> futureList) {

        if (futureList.isEmpty()) {
            return Futures.immediateFuture(null);
        }

        final SettableFuture<Void> resultFuture = SettableFuture.create();
        final FutureCallback<Void> futureCallback = new VoidFutureFromAnyCombiningCallback(futureList.size(), resultFuture);
        for (final ListenableFuture voidListenableFuture : futureList) {

            Futures.addCallback(voidListenableFuture, futureCallback, MoreExecutors.directExecutor());
        }

        return resultFuture;
    }

    public <T> void addSettableFutureCallback(final ListenableFuture<T> listenableFuture, final SettableFuture<T> settableFuture) {
        Futures.addCallback(listenableFuture, new FutureCallback<T>() {
            @Override
            public void onSuccess(final T result) {
                settableFuture.set(result);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                settableFuture.setException(throwable);
            }
        }, MoreExecutors.directExecutor());

    }

    public <T> ListenableFuture<T> futureOfFuture(final ListenableFuture<ListenableFuture<T>> futureOfFuture) {

        final SettableFuture<T> resultFuture = SettableFuture.create();

        Futures.addCallback(futureOfFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final ListenableFuture<T> result) {
                if (result == null) {
                    resultFuture.set(null);
                    return;
                }

                addSettableFutureCallback(result, resultFuture);
            }

            @Override
            public void onFailure(final Throwable t) {
                resultFuture.setException(t);
            }
        }, MoreExecutors.directExecutor());

        return resultFuture;
    }

    public <T> void addSettableFutureCallback(final ListenableFuture<Void> listenableFuture, final SettableFuture<T> settableFuture, final T result) {
        Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void aVoid) {
                settableFuture.set(result);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                settableFuture.setException(throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    public void addExceptionLogger(final ListenableFuture<?> listenableFuture) {
        Futures.addCallback(listenableFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable final Object o) {
                //no op
            }

            @Override
            public void onFailure(final Throwable throwable) {
                log.error("Uncaught exception", throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    public <T> void addPersistenceCallback(final ListenableFuture<T> future, final FutureCallback<? super T> callback, final ExecutorService executorService) {
        if (!executorService.isShutdown()) {
            Futures.addCallback(future, callback, executorService);
        }
    }

    public abstract <T> void addPersistenceCallback(final ListenableFuture<T> future, final FutureCallback<? super T> callback);

    public abstract <T, S> void multiFutureResult(@NotNull final Map<ListenableFuture<T>, FutureUtils.Function<T>> futures, final S resultSum, final SettableFuture<S> resultSumFuture);

    public <T, S> void multiFutureResult(@NotNull final Map<ListenableFuture<T>, FutureUtils.Function<T>> futures, final S resultSum, final SettableFuture<S> resultSumFuture, final ExecutorService executorService) {
        final ImmutableList.Builder<ListenableFuture<Void>> builder = ImmutableList.builder();
        for (final ListenableFuture<T> future : futures.keySet()) {
            final SettableFuture<Void> resultFuture = SettableFuture.create();
            builder.add(resultFuture);
            Futures.addCallback(future, new FutureCallback<T>() {
                @Override
                public void onSuccess(@Nullable final T result) {
                    futures.get(future).function(result);
                    resultFuture.set(null);
                }

                @Override
                public void onFailure(final Throwable t) {
                    resultFuture.setException(t);
                }
            }, executorService);
        }
        final ListenableFuture<Void> doneFuture = FutureUtils.voidFutureFromList(builder.build());
        Futures.addCallback(doneFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                resultSumFuture.set(resultSum);
            }

            @Override
            public void onFailure(final Throwable t) {
                resultSumFuture.setException(t);
            }
        }, MoreExecutors.directExecutor());
    }

    public abstract <E, C extends Collection<Set<E>>> ListenableFuture<Set<E>> combineSetResults(final ListenableFuture<C> collectionFuture);

    public <E, C extends Collection<Set<E>>> ListenableFuture<Set<E>> combineSetResults(final ListenableFuture<C> collectionFuture, final ExecutorService executorService) {
        final SettableFuture<Set<E>> resultFuture = SettableFuture.create();
        Futures.addCallback(collectionFuture, new FutureCallback<C>() {
            @Override
            public void onSuccess(@Nullable final C result) {
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
            public void onFailure(final Throwable t) {
                resultFuture.setException(t);
            }
        }, executorService);
        return resultFuture;
    }

    private static class AnyToVoidAsyncFunction implements AsyncFunction<Object, Void> {
        @Override
        public ListenableFuture<Void> apply(@Nullable final Object input) throws Exception {
            return Futures.immediateFuture(null);
        }
    }

    public ListenableFuture<Boolean> voidToBoolFuture(final ListenableFuture<Void> future, final boolean value) {
        return Futures.transformAsync(future, new VoidToBooleanAsyncFunction(value), MoreExecutors.newDirectExecutorService());
    }

    private static class VoidToBooleanAsyncFunction implements AsyncFunction<Void, Boolean> {

        private final boolean value;

        private VoidToBooleanAsyncFunction(final boolean value) {
            this.value = value;
        }

        @Override
        public ListenableFuture<Boolean> apply(@Nullable final Void input) throws Exception {
            return Futures.immediateFuture(value);
        }
    }

}
