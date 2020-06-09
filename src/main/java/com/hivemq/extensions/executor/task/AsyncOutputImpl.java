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

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Christoph Schäbel
 */
public class AsyncOutputImpl<T extends PluginTaskOutput> implements Async<T> {

    @NotNull
    private final T output;

    @NotNull
    private final SettableFuture<Boolean> asyncFuture;

    @NotNull
    private final ScheduledFuture<?> timeoutTaskFuture;

    @NotNull
    private Status status;

    public AsyncOutputImpl(@NotNull final T output,
                           @NotNull final SettableFuture<Boolean> asyncFuture,
                           @NotNull final ScheduledFuture<?> timeoutTaskFuture) {
        this.output = output;
        this.asyncFuture = asyncFuture;
        this.timeoutTaskFuture = timeoutTaskFuture;
        status = Status.RUNNING;
    }

    @Override
    public void resume() {
        timeoutTaskFuture.cancel(true);
        status = Status.DONE;
        asyncFuture.set(true);
    }

    @NotNull
    @Override
    public T getOutput() {
        return output;
    }

    @NotNull
    @Override
    public Status getStatus() {
        if (asyncFuture.isDone()) {
            try {
                //timeout is not needed here, because the future is already done, but better safe than sorry
                return asyncFuture.get(1, TimeUnit.SECONDS) ? Status.DONE : Status.CANCELED;
            } catch (final Exception e) {
                return Status.CANCELED;
            }
        }
        return status;
    }
}
