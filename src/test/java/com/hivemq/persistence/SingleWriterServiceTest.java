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

package com.hivemq.persistence;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.InternalConfigurations;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Lukas Brandl
 */
public class SingleWriterServiceTest {

    SingleWriterService singleWriterService;

    @Before
    public void setUp() throws Exception {
        InternalConfigurations.SINGLE_WRITER_THREAD_POOL_SIZE.set(4);
        InternalConfigurations.SINGLE_WRITER_CREDITS_PER_EXECUTION.set(200);
        InternalConfigurations.PERSISTENCE_SHUTDOWN_GRACE_PERIOD.set(200);
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(64);

        singleWriterService = new SingleWriterService();
    }

    @Test
    public void test_increment_non_empty_queue_count() throws Exception {
        singleWriterService.executorService = new NoOpExecutor();

        assertEquals(0, singleWriterService.getNonemptyQueueCounter().get());
        assertEquals(0, singleWriterService.getRunningThreadsCount().get());

        singleWriterService.incrementNonemptyQueueCounter();
        assertEquals(1, singleWriterService.getNonemptyQueueCounter().get());
        assertEquals(1, singleWriterService.getRunningThreadsCount().get());

        singleWriterService.incrementNonemptyQueueCounter();
        assertEquals(2, singleWriterService.getNonemptyQueueCounter().get());
        assertEquals(2, singleWriterService.getRunningThreadsCount().get());

        singleWriterService.incrementNonemptyQueueCounter();
        assertEquals(3, singleWriterService.getNonemptyQueueCounter().get());
        assertEquals(3, singleWriterService.getRunningThreadsCount().get());

        singleWriterService.incrementNonemptyQueueCounter();
        assertEquals(4, singleWriterService.getNonemptyQueueCounter().get());
        assertEquals(4, singleWriterService.getRunningThreadsCount().get());

        singleWriterService.incrementNonemptyQueueCounter();
        assertEquals(5, singleWriterService.getNonemptyQueueCounter().get());
        assertEquals(4, singleWriterService.getRunningThreadsCount().get());
    }

    @Test
    public void test_valid_amount_of_queues() throws Exception {


        assertEquals(1, singleWriterService.validAmountOfQueues(1, 64));
        assertEquals(2, singleWriterService.validAmountOfQueues(2, 64));
        assertEquals(4, singleWriterService.validAmountOfQueues(4, 64));
        assertEquals(8, singleWriterService.validAmountOfQueues(5, 64));
        assertEquals(8, singleWriterService.validAmountOfQueues(8, 64));
        assertEquals(64, singleWriterService.validAmountOfQueues(64, 64));
    }

    private static class NoOpExecutor implements ExecutorService {

        @Override
        public void shutdown() {
        }

        @NotNull
        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException {
            return false;
        }

        @NotNull
        @Override
        public <T> Future<T> submit(@NotNull final Callable<T> task) {
            return null;
        }

        @NotNull
        @Override
        public <T> Future<T> submit(@NotNull final Runnable task, final T result) {
            return null;
        }

        @NotNull
        @Override
        public Future<?> submit(@NotNull final Runnable task) {
            return SettableFuture.create();
        }

        @NotNull
        @Override
        public <T> List<Future<T>> invokeAll(@NotNull final Collection<? extends Callable<T>> tasks)
                throws InterruptedException {
            return null;
        }

        @NotNull
        @Override
        public <T> List<Future<T>> invokeAll(
                @NotNull final Collection<? extends Callable<T>> tasks, final long timeout,
                @NotNull final TimeUnit unit) throws InterruptedException {
            return null;
        }

        @NotNull
        @Override
        public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public <T> T invokeAny(
                @NotNull final Collection<? extends Callable<T>> tasks, final long timeout,
                @NotNull final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

        @Override
        public void execute(@NotNull final Runnable command) {

        }
    }
}