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
package com.hivemq.extensions.services.executor;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class WrappedRunnableWithResult<T> implements Runnable {

    @NotNull
    private final Runnable runnable;

    @NotNull
    private final ClassLoader classLoader;

    @NotNull
    private final CompletableFuture<T> future;

    @NotNull
    private final T result;

    WrappedRunnableWithResult(
            @NotNull final Runnable runnable, @NotNull final ClassLoader classLoader,
            @NotNull final CompletableFuture<T> future, @NotNull final T result) {
        this.runnable = runnable;
        this.classLoader = classLoader;
        this.future = future;
        this.result = result;
    }

    @Override
    public void run() {

        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            runnable.run();
            future.complete(result);
        } catch (final Throwable t) {
            future.completeExceptionally(t);
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
    }
}
