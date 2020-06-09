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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.services.CompletableScheduledFuture;

import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class CompletableScheduledFutureImpl<T> extends CompletableScheduledFuture<T> {

    @Nullable
    private ScheduledFuture<?> scheduledFuture;

    @Override
    public long getDelay(@NotNull final TimeUnit unit) {
        return Objects.requireNonNull(scheduledFuture).getDelay(unit);
    }

    @Override
    public int compareTo(@NotNull final Delayed o) {
        return Objects.requireNonNull(scheduledFuture).compareTo(o);
    }

    public void setScheduledFuture(@NotNull final ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    @Nullable
    public ScheduledFuture<?> getScheduledFuture() {
        return scheduledFuture;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        Objects.requireNonNull(scheduledFuture).cancel(mayInterruptIfRunning);
        return super.cancel(mayInterruptIfRunning);
    }
}
