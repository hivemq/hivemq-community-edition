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
package com.hivemq.extensions.executor.task;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yannick Weber
 */
public class AbstractSimpleAsyncOutput<T> implements PluginTaskOutput, SimpleAsyncOutput<T> {

    protected final @NotNull PluginOutPutAsyncer asyncer;

    private final @NotNull AtomicBoolean async = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean called = new AtomicBoolean(false);
    private final @NotNull AtomicBoolean timedOut = new AtomicBoolean(false);
    private final @NotNull SettableFuture<Boolean> asyncFuture = SettableFuture.create();

    public AbstractSimpleAsyncOutput(final @NotNull PluginOutPutAsyncer asyncer) {
        this.asyncer = asyncer;
    }

    @Override
    public @NotNull Async<T> async(final @NotNull Duration timeout) {
        Preconditions.checkNotNull(timeout, "Timeout duration must never be null");
        checkCalled();
        //noinspection unchecked: this cast is safe since this implements AsyncOutput and PluginTaskOutput
        return (Async<T>) asyncer.asyncify(this, timeout);
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

    @Override
    public void resetAsyncStatus() {
        timedOut.set(false);
        async.set(false);
        called.set(false);
    }

    @Override
    public @NotNull SettableFuture<Boolean> getAsyncFuture() {
        return asyncFuture;
    }

    @Override
    public @NotNull TimeoutFallback getTimeoutFallback() {
        return TimeoutFallback.FAILURE;
    }
}
