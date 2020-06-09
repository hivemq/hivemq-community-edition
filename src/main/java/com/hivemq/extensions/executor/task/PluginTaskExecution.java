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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * This class is not thread-safe
 *
 * @author Christoph Schäbel
 */
public class PluginTaskExecution<I extends PluginTaskInput, O extends PluginTaskOutput> {

    @NotNull
    private final PluginTaskContext pluginInOutContext;
    @Nullable
    private final Supplier<I> pluginInputSupplier;
    @Nullable
    private final Supplier<O> pluginOutputSupplier;
    @NotNull
    private final PluginTask pluginTask;

    @Nullable
    private O output;

    @Nullable
    private I input;

    @NotNull
    private final AtomicBoolean async = new AtomicBoolean(false);

    @NotNull
    private final AtomicBoolean done = new AtomicBoolean(false);

    public PluginTaskExecution(
            @NotNull final PluginTaskContext pluginInOutContext,
            @Nullable final Supplier<I> pluginInputSupplier,
            @Nullable final Supplier<O> pluginOutputSupplier,
            @NotNull final PluginTask pluginTask) {
        this.pluginInOutContext = pluginInOutContext;
        this.pluginInputSupplier = pluginInputSupplier;
        this.pluginOutputSupplier = pluginOutputSupplier;
        this.pluginTask = pluginTask;
    }

    @NotNull
    public PluginTaskContext getPluginContext() {
        return pluginInOutContext;
    }

    @NotNull
    public PluginTask getPluginTask() {
        return pluginTask;
    }

    public boolean isAsync() {
        return async.get();
    }

    public boolean isDone() {
        return done.get();
    }

    public void markAsAsync() {
        this.async.set(true);
    }

    public void markAsDone() {
        this.done.set(true);
    }

    @Nullable
    public O getOutputObject() {
        if (output == null && pluginOutputSupplier != null) {
            output = pluginOutputSupplier.get();
        }
        return output;
    }

    @Nullable
    public I getInputObject() {
        if (input == null && pluginInputSupplier != null) {
            input = pluginInputSupplier.get();
        }
        return input;
    }

    public void setOutputObject(@NotNull final O output) {
        this.output = output;
    }
}
