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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;

/**
 * @author Georg Held
 * @author Christoph Schäbel
 */
public interface PluginTaskOutput {

    /**
     * @return true if the extension method invoked the async handling
     */
    boolean isAsync();

    /**
     * Marks this output as output of an async execution
     */
    void markAsAsync();

    /**
     * @return true if the task's async timeout is exceeded, false if the task is still running or resume was called
     */
    boolean isTimedOut();

    /**
     * Marks this output as timed out
     */
    void markAsTimedOut();

    /**
     * resets the async and timeout status
     */
    void resetAsyncStatus();

    /**
     * @return a {@link SettableFuture} which is set to true if resume has been called on the
     * {@link AsyncOutput} and false if a timeout occurred
     */
    @Nullable
    SettableFuture<Boolean> getAsyncFuture();

    /**
     * @return the timeout fallback to use if the async operation times out
     */
    @NotNull
    TimeoutFallback getTimeoutFallback();
}
