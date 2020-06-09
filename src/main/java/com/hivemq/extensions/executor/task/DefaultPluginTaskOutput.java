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
import com.hivemq.extension.sdk.api.async.TimeoutFallback;

/**
 * @author Christoph Schäbel
 */
public class DefaultPluginTaskOutput implements PluginTaskOutput {

    private static final DefaultPluginTaskOutput INSTANCE = new DefaultPluginTaskOutput();

    @NotNull
    public static DefaultPluginTaskOutput getInstance() {
        return INSTANCE;
    }

    private DefaultPluginTaskOutput() {
    }


    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void markAsAsync() {
        //noop
    }

    @Override
    public boolean isTimedOut() {
        return false;
    }

    @Override
    public void markAsTimedOut() {
        //noop
    }

    @Override
    public void resetAsyncStatus() {
        //noop
    }

    @Nullable
    @Override
    public SettableFuture<Boolean> getAsyncFuture() {
        return null;
    }

    @Override
    public @NotNull TimeoutFallback getTimeoutFallback() {
        return TimeoutFallback.FAILURE;
    }
}
