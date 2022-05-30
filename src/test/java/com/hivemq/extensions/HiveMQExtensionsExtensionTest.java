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

package com.hivemq.extensions;

import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Georg Held
 */
public class HiveMQExtensionsExtensionTest extends AbstractExtensionTest {

    @Rule
    public final @NotNull TemporaryFolder tmpFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtension extension1 = mock(HiveMQExtension.class);
    private final @NotNull HiveMQExtension extension2 = mock(HiveMQExtension.class);
    private final @NotNull IsolatedExtensionClassloader loader1 = mock(IsolatedExtensionClassloader.class);
    private final @NotNull IsolatedExtensionClassloader loader2 = mock(IsolatedExtensionClassloader.class);
    private final @NotNull ListenerConfigurationService listenerConfigurationService =
            mock(ListenerConfigurationService.class);

    private @NotNull String id1;
    private @NotNull String id2;

    private @NotNull HiveMQExtensions hiveMQExtensions;

    @Before
    public void setUp() throws Exception {
        id1 = "extension1";
        id2 = "extension2";

        when(extension1.getId()).thenReturn(id1);
        when(extension2.getId()).thenReturn(id2);

        Mockito.<Class<? extends ExtensionMain>>when(extension1.getExtensionMainClazz())
                .thenReturn(ExtensionMain.class);
        Mockito.<Class<? extends ExtensionMain>>when(extension2.getExtensionMainClazz())
                .thenReturn(ExtensionMain.class);
        when(extension1.getExtensionClassloader()).thenReturn(loader1);
        when(extension2.getExtensionClassloader()).thenReturn(loader2);

        hiveMQExtensions = new HiveMQExtensions(new ServerInformationImpl(new SystemInformationImpl(),
                listenerConfigurationService));
        hiveMQExtensions.addHiveMQExtension(extension1);
    }

    @Test(timeout = 5000)
    public void test_disabled_extension_is_not_started() {
        assertFalse(hiveMQExtensions.extensionStart(id1));
    }

    @Test(timeout = 5000)
    public void test_enabled_extension_is_started() throws Throwable {
        when(extension1.isEnabled()).thenReturn(true);
        assertTrue(hiveMQExtensions.extensionStart(id1));

        verify(extension1, times(1)).start(any(ExtensionStartInput.class), any(ExtensionStartOutput.class));
        verify(extension1, times(1)).getExtensionClassloader();

        assertEquals(extension1, hiveMQExtensions.getExtensionForClassloader(loader1));
    }

    @Test(timeout = 5000)
    public void test_disabled_extension_is_not_disabled() {
        assertFalse(hiveMQExtensions.extensionStop(id1, false));
    }

