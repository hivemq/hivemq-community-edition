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

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static com.hivemq.configuration.service.InternalConfigurations.MANAGED_PLUGIN_EXECUTOR_SHUTDOWN_TIMEOUT;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@Singleton
public class GlobalManagedExtensionExecutorService implements ScheduledExecutorService {

    private static final Logger log = LoggerFactory.getLogger(GlobalManagedExtensionExecutorService.class);

    private final @NotNull ShutdownHooks shutdownHooks;

    private @Nullable ScheduledExecutorService scheduledExecutorService;
    private @Nullable ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    @Inject
    public GlobalManagedExtensionExecutorService(final @NotNull ShutdownHooks shutdownHooks) {
        this.shutdownHooks = shutdownHooks;
    }

    @PostConstruct
    public void postConstruct() {

        final ThreadFactory threadFactory = ThreadFactoryUtil.create("managed-extension-executor-%d");

        final int corePoolSize = InternalConfigurations.MANAGED_PLUGIN_THREAD_POOL_SIZE.get();
        final int keepAlive = InternalConfigurations.MANAGED_PLUGIN_THREAD_POOL_KEEP_ALIVE_SECONDS.get();

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
        log.debug("Set extension executor thread pool size to {}", corePoolSize);

        scheduledThreadPoolExecutor.setKeepAliveTime(keepAlive, TimeUnit.SECONDS);
        log.debug("Set extension executor thread pool keep-alive to {} seconds", keepAlive);

        scheduledThreadPoolExecutor.allowCoreThreadTimeOut(true);

        //for instrumentation (metrics)
        scheduledExecutorService = scheduledThreadPoolExecutor;

        shutdownHooks.add(new ManagedPluginExecutorShutdownHook(this, MANAGED_PLUGIN_EXECUTOR_SHUTDOWN_TIMEOUT.get()));
    }

    public int getCorePoolSize() {
        return Objects.requireNonNull(scheduledThreadPoolExecutor).getCorePoolSize();
    }

    public int getMaxPoolSize() {
        return Objects.requireNonNull(scheduledThreadPoolExecutor).getMaximumPoolSize();
    }

    public int getCurrentPoolSize() {
        return Objects.requireNonNull(scheduledThreadPoolExecutor).getPoolSize();
    }

    public long getKeepAliveSeconds() {
        return Objects.requireNonNull(scheduledThreadPoolExecutor).getKeepAliveTime(TimeUnit.SECONDS);
    }

    @NotNull
    public ScheduledFuture<?> schedule(
            @NotNull final Runnable command, final long delay, @NotNull final TimeUnit unit) {
        return Objects.requireNonNull(scheduledExecutorService).schedule(command, delay, unit);
    }

    @NotNull
    public <V> ScheduledFuture<V> schedule(
            @NotNull final Callable<V> callable, final long delay, @NotNull final TimeUnit unit) {
        return Objects.requireNonNull(scheduledExecutorService).schedule(callable, delay, unit);
    }

    @NotNull
    public ScheduledFuture<?> scheduleAtFixedRate(
            @NotNull final Runnable command, final long initialDelay, final long period, @NotNull final TimeUnit unit) {
        return Objects.requireNonNull(scheduledExecutorService)
                .scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @NotNull
    public ScheduledFuture<?> scheduleWithFixedDelay(
            @NotNull final Runnable command, final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
        return Objects.requireNonNull(scheduledExecutorService)
                .scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        Objects.requireNonNull(scheduledExecutorService).shutdown();
    }

    @Override
    public @NotNull List<Runnable> shutdownNow() {
        return Objects.requireNonNull(scheduledExecutorService).shutdownNow();
    }

    public boolean isShutdown() {
        return Objects.requireNonNull(scheduledExecutorService).isShutdown();
    }

    public boolean isTerminated() {
        return Objects.requireNonNull(scheduledExecutorService).isTerminated();
    }

    public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException {
        return Objects.requireNonNull(scheduledExecutorService).awaitTermination(timeout, unit);
    }

    @NotNull
    public <T> Future<T> submit(@NotNull final Callable<T> task) {
        return Objects.requireNonNull(scheduledExecutorService).submit(task);
    }

    @NotNull
    public <T> Future<T> submit(@NotNull final Runnable task, @NotNull final T result) {
        return Objects.requireNonNull(scheduledExecutorService).submit(task, result);
    }

    @NotNull
    public Future<?> submit(@NotNull final Runnable task) {
        return Objects.requireNonNull(scheduledExecutorService).submit(task);
    }

    @NotNull
    public <T> List<Future<T>> invokeAll(@NotNull final Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return Objects.requireNonNull(scheduledExecutorService).invokeAll(tasks);
    }

    @NotNull
    public <T> List<Future<T>> invokeAll(
            @NotNull final Collection<? extends Callable<T>> tasks, final long timeout, @NotNull final TimeUnit unit)
            throws InterruptedException {
        return Objects.requireNonNull(scheduledExecutorService).invokeAll(tasks, timeout, unit);
    }

    @NotNull
    public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return Objects.requireNonNull(scheduledExecutorService).invokeAny(tasks);
    }

    @NotNull
    public <T> T invokeAny(
            @NotNull final Collection<? extends Callable<T>> tasks, final long timeout, @NotNull final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return Objects.requireNonNull(scheduledExecutorService).invokeAny(tasks, timeout, unit);
    }

    public void execute(@NotNull final Runnable command) {
        Objects.requireNonNull(scheduledExecutorService).execute(command);
    }
}
