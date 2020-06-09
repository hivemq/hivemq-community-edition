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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extensions.executor.task.AsyncOutputImpl;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.util.ThreadFactoryUtil;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class PluginOutputAsyncerImpl implements PluginOutPutAsyncer {

    @NotNull
    private final ScheduledExecutorService scheduledExecutor;

    @NotNull
    private final ShutdownHooks shutdownHooks;

    @PostConstruct
    public void postConstruct() {

        shutdownHooks.add(new PluginOutputAsyncerShutdownHook(scheduledExecutor));
    }

    @Inject
    public PluginOutputAsyncerImpl(@NotNull final ShutdownHooks shutdownHooks) {
        this.shutdownHooks = shutdownHooks;

        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        //enable removing canceled tasks from the executor queue
        executor.setRemoveOnCancelPolicy(true);
        executor.setThreadFactory(ThreadFactoryUtil.create("extension-timeout-executor-%d"));
        //for instrumentation (metrics)
        scheduledExecutor = executor;
    }

    @Override
    public @NotNull <T extends PluginTaskOutput> Async<T> asyncify(@NotNull final T output,
                                                                   @NotNull final Duration timeout) {

        output.markAsAsync();

        final SettableFuture<Boolean> asyncFuture = output.getAsyncFuture();

        Preconditions.checkNotNull(asyncFuture, "Async future cannot be null for async output");

        final ScheduledFuture<?> scheduledFuture = scheduledExecutor.schedule(() -> {
            output.markAsTimedOut();
            asyncFuture.set(false);
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);

        return new AsyncOutputImpl<>(output, asyncFuture, scheduledFuture);
    }


    static class PluginOutputAsyncerShutdownHook extends HiveMQShutdownHook {
        @NotNull
        private final ScheduledExecutorService scheduledExecutor;

        @VisibleForTesting
        PluginOutputAsyncerShutdownHook(@NotNull final ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
        }

        @Override
        public @NotNull String name() {
            return "Extension Timeout Executor Shutdown Hook";
        }

        @Override
        public @NotNull Priority priority() {
            return Priority.DOES_NOT_MATTER;
        }

        @Override
        public boolean isAsynchronous() {
            return false;
        }

        @Override
        public void run() {
            scheduledExecutor.shutdown();
        }
    }
}
