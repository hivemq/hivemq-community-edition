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

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ManagedPluginExecutorShutdownHookTest {

    @Mock
    GlobalManagedExtensionExecutorService executorService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_run() throws Exception {

        final ManagedPluginExecutorShutdownHook pluginExecutorShutdownHook =
                new ManagedPluginExecutorShutdownHook(executorService, 60);

        assertEquals("ManagedExtensionExecutorService shutdown", pluginExecutorShutdownHook.name());
        assertEquals(HiveMQShutdownHook.Priority.DOES_NOT_MATTER, pluginExecutorShutdownHook.priority());
        assertEquals(false, pluginExecutorShutdownHook.isAsynchronous());

        pluginExecutorShutdownHook.run();

        verify(executorService, times(1)).shutdownNow();

    }

    @Test
    public void test_run_exc() throws Exception {
        final ManagedPluginExecutorShutdownHook pluginExecutorShutdownHook =
                new ManagedPluginExecutorShutdownHook(executorService, 60);

        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenThrow(
                new InterruptedException("test"));
        pluginExecutorShutdownHook.run();

        verify(executorService, times(1)).shutdownNow();
    }

}