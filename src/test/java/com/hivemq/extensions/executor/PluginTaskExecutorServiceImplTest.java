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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.executor.task.*;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Provider;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class PluginTaskExecutorServiceImplTest {

    private PluginTaskExecutorServiceImpl executorService;

    @Mock
    private PluginTaskExecutor executor1;

    @Mock
    private PluginTaskExecutor executor2;

    @Mock
    IsolatedExtensionClassloader classloader;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        InternalConfigurations.EXTENSION_TASK_QUEUE_EXECUTOR_THREADS_COUNT.set(2);

        executorService =
                new PluginTaskExecutorServiceImpl(new ExecutorProvider(Lists.newArrayList(executor1, executor2)),
                        mock(ShutdownHooks.class));
    }

    @Test
    public void test_inout_executed_in_the_right_executor() {

        executorService.handlePluginInOutTaskExecution(
                new TestPluginInOutContext(getIdForBucket(0)),
                TestPluginTaskInput::new,
                TestPluginTaskOutput::new,
                new TestPluginInOutTask(classloader)
        );

        verify(executor1, times(1)).handlePluginTaskExecution(any(PluginTaskExecution.class));

        executorService.handlePluginInOutTaskExecution(
                new TestPluginInOutContext(getIdForBucket(1)),
                TestPluginTaskInput::new,
                TestPluginTaskOutput::new,
                new TestPluginInOutTask(classloader)
        );

        verify(executor2, times(1)).handlePluginTaskExecution(any(PluginTaskExecution.class));

    }

    @Test
    public void test_in_executed_in_the_right_executor() {

        executorService.handlePluginInTaskExecution(
                new TestPluginInContext(getIdForBucket(0)),
                TestPluginTaskInput::new,
                new TestPluginInTask(classloader)
        );

        verify(executor1, times(1)).handlePluginTaskExecution(any(PluginTaskExecution.class));

        executorService.handlePluginInTaskExecution(
                new TestPluginInContext(getIdForBucket(1)),
                TestPluginTaskInput::new,
                new TestPluginInTask(classloader)
        );

        verify(executor2, times(1)).handlePluginTaskExecution(any(PluginTaskExecution.class));

    }

    @Test
    public void test_out_executed_in_the_right_executor() {

        executorService.handlePluginOutTaskExecution(
                new TestPluginOutContext(getIdForBucket(0)),
                TestPluginTaskOutput::new,
                new TestPluginOutTask(classloader)
        );

        verify(executor1, times(1)).handlePluginTaskExecution(any(PluginTaskExecution.class));

        executorService.handlePluginOutTaskExecution(
                new TestPluginOutContext(getIdForBucket(1)),
                TestPluginTaskOutput::new,
                new TestPluginOutTask(classloader)
        );

        verify(executor2, times(1)).handlePluginTaskExecution(any(PluginTaskExecution.class));

    }

    private String getIdForBucket(final int index) {
        for (; ; ) {
            final String s = RandomStringUtils.randomAlphanumeric(10);
            final int bucket = BucketUtils.getBucket(s, 2);
            if (bucket == index) {
                return s;
            }
        }
    }

    private class ExecutorProvider implements Provider<PluginTaskExecutor> {

        private final List<PluginTaskExecutor> executors;
        private Iterator<PluginTaskExecutor> iterator;

        private ExecutorProvider(final List<PluginTaskExecutor> executors) {
            this.executors = executors;
            this.iterator = executors.iterator();
        }

        @Override
        public PluginTaskExecutor get() {
            if (iterator.hasNext()) {
                return iterator.next();
            }

            iterator = executors.iterator();
            return iterator.next();
        }
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

    private static class TestPluginInOutTask implements PluginInOutTask<TestPluginTaskInput, TestPluginTaskOutput> {

        private final IsolatedExtensionClassloader classloader;

        public TestPluginInOutTask(final IsolatedExtensionClassloader classloader) {
            this.classloader = classloader;
        }

        @NotNull
        @Override
        public TestPluginTaskOutput apply(
                @NotNull final TestPluginTaskInput testPluginTaskInput,
                @NotNull final TestPluginTaskOutput testPluginTaskOutput) {

            return testPluginTaskOutput;
        }

        @Override
        public @NotNull IsolatedExtensionClassloader getPluginClassLoader() {
            return classloader;
        }
    }

    private static class TestPluginOutTask implements PluginOutTask<TestPluginTaskOutput> {

        private final IsolatedExtensionClassloader classloader;

        public TestPluginOutTask(final IsolatedExtensionClassloader classloader) {
            this.classloader = classloader;
        }

        @Override
        public TestPluginTaskOutput apply(final TestPluginTaskOutput testPluginTaskOutput) {
            return testPluginTaskOutput;
        }

        @Override
        public @NotNull IsolatedExtensionClassloader getPluginClassLoader() {
            return classloader;
        }
    }

    private static class TestPluginInTask implements PluginInTask<TestPluginTaskInput> {

        private final IsolatedExtensionClassloader classloader;

        public TestPluginInTask(final IsolatedExtensionClassloader classloader) {
            this.classloader = classloader;
        }

        @Override
        public void accept(final TestPluginTaskInput testPluginTaskInput) {

        }

        @Override
        public @NotNull IsolatedExtensionClassloader getPluginClassLoader() {
            return classloader;
        }
    }

}