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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;

import java.time.Duration;

/**
 * @author Christoph Schäbel
 */
public class AbstractAsyncOutput<T> extends AbstractSimpleAsyncOutput<T> implements AsyncOutput<T> {

    protected @NotNull TimeoutFallback timeoutFallback = TimeoutFallback.FAILURE;

    public AbstractAsyncOutput(final @NotNull PluginOutPutAsyncer asyncer) {
        super(asyncer);
    }

    @Override
    public @NotNull Async<T> async(final @NotNull Duration timeout, final @NotNull TimeoutFallback fallback) {
        Preconditions.checkNotNull(fallback, "Timeout fallback must never be null");
        final Async<T> async = async(timeout);
        this.timeoutFallback = fallback;
        return async;
    }

    @Override
    public @NotNull TimeoutFallback getTimeoutFallback() {
        return timeoutFallback;
    }
}