    @Test(timeout = 5000)
    public void test_enabled_extension_is_not_disabled() throws Throwable {
        when(extension1.isEnabled()).thenReturn(true);
        assertTrue(hiveMQExtensions.extensionStart(id1));
        assertNotNull(hiveMQExtensions.getExtensionForClassloader(loader1));

        hiveMQExtensions.extensionStop(id1, false);
        verify(extension1, times(1)).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));
        verify(extension1, times(1)).clean(false);
        assertNull(hiveMQExtensions.getExtensionForClassloader(loader1));
    }

    @Test(timeout = 5000)
    public void test_extension_stop_throws_exception() throws Throwable {
        when(extension1.isEnabled()).thenReturn(true);
        hiveMQExtensions.extensionStart(id1);
        assertNotNull(hiveMQExtensions.getExtensionForClassloader(loader1));

        doThrow(new RuntimeException()).when(extension1)
                .stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, true);

        verify(extension1, times(1)).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));
        assertNull(hiveMQExtensions.getExtensionForClassloader(loader1));
        assertTrue(hiveMQExtensions.getClassloaderToExtensionMap().isEmpty());
    }

    @Test(timeout = 5000)
    public void test_enabled_extension_is_returned() {
        hiveMQExtensions.addHiveMQExtension(extension2);
        when(extension1.isEnabled()).thenReturn(true);

        final Map<String, HiveMQExtension> enabledHiveMQExtensions = hiveMQExtensions.getEnabledHiveMQExtensions();
        assertEquals(1, enabledHiveMQExtensions.size());
        assertTrue(enabledHiveMQExtensions.containsKey(id1));
        assertFalse(enabledHiveMQExtensions.containsKey(id2));
    }

    @Test(timeout = 5000)
    public void test_enabled_extensions_is_empty() {
        final Map<String, HiveMQExtension> enabledHiveMQExtensions = hiveMQExtensions.getEnabledHiveMQExtensions();
        assertEquals(0, enabledHiveMQExtensions.size());
    }

    @Test(timeout = 5000)
    public void test_previous_version_is_set() {
        final String version = "some-old-version";
        when(extension1.getVersion()).thenReturn(version);
        when(extension2.getId()).thenReturn(id1);

        hiveMQExtensions.addHiveMQExtension(extension2);

        verify(extension2, times(1)).setPreviousVersion(same(version));
    }

    @Test(timeout = 5000)
    public void test_previous_version_is_not_set() {
        final String version = "some-old-version";
        when(extension1.getVersion()).thenReturn(version);
        when(extension2.getId()).thenReturn(id2);

        hiveMQExtensions.addHiveMQExtension(extension2);

        verify(extension2, never()).setPreviousVersion(anyString());
    }

    @Test(timeout = 5000)
    public void test_before_stop_callback() throws Throwable {
        when(extension1.isEnabled()).thenReturn(true);

        final ExtensionStopCallback extensionStopCallback = new ExtensionStopCallback();
        hiveMQExtensions.addBeforeExtensionStopCallback(extensionStopCallback);

        hiveMQExtensions.extensionStart(id1);

        final AtomicBoolean before = new AtomicBoolean(false);
        doAnswer(invocation -> {
            before.set(extension1 == extensionStopCallback.hiveMQExtension);
            return null;
        }).when(extension1).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, false);

        assertTrue(before.get());
        assertEquals(1, extensionStopCallback.count);
    }

    @Test(timeout = 5000)
    public void test_before_stop_callback_exception() throws Throwable {
        when(extension1.isEnabled()).thenReturn(true);

        final ExtensionStopCallback extensionStopCallback = new ExtensionStopCallback();
        hiveMQExtensions.addBeforeExtensionStopCallback(extensionStopCallback);

        hiveMQExtensions.extensionStart(id1);

        final AtomicBoolean before = new AtomicBoolean(false);
        doAnswer(invocation -> {
            before.set(extension1 == extensionStopCallback.hiveMQExtension);
            throw new IllegalStateException("test");
        }).when(extension1).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, false);

        assertTrue(before.get());
        assertEquals(1, extensionStopCallback.count);
    }

    @Test(timeout = 5000)
    public void test_after_stop_callback() throws Throwable {
        when(extension1.isEnabled()).thenReturn(true);

        final ExtensionStopCallback extensionStopCallback = new ExtensionStopCallback();
        hiveMQExtensions.addAfterExtensionStopCallback(extensionStopCallback);

        hiveMQExtensions.extensionStart(id1);

        final AtomicBoolean notBefore = new AtomicBoolean(false);
        doAnswer(invocation -> {
            notBefore.set(extension1 != extensionStopCallback.hiveMQExtension);
            return null;
        }).when(extension1).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, false);

        assertTrue(notBefore.get());
        assertSame(extension1, extensionStopCallback.hiveMQExtension);
        assertEquals(1, extensionStopCallback.count);
    }

    @Test(timeout = 5000)
    public void test_after_stop_callback_exception() throws Throwable {
        when(extension1.isEnabled()).thenReturn(true);

        final ExtensionStopCallback extensionStopCallback = new ExtensionStopCallback();
        hiveMQExtensions.addAfterExtensionStopCallback(extensionStopCallback);

        hiveMQExtensions.extensionStart(id1);

        final AtomicBoolean notBefore = new AtomicBoolean(false);
        doAnswer(invocation -> {
            notBefore.set(extension1 != extensionStopCallback.hiveMQExtension);
            throw new IllegalStateException("test");
        }).when(extension1).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, false);

        assertTrue(notBefore.get());
        assertSame(extension1, extensionStopCallback.hiveMQExtension);
        assertEquals(1, extensionStopCallback.count);
    }

    private static class ExtensionStopCallback implements Consumer<HiveMQExtension> {

        @Nullable HiveMQExtension hiveMQExtension;
        int count;

        @Override
        public void accept(final @NotNull HiveMQExtension hiveMQExtension) {
            this.hiveMQExtension = hiveMQExtension;
            count++;
        }
    }
}
