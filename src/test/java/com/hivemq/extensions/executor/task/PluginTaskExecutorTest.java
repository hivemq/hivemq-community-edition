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
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class PluginTaskExecutorTest {

    private PluginTaskExecutor pluginTaskExecutor;

    private List<Integer> executionOrder;

    @Mock
    IsolatedExtensionClassloader classloader;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        executionOrder = Collections.synchronizedList(new ArrayList<>());

        pluginTaskExecutor = new PluginTaskExecutor(new AtomicLong(0));
        pluginTaskExecutor.postConstruct();
    }

    @After
    public void after() {
        pluginTaskExecutor.stop();
    }

    @Test(timeout = 5000)
    public void test_inout_task_is_executed() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        addTask(pluginTaskExecutor, latch, "client", false, 1, executionOrder, 0, classloader);

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_inout_tasks_for_same_client_are_executed_in_order() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addTask(pluginTaskExecutor, latch, "clientid", false, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));

        assertEquals(tries, executionOrder.size());
        for (int i = 0; i < tries; i++) {
            assertEquals(i, executionOrder.get(i).intValue());
        }
    }

    @Test(timeout = 5000)
    public void test_multiple_inout_tasks_for_different_clients_are_executed() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void test_async_inout_task_is_executed() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        addTask(pluginTaskExecutor, latch, "client", true, 1, executionOrder, 0, classloader);

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_inout_async_tasks_for_same_client_are_executed_in_order() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addTask(pluginTaskExecutor, latch, "clientid", true, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));

        assertEquals(tries, executionOrder.size());
        for (int i = 0; i < tries; i++) {
            assertEquals(i, executionOrder.get(i).intValue());
        }
    }

    @Test(timeout = 5000)
    public void test_multiple_inout_async_tasks_for_different_clients_are_executed() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }


    @Test(timeout = 5000)
    public void test_multiple_inout_async_tasks_for_different_clients_from_different_producers_are_executed() throws Exception {

        final int tries = 250;
        final int threads = 4;
        final CountDownLatch latch = new CountDownLatch(tries * threads);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);


        for (int j = 0; j < threads; j++) {
            final int finalJ = j;
            executorService.execute(() -> {
                for (int i = finalJ * tries; i < (tries * finalJ) + tries; i++) {
                    addTask(pluginTaskExecutor, latch, "" + (i % 100), true, i, executionOrder, 0, classloader);
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_inout_tasks_for_different_clients_from_different_producers_are_executed() throws Exception {

        final int tries = 250;
        final int threads = 4;
        final CountDownLatch latch = new CountDownLatch(tries * threads);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);


        for (int j = 0; j < threads; j++) {
            final int finalJ = j;
            executorService.execute(() -> {
                for (int i = finalJ * tries; i < (tries * finalJ) + tries; i++) {
                    addTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 0, classloader);
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_inout_async_tasks_for_different_clients_from_different_producers_are_executed_delay() throws Exception {

        final int tries = 250;
        final int threads = 4;
        final CountDownLatch latch = new CountDownLatch(tries * threads);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);


        for (int j = 0; j < threads; j++) {
            final int finalJ = j;
            executorService.execute(() -> {
                for (int i = finalJ * tries; i < (tries * finalJ) + tries; i++) {
                    addTask(pluginTaskExecutor, latch, "" + (i % 100), true, i, executionOrder, 1, classloader);
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_inout_tasks_for_different_clients_from_different_producers_are_executed_delay() throws Exception {

        final int tries = 250;
        final int threads = 4;
        final CountDownLatch latch = new CountDownLatch(tries * threads);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);


        for (int j = 0; j < threads; j++) {
            final int finalJ = j;
            executorService.execute(() -> {
                for (int i = finalJ * tries; i < (tries * finalJ) + tries; i++) {
                    addTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 1, classloader);
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_out_task_is_executed() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        addOutTask(pluginTaskExecutor, latch, "client", false, 1, executionOrder, 0, classloader);

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_out_tasks_for_same_client_are_executed_in_order() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addOutTask(pluginTaskExecutor, latch, "clientid", false, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));

        assertEquals(tries, executionOrder.size());
        for (int i = 0; i < tries; i++) {
            assertEquals(i, executionOrder.get(i).intValue());
        }
    }

    @Test(timeout = 5000)
    public void test_multiple_out_tasks_for_different_clients_are_executed() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addOutTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_async_out_task_is_executed() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        addOutTask(pluginTaskExecutor, latch, "client", true, 1, executionOrder, 0, classloader);

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_out_async_tasks_for_same_client_are_executed_in_order() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addOutTask(pluginTaskExecutor, latch, "clientid", true, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));

        assertEquals(tries, executionOrder.size());
        for (int i = 0; i < tries; i++) {
            assertEquals(i, executionOrder.get(i).intValue());
        }
    }

    @Test(timeout = 5000)
    public void test_multiple_out_async_tasks_for_different_clients_are_executed() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addOutTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }


    @Test(timeout = 5000)
    public void test_multiple_out_async_tasks_for_different_clients_from_different_producers_are_executed() throws Exception {

        final int tries = 250;
        final int threads = 4;
        final CountDownLatch latch = new CountDownLatch(tries * threads);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);


        for (int j = 0; j < threads; j++) {
            final int finalJ = j;
            executorService.execute(() -> {
                for (int i = finalJ * tries; i < (tries * finalJ) + tries; i++) {
                    addOutTask(pluginTaskExecutor, latch, "" + (i % 100), true, i, executionOrder, 0, classloader);
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_out_tasks_for_different_clients_from_different_producers_are_executed() throws Exception {

        final int tries = 250;
        final int threads = 4;
        final CountDownLatch latch = new CountDownLatch(tries * threads);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);


        for (int j = 0; j < threads; j++) {
            final int finalJ = j;
            executorService.execute(() -> {
                for (int i = finalJ * tries; i < (tries * finalJ) + tries; i++) {
                    addOutTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 0, classloader);
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_out_async_tasks_for_different_clients_from_different_producers_are_executed_delay() throws Exception {

        final int tries = 250;
        final int threads = 4;
        final CountDownLatch latch = new CountDownLatch(tries * threads);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);


        for (int j = 0; j < threads; j++) {
            final int finalJ = j;
            executorService.execute(() -> {
                for (int i = finalJ * tries; i < (tries * finalJ) + tries; i++) {
                    addOutTask(pluginTaskExecutor, latch, "" + (i % 100), true, i, executionOrder, 1, classloader);
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_out_tasks_for_different_clients_from_different_producers_are_executed_delay() throws Exception {

        final int tries = 250;
        final int threads = 4;
        final CountDownLatch latch = new CountDownLatch(tries * threads);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);


        for (int j = 0; j < threads; j++) {
            final int finalJ = j;
            executorService.execute(() -> {
                for (int i = finalJ * tries; i < (tries * finalJ) + tries; i++) {
                    addOutTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 1, classloader);
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_in_task_is_executed() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        addInTask(pluginTaskExecutor, latch, "client", false, 1, executionOrder, 0, classloader);

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_in_tasks_for_same_client_are_executed_in_order() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addInTask(pluginTaskExecutor, latch, "clientid", false, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));

        assertEquals(tries, executionOrder.size());
        for (int i = 0; i < tries; i++) {
            assertEquals(i, executionOrder.get(i).intValue());
        }
    }

    @Test(timeout = 5000)
    public void test_multiple_in_tasks_for_different_clients_are_executed() throws Exception {

        final int tries = 1000;
        final CountDownLatch latch = new CountDownLatch(tries);

        for (int i = 0; i < tries; i++) {
            addInTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 0, classloader);
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_multiple_in_tasks_for_different_clients_from_different_producers_are_executed_delay() throws Exception {

        final int tries = 250;
        final int threads = 4;
        final CountDownLatch latch = new CountDownLatch(tries * threads);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);


        for (int j = 0; j < threads; j++) {
            final int finalJ = j;
            executorService.execute(() -> {
                for (int i = finalJ * tries; i < (tries * finalJ) + tries; i++) {
                    addInTask(pluginTaskExecutor, latch, "" + (i % 100), false, i, executionOrder, 1, classloader);
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void test_task_throws_exception_queue_can_continue() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        //add task which throws exception
        addExceptionTask(pluginTaskExecutor, latch, false, executionOrder, classloader);


        //add a normal task
        addTask(pluginTaskExecutor, latch, "client", false, 1, executionOrder, 0, classloader);

        //check if both tasks are executed
        assertTrue(latch.await(30, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_async_task_throws_exception_queue_can_continue() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        //add task which throws exception
        addExceptionTask(pluginTaskExecutor, latch, true, executionOrder, classloader);


        //add a normal task
        addTask(pluginTaskExecutor, latch, "client", true, 1, executionOrder, 0, classloader);

        //check if both tasks are executed
        latch.await(30, TimeUnit.SECONDS);

    }

    @Test(timeout = 5000)
    public void test_post_throws_exception_queue_can_continue() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        //add task which throws exception
        addExceptionPostTask(pluginTaskExecutor, latch, false, executionOrder, classloader);


        //add a normal task
        addTask(pluginTaskExecutor, latch, "client", false, 1, executionOrder, 0, classloader);

        //check if both tasks are executed
        assertTrue(latch.await(30, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_async_post_throws_exception_queue_can_continue() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        //add task which throws exception
        addExceptionPostTask(pluginTaskExecutor, latch, true, executionOrder, classloader);


        //add a normal task
        addTask(pluginTaskExecutor, latch, "client", true, 1, executionOrder, 0, classloader);

        //check if both tasks are executed
        assertTrue(latch.await(30, TimeUnit.SECONDS));

    }


    @Test(timeout = 5000)
    public void test_async_throws_exception_queue_can_continue() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        //add task which throws exception
        addExceptionAsyncTask(pluginTaskExecutor, latch, executionOrder, classloader);


        //add a normal task
        addTask(pluginTaskExecutor, latch, "client", true, 1, executionOrder, 0, classloader);

        //check if both tasks are executed
        assertTrue(latch.await(30, TimeUnit.SECONDS));

    }

    private static void addTask(final PluginTaskExecutor pluginTaskExecutor,
                                @NotNull final CountDownLatch latch,
                                @NotNull final String clientId,
                                final boolean async,
                                final int number,
                                @NotNull final List<Integer> executionOrder,
                                final int delay,
                                @NotNull final IsolatedExtensionClassloader classloader) {
        pluginTaskExecutor.handlePluginTaskExecution(
                new PluginTaskExecution<>(new TestPluginInOutContext(clientId),
                        () -> new TestPluginTaskInput(),
                        () -> async ? new TestPluginTaskOutputAsync() : new TestPluginTaskOutput(),
                        new TestPluginInOutTask(latch, number, executionOrder, delay, classloader)));
    }

    private static void addOutTask(final PluginTaskExecutor pluginTaskExecutor,
                                   @NotNull final CountDownLatch latch,
                                   @NotNull final String clientId,
                                   final boolean async,
                                   final int number,
                                   @NotNull final List<Integer> executionOrder,
                                   final int delay,
                                   @NotNull final IsolatedExtensionClassloader classloader) {
        pluginTaskExecutor.handlePluginTaskExecution(
                new PluginTaskExecution<>(new TestPluginOutContext(clientId),
                        null,
                        () -> async ? new TestPluginTaskOutputAsync() : new TestPluginTaskOutput(),
                        new TestPluginOutTask(latch, number, executionOrder, delay, classloader)));
    }

    private static void addInTask(final PluginTaskExecutor pluginTaskExecutor,
                                  @NotNull final CountDownLatch latch,
                                  @NotNull final String clientId,
                                  final boolean async,
                                  final int number,
                                  @NotNull final List<Integer> executionOrder,
                                  final int delay,
                                  @NotNull final IsolatedExtensionClassloader classloader) {
        pluginTaskExecutor.handlePluginTaskExecution(
                new PluginTaskExecution<TestPluginTaskInput, DefaultPluginTaskOutput>(new TestPluginInContext(clientId),
                        () -> new TestPluginTaskInput(),
                        null,
                        new TestPluginInTask(latch, number, executionOrder, delay, classloader)));
    }

    private static void addExceptionTask(final PluginTaskExecutor pluginTaskExecutor,
                                         @NotNull final CountDownLatch latch,
                                         final boolean async,
                                         @NotNull final List<Integer> executionOrder,
                                         @NotNull final IsolatedExtensionClassloader classloader) {
        pluginTaskExecutor.handlePluginTaskExecution(
                new PluginTaskExecution<>(new TestPluginInOutContext("client"),
                        () -> new TestPluginTaskInput(),
                        () -> async ? new TestPluginTaskOutputAsync() : new TestPluginTaskOutput(),
                        new TestPluginInOutexceptionTask(latch, 1, executionOrder, 0, classloader)));
    }

    private static void addExceptionPostTask(final PluginTaskExecutor pluginTaskExecutor,
                                             @NotNull final CountDownLatch latch,
                                             final boolean async,
                                             @NotNull final List<Integer> executionOrder,
                                             @NotNull final IsolatedExtensionClassloader classloader) {
        pluginTaskExecutor.handlePluginTaskExecution(
                new PluginTaskExecution<>(new TestPluginInOutExceptionContext("client"),
                        () -> new TestPluginTaskInput(),
                        () -> async ? new TestPluginTaskOutputAsync() : new TestPluginTaskOutput(),
                        new TestPluginInOutTask(latch, 1, executionOrder, 0, classloader)));
    }

    private static void addExceptionAsyncTask(final PluginTaskExecutor pluginTaskExecutor,
                                              @NotNull final CountDownLatch latch,
                                              @NotNull final List<Integer> executionOrder,
                                              @NotNull final IsolatedExtensionClassloader classloader) {
        pluginTaskExecutor.handlePluginTaskExecution(
                new PluginTaskExecution<>(new TestPluginInOutContext("client"),
                        () -> new TestPluginTaskInput(),
                        () -> new TestPluginTaskOutputExceptionAsync(),
                        new TestPluginInOutTask(latch, 1, executionOrder, 0, classloader)));
    }


    private static class TestPluginTaskInput implements PluginTaskInput {

    }

    private static class TestPluginInOutContext extends PluginInOutTaskContext<TestPluginTaskOutput> {

        TestPluginInOutContext(@NotNull final String identifier) {
            super(identifier);
        }

        @Override
        public void pluginPost(@NotNull final TestPluginTaskOutput pluginOutput) {

        }
    }

    private static class TestPluginOutContext extends PluginOutTaskContext<TestPluginTaskOutput> {

        TestPluginOutContext(@NotNull final String identifier) {
            super(identifier);
        }

        @Override
        public void pluginPost(@NotNull final TestPluginTaskOutput pluginOutput) {

        }
    }

    private static class TestPluginInContext extends PluginInTaskContext {

        TestPluginInContext(@NotNull final String identifier) {
            super(identifier);
        }

    }

    private static class TestPluginInOutExceptionContext extends PluginInOutTaskContext<TestPluginTaskOutput> {

        TestPluginInOutExceptionContext(@NotNull final String identifier) {
            super(identifier);
        }

        @Override
        public void pluginPost(@NotNull final TestPluginTaskOutput pluginOutput) {
            throw new RuntimeException("Test-Exception");
        }
    }

    private static class TestPluginTaskOutput implements PluginTaskOutput {

        @Override
        public boolean isAsync() {
            return false;
        }

        @Override
        public void markAsAsync() {
            //
        }

        @Override
        public boolean isTimedOut() {
            return false;
        }

        @Override
        public void markAsTimedOut() {

        }

        @Override
        public void resetAsyncStatus() {

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

    private static class TestPluginTaskOutputAsync extends TestPluginTaskOutput {

        @Override
        public boolean isAsync() {
            return true;
        }

        @Nullable
        @Override
        public SettableFuture<Boolean> getAsyncFuture() {
            final SettableFuture<Boolean> booleanSettableFuture = SettableFuture.create();
            booleanSettableFuture.set(true);
            return booleanSettableFuture;
        }

    }

    private static class TestPluginTaskOutputExceptionAsync extends TestPluginTaskOutput {

        @Override
        public boolean isAsync() {
            return true;
        }

        @Nullable
        @Override
        public SettableFuture<Boolean> getAsyncFuture() {
            final SettableFuture<Boolean> booleanSettableFuture = SettableFuture.create();
            booleanSettableFuture.setException(new RuntimeException("Test-Exception"));
            return booleanSettableFuture;
        }

    }

    private static class TestPluginInOutTask implements PluginInOutTask<TestPluginTaskInput, TestPluginTaskOutput> {

        @NotNull
        private final CountDownLatch latch;
        private final int number;
        @NotNull
        private final List<Integer> executionOrder;
        private final int delay;
        private final IsolatedExtensionClassloader classloader;

        TestPluginInOutTask(@NotNull final CountDownLatch latch, final int number,
                            @NotNull final List<Integer> executionOrder, final int delay,
                            @NotNull final IsolatedExtensionClassloader classloader) {

            this.latch = latch;
            this.number = number;
            this.executionOrder = executionOrder;
            this.delay = delay;
            this.classloader = classloader;
        }

        @NotNull
        @Override
        public TestPluginTaskOutput apply(@NotNull final TestPluginTaskInput testPluginTaskInput,
                                          @NotNull final TestPluginTaskOutput testPluginTaskOutput) {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (final InterruptedException ignored) {
                    //ignore
                }
            }
            executionOrder.add(number);
            if (Thread.currentThread().getContextClassLoader() == classloader) {
                latch.countDown();
            } else {
                System.out.println("Class load was not set!");
            }
            return testPluginTaskOutput;
        }

        @Override
        public @NotNull IsolatedExtensionClassloader getPluginClassLoader() {
            return classloader;
        }
    }

    private static class TestPluginOutTask implements PluginOutTask<TestPluginTaskOutput> {

        @NotNull
        private final CountDownLatch latch;
        private final int number;
        @NotNull
        private final List<Integer> executionOrder;
        private final int delay;
        private final IsolatedExtensionClassloader classloader;

        TestPluginOutTask(@NotNull final CountDownLatch latch, final int number,
                          @NotNull final List<Integer> executionOrder, final int delay,
                          @NotNull final IsolatedExtensionClassloader classloader) {

            this.latch = latch;
            this.number = number;
            this.executionOrder = executionOrder;
            this.delay = delay;
            this.classloader = classloader;
        }

        @Override
        public TestPluginTaskOutput apply(final TestPluginTaskOutput testPluginTaskOutput) {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (final InterruptedException ignored) {
                    //ignore
                }
            }
            executionOrder.add(number);
            if (Thread.currentThread().getContextClassLoader() == classloader) {
                latch.countDown();
            } else {
                System.out.println("Class load was not set!");
            }
            return testPluginTaskOutput;
        }

        @Override
        public @NotNull IsolatedExtensionClassloader getPluginClassLoader() {
            return classloader;
        }
    }

    private static class TestPluginInTask implements PluginInTask<TestPluginTaskInput> {

        @NotNull
        private final CountDownLatch latch;
        private final int number;
        @NotNull
        private final List<Integer> executionOrder;
        private final int delay;
        private final IsolatedExtensionClassloader classloader;

        TestPluginInTask(@NotNull final CountDownLatch latch, final int number,
                         @NotNull final List<Integer> executionOrder, final int delay,
                         @NotNull final IsolatedExtensionClassloader classloader) {

            this.latch = latch;
            this.number = number;
            this.executionOrder = executionOrder;
            this.delay = delay;
            this.classloader = classloader;
        }

        @Override
        public void accept(final TestPluginTaskInput testPluginTaskInput) {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (final InterruptedException ignored) {
                    //ignore
                }
            }
            executionOrder.add(number);
            if (Thread.currentThread().getContextClassLoader() == classloader) {
                latch.countDown();
            } else {
                System.out.println("Class load was not set!");
            }
        }

        @Override
        public @NotNull IsolatedExtensionClassloader getPluginClassLoader() {
            return classloader;
        }
    }

    private static class TestPluginInOutexceptionTask extends TestPluginInOutTask {

        TestPluginInOutexceptionTask(@NotNull final CountDownLatch latch, final int number,
                                     @NotNull final List<Integer> executionOrder, final int delay,
                                     @NotNull final IsolatedExtensionClassloader classloader) {
            super(latch, number, executionOrder, delay, classloader);
        }

        @Override
        public @NotNull TestPluginTaskOutput apply(@NotNull final TestPluginTaskInput testPluginTaskInput,
                                                   @NotNull final TestPluginTaskOutput testPluginTaskOutput) {
            super.apply(testPluginTaskInput, testPluginTaskOutput);

            throw new RuntimeException("Test-Exception");

        }
    }


}
