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

package com.hivemq.extensions.executor.task;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Christoph Schäbel
 */
public class AbstractAsyncOutput<T> implements PluginTaskOutput, AsyncOutput<T> {

    @NotNull
    protected final PluginOutPutAsyncer asyncer;

    @NotNull
    protected TimeoutFallback timeoutFallback = TimeoutFallback.FAILURE;

    private final AtomicBoolean async = new AtomicBoolean(false);
    private final AtomicBoolean called = new AtomicBoolean(false);
    private final AtomicBoolean timedOut = new AtomicBoolean(false);
    private final SettableFuture<Boolean> asyncFuture = SettableFuture.create();


    public AbstractAsyncOutput(@NotNull final PluginOutPutAsyncer asyncer) {
        this.asyncer = asyncer;
    }

    @NotNull
    @Override
    public Async<T> async(@NotNull final Duration timeout, @NotNull final TimeoutFallback fallback) {
        checkCalled();
        this.timeoutFallback = fallback;
        //noinspection unchecked: this cast is safe since this implements AsyncOutput and PluginTaskOutput
        return (Async<T>) asyncer.asyncify(this, timeout);
    }

    @NotNull
    @Override
    public Async<T> async(@NotNull final Duration timeout) {
        return async(timeout, TimeoutFallback.FAILURE);
    }

    private void checkCalled() {
        if (!called.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("async must not be called more than once");
        }
    }

    @Override
    public boolean isAsync() {
        return async.get();
    }

    @Override
    public void markAsAsync() {
        async.set(true);
    }

    @Override
    public boolean isTimedOut() {
        return timedOut.get();
    }

    @Override
    public void markAsTimedOut() {
        timedOut.set(true);
    }

    public void resetAsyncStatus() {
        timedOut.set(false);
        async.set(false);
        called.set(false);
    }

    @Nullable
    @Override
    public SettableFuture<Boolean> getAsyncFuture() {
        return asyncFuture;
    }

    @NotNull
    @Override
    public TimeoutFallback getTimeoutFallback() {
        return timeoutFallback;
    }
}
