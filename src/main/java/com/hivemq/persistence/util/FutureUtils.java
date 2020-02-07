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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Lukas Brandl
 */
public class FutureUtils {

    @Inject
    public static AbstractFutureUtils delegate;

    public static ListenableFuture<Void> voidFutureFromAnyFuture(final ListenableFuture<?> anyFuture) {
        return delegate.voidFutureFromAnyFuture(anyFuture);
    }

    public static ListenableFuture<Void> voidFutureFromAnyFuture(final ListenableFuture<?> anyFuture, final ExecutorService executorService) {
        return delegate.voidFutureFromAnyFuture(anyFuture, executorService);
    }

    public static ListenableFuture<Void> mergeVoidFutures(final ListenableFuture<Void> future1, final ListenableFuture<Void> future2) {
        return delegate.mergeVoidFutures(future1, future2);
    }

    public static ListenableFuture<Void> voidFutureFromList(final ImmutableList<ListenableFuture<Void>> futureList) {
        return delegate.voidFutureFromList(futureList);
    }

    public static ListenableFuture<Void> voidFutureFromAnyFutureList(final ImmutableList<ListenableFuture> futureList) {
        return delegate.voidFutureFromAnyFutureList(futureList);
    }

    public static <T> void addSettableFutureCallback(final ListenableFuture<T> listenableFuture, final SettableFuture<T> settableFuture) {
        delegate.addSettableFutureCallback(listenableFuture, settableFuture);
    }

    public static <T> ListenableFuture<T> futureOfFuture(final ListenableFuture<ListenableFuture<T>> futureOfFuture) {
        return delegate.futureOfFuture(futureOfFuture);
    }

    public static <T> void addSettableFutureCallback(final ListenableFuture<Void> listenableFuture, final SettableFuture<T> settableFuture, final T result) {
        delegate.addSettableFutureCallback(listenableFuture, settableFuture, result);
    }

    public static void addExceptionLogger(final ListenableFuture<?> listenableFuture) {
        delegate.addExceptionLogger(listenableFuture);
    }

    public static <T> void addPersistenceCallback(final ListenableFuture<T> future, final FutureCallback<? super T> callback) {
        delegate.addPersistenceCallback(future, callback);
    }

    public static <T, S> void multiFutureResult(@NotNull final Map<ListenableFuture<T>, Function<T>> futures, final S resultSum, final SettableFuture<S> resultSumFuture) {
        delegate.multiFutureResult(futures, resultSum, resultSumFuture);
    }

    public static <T, S> void multiFutureResult(@NotNull final Map<ListenableFuture<T>, Function<T>> futures, final S resultSum, final SettableFuture<S> resultSumFuture, final ExecutorService executorService) {
        delegate.multiFutureResult(futures, resultSum, resultSumFuture, executorService);
    }

    public static <E, C extends Collection<Set<E>>> ListenableFuture<Set<E>> combineSetResults(final ListenableFuture<C> collectionFuture) {
        return delegate.combineSetResults(collectionFuture);
    }

    public static <E, C extends Collection<Set<E>>> ListenableFuture<Set<E>> combineSetResults(final ListenableFuture<C> collectionFuture, final ExecutorService executorService) {
        return delegate.combineSetResults(collectionFuture, executorService);
    }

    public interface Function<T> {
        void function(final T result);
    }

    public static ListenableFuture<Boolean> voidToBoolFuture(final ListenableFuture<Void> future, final boolean value) {
        return delegate.voidToBoolFuture(future, value);
    }
}
