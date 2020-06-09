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
package com.hivemq.extensions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
     * <p>
     * objects may be null or not
     */
    @NotNull
    public static <T, U> CompletableFuture<U> toCompletable(@NotNull final ListenableFuture<T> listenableFuture, @NotNull final Function<T, U> converter, final boolean nullableResult, final @NotNull Executor executor) {
        return createCompletable(listenableFuture, converter, nullableResult, executor);
    }

    /**
     * This method converts a ListenableFuture to a CompletableFuture
     * with different object types,
     * or the objects needs to be changed.
     * <p>
     * objects may be null
     */
    @NotNull
    public static <T, U> CompletableFuture<U> toCompletable(@NotNull final ListenableFuture<T> listenableFuture, @NotNull final Function<T, U> converter, final @NotNull Executor executor) {
        return createCompletable(listenableFuture, converter, true, executor);

    }

    /**
     * This method converts a ListenableFuture to a CompletableFuture
     * with equal object types,
     * and the object does not need to be changed.
     * <p>
     * objects may be null
     */
    @NotNull
    public static <T> CompletableFuture<T> toCompletable(@NotNull final ListenableFuture<T> listenableFuture, final @NotNull Executor executor) {
        return createCompletable(listenableFuture, Function.identity(), true, executor);
    }


    /**
     * This method converts any ListenableFuture to a CompletableFuture<Void>
     */
    @NotNull
    public static <T> CompletableFuture<Void> toVoidCompletable(@NotNull final ListenableFuture<T> listenableFuture, final @NotNull Executor executor) {
        return createCompletable(listenableFuture, result -> null, true, executor);
    }

    @NotNull
    private static <T, U> CompletableFuture<U> createCompletable(@NotNull final ListenableFuture<T> listenableFuture, @NotNull final Function<T, U> converter, final boolean nullableResult, final @NotNull Executor executor) {

        final ClassLoader callingThreadClassLoader = Thread.currentThread().getContextClassLoader();
        final CompletableFuture<U> completableFuture = new CompletableFuture<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                final boolean cancelled = listenableFuture.cancel(mayInterruptIfRunning);
                final ClassLoader current = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(callingThreadClassLoader);
                    super.cancel(cancelled);
                    return cancelled;
                } finally {
                    Thread.currentThread().setContextClassLoader(current);
                }
            }
        };

        Futures.addCallback(listenableFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final T result) {
                final ClassLoader current = Thread.currentThread().getContextClassLoader();
                try {
                    if (nullableResult && result == null) {
                        Thread.currentThread().setContextClassLoader(callingThreadClassLoader);
                        completableFuture.complete(null);
                    } else {
                        //apply in current class loader
                        final U convertedResult = converter.apply(result);
                        //complete in previous
                        Thread.currentThread().setContextClassLoader(callingThreadClassLoader);
                        completableFuture.complete(convertedResult);
                    }
                } catch (final Throwable throwable) {
                    Thread.currentThread().setContextClassLoader(callingThreadClassLoader);
                    completableFuture.completeExceptionally(throwable);
                } finally {
                    Thread.currentThread().setContextClassLoader(current);
                }
            }

            @Override
            public void onFailure(@NotNull final Throwable ex) {
                final ClassLoader current = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(callingThreadClassLoader);
                    completableFuture.completeExceptionally(ex);
                } finally {
                    Thread.currentThread().setContextClassLoader(current);
                }
            }
        }, executor);
        return completableFuture;
    }

}