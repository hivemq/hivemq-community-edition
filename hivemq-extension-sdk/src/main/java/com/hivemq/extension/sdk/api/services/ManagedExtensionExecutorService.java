/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extension.sdk.api.services;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A shared thread pool executor which is a {@link ScheduledExecutorService}.
 * It is recommended to use this instead of creating your own thread(-pool) in your extension.
 * <p>
 * Use this class for all concurrent code.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface ManagedExtensionExecutorService extends ScheduledExecutorService {

    /**
     * DO NOT CALL THIS METHOD!
     * <p>
     * The Extension Executor Service is automatically shut down when HiveMQ is shut down.
     * <p>
     * Manual calls to this method from the extension system are not supported.
     *
     * @throws UnsupportedOperationException If it should be called.
     * @since 4.0.0
     */
    @Deprecated
    @Override
    default void shutdown() {
        throw new UnsupportedOperationException("ManagedExtensionExecutorService must not be shut down manually");
    }

    /**
     * DO NOT CALL THIS METHOD!
     * <p>
     * The Extension Executor Service is automatically shut down when HiveMQ shuts down.
     * <p>
     * Manual calls to this method from the extension system are not supported.
     *
     * @throws UnsupportedOperationException If it should be called.
     * @since 4.0.0
     */
    @Deprecated
    @Override
    default @NotNull List<@NotNull Runnable> shutdownNow() {
        throw new UnsupportedOperationException("ManagedExtensionExecutorService must not be shut down manually");
    }

    /**
     * @return A {@link CompletableFuture} representing pending completion of the task.
     * @see ScheduledExecutorService#submit(Runnable)
     * @since 4.0.0
     */
    @Override
    @NotNull CompletableFuture<?> submit(@NotNull Runnable task);

    /**
     * @return A {@link CompletableFuture} representing pending completion of the task.
     * @see ScheduledExecutorService#submit(Callable)
     * @since 4.0.0
     */
    @Override
    <T> @NotNull CompletableFuture<T> submit(@NotNull Callable<T> task);

    /**
     * @return A {@link CompletableFuture} representing pending completion of the task.
     * @see ScheduledExecutorService#submit(Runnable, Object)
     * @since 4.0.0
     */
    @Override
    <T> @NotNull CompletableFuture<T> submit(@NotNull Runnable task, @NotNull T result);

    /**
     * @return A {@link CompletableScheduledFuture} representing pending completion of the task.
     * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
     * @since 4.0.0
     */
    @Override
    @NotNull CompletableScheduledFuture<?> schedule(
            @NotNull Runnable command, long delay, @NotNull TimeUnit unit);

    /**
     * @return A {@link CompletableScheduledFuture} representing pending completion of the task.
     * @see ScheduledExecutorService#schedule(Callable, long, TimeUnit)
     * @since 4.0.0
     */
    @Override
    <V> @NotNull CompletableScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit);

    /**
     * @return A {@link CompletableScheduledFuture} representing pending completion of the task.
     * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
     * @since 4.0.0
     */
    @Override
    @NotNull CompletableScheduledFuture<?> scheduleAtFixedRate(
            @NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit);

    /**
     * @return A {@link CompletableScheduledFuture} representing pending completion of the task.
     * @see ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)
     * @since 4.0.0
     */
    @Override
    @NotNull CompletableScheduledFuture<?> scheduleWithFixedDelay(
            @NotNull Runnable command, long initialDelay, long delay, @NotNull TimeUnit unit);
}