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
package com.hivemq.extensions.executor;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;

import java.time.Duration;

/**
 * Interface that handles the async option for the diverse {@link AsyncOutput}s.
 *
 * @author Georg Held
 * @author Christoph Schäbel
 */
public interface PluginOutPutAsyncer {

    /**
     * @param output  the original {@link PluginTaskOutput} implementation.
     * @param timeout the timeout after which the {@code outputTimeoutTransformation} should be
     *                applied to the {@code <T>} instead of the real {@link
     *                com.hivemq.extensions.executor.task.PluginTask}
     * @param <T>     an implementation of {@link com.hivemq.extensions.executor.task.PluginTaskOutput}
     * @return an {@link Async} of {@code <T>}
     */
    @NotNull <T extends PluginTaskOutput> Async<T> asyncify(@NotNull final T output,
                                                            @NotNull final Duration timeout);
}