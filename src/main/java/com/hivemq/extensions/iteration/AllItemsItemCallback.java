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
package com.hivemq.extensions.iteration;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extensions.services.general.IterationContextImpl;

import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * @author Georg Held
 */
public class AllItemsItemCallback<T> implements AsyncIterator.ItemCallback<T> {

    private final @NotNull Executor callbackExecutor;
    private final @NotNull IterationCallback<T> callback;

    public AllItemsItemCallback(final @NotNull Executor callbackExecutor, final @NotNull IterationCallback<T> callback) {
        this.callbackExecutor = callbackExecutor;
        this.callback = callback;
    }

    @Override
    public @NotNull ListenableFuture<Boolean> onItems(final @NotNull Collection<T> items) {
        final IterationContextImpl iterationContext = new IterationContextImpl();
        final SettableFuture<Boolean> resultFuture = SettableFuture.create();

        callbackExecutor.execute(() -> {

            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(callback.getClass().getClassLoader());
                for (final T item : items) {

                    callback.iterate(iterationContext, item);

                    if (iterationContext.isAborted()) {
                        resultFuture.set(false);
                        return;
                    }
                }
            } catch (final Throwable t) {
                resultFuture.setException(t);
                return;
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
            resultFuture.set(true);
        });

        return resultFuture;
    }
}

