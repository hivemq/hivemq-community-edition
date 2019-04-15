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

package com.hivemq.extensions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ListenableFutureConverter {

    /**
     * This method converts a ListenableFuture to a CompletableFuture
     * with different object types,
     * or the objects needs to be changed.
     *
     * objects may be null or not
     *
     */
    @NotNull
    public static <T, U> CompletableFuture<U> toCompletable(@NotNull final ListenableFuture<T> listenableFuture, @NotNull final Function<T, U> converter, final boolean nullableResult) {
        return createCompletable(listenableFuture, converter, nullableResult);

    }

    /**
     * This method converts a ListenableFuture to a CompletableFuture
     * with different object types,
     * or the objects needs to be changed.
     *
     * objects may be null
     *
     */
    @NotNull
    public static <T, U> CompletableFuture<U> toCompletable(@NotNull final ListenableFuture<T> listenableFuture, @NotNull final Function<T, U> converter) {
        return createCompletable(listenableFuture, converter, true);

    }

    @NotNull
    public static <T> CompletableFuture<Void> toVoidCompletable(@NotNull final ListenableFuture<T> listenableFuture) {
        return createCompletable(listenableFuture, result -> null, true);
    }

    /**
     * This method converts a ListenableFuture to a CompletableFuture
     * with equal object types,
     * and the object does not need to be changed.
     *
     * objects may be null
     *
     */

    @NotNull
    public static <T> CompletableFuture<T> toCompletable(@NotNull final ListenableFuture<T> listenableFuture) {
        return createCompletable(listenableFuture, Function.identity(), true);
    }

    @NotNull
    private static <T, U> CompletableFuture<U> createCompletable(@NotNull final ListenableFuture<T> listenableFuture, @NotNull final Function<T, U> converter, final boolean nullableResult) {
        final CompletableFuture<U> completableFuture = new CompletableFuture<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                final boolean cancelled = listenableFuture.cancel(mayInterruptIfRunning);
                super.cancel(cancelled);
                return cancelled;
            }
        };

        Futures.addCallback(listenableFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final T result) {
                try {
                    if (nullableResult && result == null) {
                        completableFuture.complete(null);
                    } else {
                        completableFuture.complete(converter.apply(result));
                    }
                } catch (final Throwable throwable) {
                    completableFuture.completeExceptionally(throwable);
                }
            }

            @Override
            public void onFailure(@NotNull final Throwable ex) {
                completableFuture.completeExceptionally(ex);
            }
        }, MoreExecutors.directExecutor());
        return completableFuture;
    }

}