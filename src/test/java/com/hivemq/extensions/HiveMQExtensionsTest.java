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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class HiveMQExtensionsTest extends PluginAbstractTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtension plugin1;

    @Mock
    private HiveMQExtension plugin2;

    @Mock
    private IsolatedExtensionClassloader loader1;

    @Mock
    private IsolatedExtensionClassloader loader2;

    @Mock
    private ListenerConfigurationService listenerConfigurationService;

    private String id1;
    private String id2;
    private HiveMQExtensions hiveMQExtensions;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        id1 = "plugin1";
        id2 = "plugin2";

        when(plugin1.getId()).thenReturn(id1);
        when(plugin2.getId()).thenReturn(id2);

        Mockito.<Class<? extends ExtensionMain>>when(plugin1.getExtensionMainClazz()).thenReturn(ExtensionMain.class);
        Mockito.<Class<? extends ExtensionMain>>when(plugin2.getExtensionMainClazz()).thenReturn(ExtensionMain.class);
        when(plugin1.getExtensionClassloader()).thenReturn(loader1);
        when(plugin2.getExtensionClassloader()).thenReturn(loader2);


        hiveMQExtensions = new HiveMQExtensions(new ServerInformationImpl(new SystemInformationImpl(), listenerConfigurationService));

        hiveMQExtensions.addHiveMQExtension(plugin1);
    }

    @Test(timeout = 5000)
    public void test_disabled_plugin_is_not_started() {
        assertFalse(hiveMQExtensions.extensionStart(id1));
    }

    @Test(timeout = 5000)
    public void test_enabled_plugin_is_started() throws Throwable {
        when(plugin1.isEnabled()).thenReturn(true);
        assertTrue(hiveMQExtensions.extensionStart(id1));

        verify(plugin1, times(1)).start(any(ExtensionStartInput.class), any(ExtensionStartOutput.class));
        verify(plugin1, times(1)).getExtensionClassloader();

        assertEquals(plugin1, hiveMQExtensions.getExtensionForClassloader(loader1));
    }

    @Test(timeout = 5000)
    public void test_disabled_plugin_is_not_disabled() {
        assertFalse(hiveMQExtensions.extensionStop(id1, false));
    }

    @Test(timeout = 5000)
    public void test_enabled_plugin_is_not_disabled() throws Throwable {
        when(plugin1.isEnabled()).thenReturn(true);
        assertTrue(hiveMQExtensions.extensionStart(id1));
        assertNotNull(hiveMQExtensions.getExtensionForClassloader(loader1));

        hiveMQExtensions.extensionStop(id1, false);
        verify(plugin1, times(1)).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));
        verify(plugin1, times(1)).clean(false);
        assertNull(hiveMQExtensions.getExtensionForClassloader(loader1));
    }

    @Test(timeout = 5000)
    public void test_plugin_stop_throws_exception() throws Throwable {

        when(plugin1.isEnabled()).thenReturn(true);
        hiveMQExtensions.extensionStart(id1);
        assertNotNull(hiveMQExtensions.getExtensionForClassloader(loader1));

        doThrow(new RuntimeException()).when(plugin1).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, true);

        verify(plugin1, times(1)).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));
        assertNull(hiveMQExtensions.getExtensionForClassloader(loader1));
        assertTrue(hiveMQExtensions.getClassloaderToExtensionMap().isEmpty());
    }

    @Test(timeout = 5000)
    public void test_enabled_plugin_is_returned() {
        hiveMQExtensions.addHiveMQExtension(plugin2);
        when(plugin1.isEnabled()).thenReturn(true);


        final Map<String, HiveMQExtension> enabledHiveMQPlugins = hiveMQExtensions.getEnabledHiveMQExtensions();
        assertEquals(1, enabledHiveMQPlugins.size());
        assertTrue(enabledHiveMQPlugins.containsKey(id1));
        assertFalse(enabledHiveMQPlugins.containsKey(id2));
    }

    @Test(timeout = 5000)
    public void test_enabled_plugins_is_empty() {
        final Map<String, HiveMQExtension> enabledHiveMQPlugins = hiveMQExtensions.getEnabledHiveMQExtensions();
        assertEquals(0, enabledHiveMQPlugins.size());
    }

    @Test(timeout = 5000)
    public void test_previous_version_is_set() {
        final String version = "some-old-version";
        when(plugin1.getVersion()).thenReturn(version);
        when(plugin2.getId()).thenReturn(id1);

        hiveMQExtensions.addHiveMQExtension(plugin2);

        verify(plugin2, times(1)).setPreviousVersion(same(version));
    }

    @Test(timeout = 5000)
    public void test_previous_version_is_not_set() {
        final String version = "some-old-version";
        when(plugin1.getVersion()).thenReturn(version);
        when(plugin2.getId()).thenReturn(id2);

        hiveMQExtensions.addHiveMQExtension(plugin2);

        verify(plugin2, never()).setPreviousVersion(anyString());
    }

    @Test(timeout = 5000)
    public void test_before_stop_callback() throws Throwable {
        when(plugin1.isEnabled()).thenReturn(true);

        final PluginStopCallback pluginStopCallback = new PluginStopCallback();
        hiveMQExtensions.addBeforeExtensionStopCallback(pluginStopCallback);

        hiveMQExtensions.extensionStart(id1);

        final AtomicBoolean before = new AtomicBoolean(false);

        doAnswer(invocation -> {
            before.set(plugin1 == pluginStopCallback.plugin);
            return null;
        }).when(plugin1).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, false);

        assertTrue(before.get());
        assertEquals(1, pluginStopCallback.count);
    }

    @Test(timeout = 5000)
    public void test_before_stop_callback_exception() throws Throwable {
        when(plugin1.isEnabled()).thenReturn(true);

        final PluginStopCallback pluginStopCallback = new PluginStopCallback();
        hiveMQExtensions.addBeforeExtensionStopCallback(pluginStopCallback);

        hiveMQExtensions.extensionStart(id1);

        final AtomicBoolean before = new AtomicBoolean(false);

        doAnswer(invocation -> {
            before.set(plugin1 == pluginStopCallback.plugin);
            throw new IllegalStateException("test");
        }).when(plugin1).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, false);

        assertTrue(before.get());
        assertEquals(1, pluginStopCallback.count);
    }

    @Test(timeout = 5000)
    public void test_after_stop_callback() throws Throwable {
        when(plugin1.isEnabled()).thenReturn(true);

        final PluginStopCallback pluginStopCallback = new PluginStopCallback();
        hiveMQExtensions.addAfterExtensionStopCallback(pluginStopCallback);

        hiveMQExtensions.extensionStart(id1);

        final AtomicBoolean notBefore = new AtomicBoolean(false);

        doAnswer(invocation -> {
            notBefore.set(plugin1 != pluginStopCallback.plugin);
            return null;
        }).when(plugin1).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, false);

        assertTrue(notBefore.get());
        assertSame(plugin1, pluginStopCallback.plugin);
        assertEquals(1, pluginStopCallback.count);
    }

    @Test(timeout = 5000)
    public void test_after_stop_callback_exception() throws Throwable {
        when(plugin1.isEnabled()).thenReturn(true);

        final PluginStopCallback pluginStopCallback = new PluginStopCallback();
        hiveMQExtensions.addAfterExtensionStopCallback(pluginStopCallback);

        hiveMQExtensions.extensionStart(id1);

        final AtomicBoolean notBefore = new AtomicBoolean(false);

        doAnswer(invocation -> {
            notBefore.set(plugin1 != pluginStopCallback.plugin);
            throw new IllegalStateException("test");
        }).when(plugin1).stop(any(ExtensionStopInput.class), any(ExtensionStopOutput.class));

        hiveMQExtensions.extensionStop(id1, false);

        assertTrue(notBefore.get());
        assertSame(plugin1, pluginStopCallback.plugin);
        assertEquals(1, pluginStopCallback.count);
    }

    private static class PluginStopCallback implements Consumer<HiveMQExtension> {

        HiveMQExtension plugin;
        int count;

        @Override
        public void accept(final @NotNull HiveMQExtension hiveMQExtension) {
            this.plugin = hiveMQExtension;
            count++;
        }
    }

}