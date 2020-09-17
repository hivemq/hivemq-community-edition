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
import com.hivemq.extension.sdk.api.services.CompletableScheduledFuture;
import com.hivemq.extensions.HiveMQExtensions;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class WrappedScheduledRunnable implements Runnable {

    @NotNull
    private final Runnable runnable;

    @NotNull
    private final ClassLoader classLoader;

    @NotNull
    private final CompletableScheduledFuture<?> future;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    WrappedScheduledRunnable(
            @NotNull final Runnable runnable, @NotNull final ClassLoader classLoader,
            @NotNull final CompletableScheduledFuture<?> future, @NotNull final HiveMQExtensions hiveMQExtensions) {
        this.runnable = runnable;
        this.classLoader = classLoader;
        this.future = future;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    public void run() {

        if (hiveMQExtensions.getExtensionForClassloader(classLoader) == null) {
            if (!future.isCancelled()) {
                future.cancel(true);
            }
        }

        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();

        try {

            Thread.currentThread().setContextClassLoader(classLoader);
            runnable.run();

            //scheduled futures don't complete normally so completable wont do either.

        } catch (final Throwable t) {
            future.completeExceptionally(t);
            throw t;
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
    }
}