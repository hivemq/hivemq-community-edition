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

package com.hivemq.extensions.loader;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.HiveMQExtensionEvent;
import com.hivemq.extensions.HiveMQExtensions;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ExtensionLifecycleHandlerImplTest {

    @Rule
    public @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final @NotNull ExtensionLifecycleHandlerImpl extensionLifecycleHandler =
            new ExtensionLifecycleHandlerImpl(hiveMQExtensions, executorService);

    @After
    public void tearDown() throws Exception {
        executorService.shutdown();
    }

    @Test(timeout = 5000)
    public void test_handleExtensionEvents_enable() throws Exception {
        final ImmutableList<HiveMQExtensionEvent> events = ImmutableList.of(new HiveMQExtensionEvent(
                HiveMQExtensionEvent.Change.ENABLE,
                "test-extension",
                1,
                temporaryFolder.getRoot().toPath(),
                false));

        extensionLifecycleHandler.handleExtensionEvents(events).get();
        verify(hiveMQExtensions).extensionStart(eq("test-extension"));
    }

    @Test(timeout = 5000)
    public void handleExtensionEvents_inStartPriority() throws Exception {
        final ImmutableList<HiveMQExtensionEvent> events = ImmutableList.of(new HiveMQExtensionEvent(
                HiveMQExtensionEvent.Change.ENABLE,
                "test-extension-100",
                100,
                temporaryFolder.getRoot().toPath(),
                false), new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                "test-extension-1",
                1,
                temporaryFolder.getRoot().toPath(),
                false), new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                "test-extension-10",
                10,
                temporaryFolder.getRoot().toPath(),
                false), new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                "test-extension-1000",
                1000,
                temporaryFolder.getRoot().toPath(),
                false));

        extensionLifecycleHandler.handleExtensionEvents(events).get();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(hiveMQExtensions, times(4)).extensionStart(captor.capture());

        final List<String> allValues = captor.getAllValues();
        assertEquals("test-extension-1000", allValues.get(0));
        assertEquals("test-extension-100", allValues.get(1));
        assertEquals("test-extension-10", allValues.get(2));
        assertEquals("test-extension-1", allValues.get(3));
    }

    @Test(timeout = 5000)
    public void handleExtensionEvents_inStartPriority_exception() throws Exception {
        when(hiveMQExtensions.extensionStart(eq("test-extension-100"))).thenThrow(new RuntimeException());

        final ImmutableList<HiveMQExtensionEvent> events = ImmutableList.of(new HiveMQExtensionEvent(
                HiveMQExtensionEvent.Change.ENABLE,
                "test-extension-100",
                100,
                temporaryFolder.getRoot().toPath(),
                false), new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                "test-extension-1",
                1,
                temporaryFolder.getRoot().toPath(),
                false), new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                "test-extension-10",
                10,
                temporaryFolder.getRoot().toPath(),
                false), new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                "test-extension-1000",
                1000,
                temporaryFolder.getRoot().toPath(),
                false));

        extensionLifecycleHandler.handleExtensionEvents(events).get();

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(hiveMQExtensions, times(4)).extensionStart(captor.capture());

        final List<String> allValues = captor.getAllValues();
        assertEquals("test-extension-1000", allValues.get(0));
        assertEquals("test-extension-100", allValues.get(1));
        assertEquals("test-extension-10", allValues.get(2));
        assertEquals("test-extension-1", allValues.get(3));
    }

    @Test(timeout = 5000)
    public void test_handleExtensionEvents_disable() throws Exception {
        final ImmutableList<HiveMQExtensionEvent> events = ImmutableList.of(new HiveMQExtensionEvent(
                HiveMQExtensionEvent.Change.DISABLE,
                "test-extension",
                1,
                temporaryFolder.getRoot().toPath(),
                false));

        extensionLifecycleHandler.handleExtensionEvents(events).get();

        verify(hiveMQExtensions).extensionStop(eq("test-extension"), eq(false));
    }
}
