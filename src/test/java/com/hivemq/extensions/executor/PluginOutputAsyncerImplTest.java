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

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class PluginOutputAsyncerImplTest {


    private PluginOutPutAsyncer asyncer;

    @Mock
    private ShutdownHooks shutdownHooks;


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        asyncer = new PluginOutputAsyncerImpl(shutdownHooks);
    }

    @Test
    public void test_output_resume() {

        final TestPluginOutput output = new TestPluginOutput();

        final Async<TestPluginOutput> asyncOutput = asyncer.asyncify(output, Duration.ofSeconds(1));

        assertNotNull(asyncOutput.getOutput());
        assertEquals(Async.Status.RUNNING, asyncOutput.getStatus());

        asyncOutput.resume();


        assertEquals(Async.Status.DONE, asyncOutput.getStatus());
        assertEquals(true, asyncOutput.getOutput().getAsyncFuture().isDone());
    }


    @Test
    public void test_output_timeout() throws Exception {

        final TestPluginOutput output = new TestPluginOutput();

        final Async<TestPluginOutput> asyncOutput = asyncer.asyncify(output, Duration.ofMillis(100));

        assertNotNull(asyncOutput.getOutput());
        assertEquals(Async.Status.RUNNING, asyncOutput.getStatus());

        //wait for timeout
        Thread.sleep(200);

        assertEquals(Async.Status.CANCELED, asyncOutput.getStatus());
        assertEquals(true, asyncOutput.getOutput().getAsyncFuture().isDone());
    }


    @Test
    public void test_shutdown_hook() {

        final ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);

        final PluginOutputAsyncerImpl.PluginOutputAsyncerShutdownHook shutdownHook
                = new PluginOutputAsyncerImpl.PluginOutputAsyncerShutdownHook(scheduledExecutorService);

        shutdownHook.run();

        verify(scheduledExecutorService).shutdown();
    }

    private static class TestPluginOutput implements PluginTaskOutput {

        private final SettableFuture<Boolean> future = SettableFuture.create();

        private final AtomicBoolean async = new AtomicBoolean(false);

        @Override
        public boolean isAsync() {
            return async.get();
        }

        @Override
        public void markAsAsync() {
            async.set(true);
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
            return future;
        }

        @Override
        public @NotNull TimeoutFallback getTimeoutFallback() {
            return TimeoutFallback.FAILURE;
        }
    }

}