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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @since 4.0.0
 */
public class ManagedPluginExecutorShutdownHookTest {

    private final @NotNull GlobalManagedExtensionExecutorService executorService =
            mock(GlobalManagedExtensionExecutorService.class);

    @Test
    public void test_run() {
        final ManagedPluginExecutorShutdownHook pluginExecutorShutdownHook =
                new ManagedPluginExecutorShutdownHook(executorService, 60);

        assertEquals("ManagedExtensionExecutorService shutdown", pluginExecutorShutdownHook.name());
        assertEquals(HiveMQShutdownHook.Priority.DOES_NOT_MATTER, pluginExecutorShutdownHook.priority());

        pluginExecutorShutdownHook.run();

        verify(executorService, times(1)).shutdownNow();
    }

    @Test
    public void test_run_exc() throws Exception {
        final ManagedPluginExecutorShutdownHook pluginExecutorShutdownHook =
                new ManagedPluginExecutorShutdownHook(executorService, 60);

        when(executorService.awaitTermination(
                anyLong(),
                any(TimeUnit.class))).thenThrow(new InterruptedException("test"));
        pluginExecutorShutdownHook.run();

        verify(executorService, times(1)).shutdownNow();
    }
}
