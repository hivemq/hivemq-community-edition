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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.loader.ExtensionLifecycleHandler;
import com.hivemq.extensions.loader.ExtensionLifecycleHandlerImpl;
import com.hivemq.extensions.loader.ExtensionLoader;
import com.hivemq.extensions.services.auth.Authenticators;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;

import static org.mockito.Mockito.*;

/**
 * @author Christoph Schäbel
 */
public class ExtensionBootstrapImplTest {

    @Rule
    public @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull ExtensionLoader extensionLoader = mock(ExtensionLoader.class);
    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull ShutdownHooks shutdownHooks = mock(ShutdownHooks.class);
    private final @NotNull Authenticators authenticators = mock(Authenticators.class);
    private final @NotNull EmbeddedExtension embeddedExtension = mock(EmbeddedExtension.class);

    private @NotNull ExtensionBootstrapImpl pluginBootstrap;

    @Before
    public void before() {
        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        systemInformation.init();

        final ExtensionLifecycleHandler extensionLifecycleHandler =
                new ExtensionLifecycleHandlerImpl(hiveMQExtensions, MoreExecutors.newDirectExecutorService());
        pluginBootstrap = new ExtensionBootstrapImpl(extensionLoader,
                systemInformation,
                extensionLifecycleHandler,
                hiveMQExtensions,
                shutdownHooks,
                authenticators);
    }

    @Test
    public void test_startPluginSystem_shutdown_hook_registered() {
        when(extensionLoader.loadExtensions(any(Path.class), anyBoolean())).thenReturn(ImmutableList.of());
        pluginBootstrap.startExtensionSystem(null);

        verify(shutdownHooks).add(any(HiveMQShutdownHook.class));
    }

    @Test
    public void test_startPluginSystem_with_embeddedExtensions() {
        when(extensionLoader.loadExtensions(any(Path.class), anyBoolean())).thenReturn(ImmutableList.of());
        when(extensionLoader.loadEmbeddedExtension(any(EmbeddedExtension.class))).thenReturn(new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                "my-extension",
                0,
                new File("/tmp").toPath(),
                true));
        pluginBootstrap.startExtensionSystem(embeddedExtension);

        verify(hiveMQExtensions).extensionStart("my-extension");
        verify(shutdownHooks).add(any(HiveMQShutdownHook.class));
    }

    @Test
    public void test_startPluginSystem_mixed() {
        when(extensionLoader.loadExtensions(
                any(Path.class),
                anyBoolean())).thenReturn(ImmutableList.of(new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                "my-extension-1",
                0,
                new File("/folder").toPath(),
                false)));
        when(extensionLoader.loadEmbeddedExtension(any(EmbeddedExtension.class))).thenReturn(new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                "my-extension-2",
                0,
                new File("/tmp").toPath(),
                true));
        pluginBootstrap.startExtensionSystem(embeddedExtension);

        verify(hiveMQExtensions).extensionStart("my-extension-1");
        verify(hiveMQExtensions).extensionStart("my-extension-2");
    }

    @Test
    public void test_stopPluginSystem_all_enabled_plugins_stopped() {
        final HashMap<String, HiveMQExtension> extensions = Maps.newHashMap();

        extensions.put("extension-1", new TestHiveMQExtension("extension-1", temporaryFolder));
        extensions.put("extension-2", new TestHiveMQExtension("extension-2", temporaryFolder));

        when(hiveMQExtensions.getEnabledHiveMQExtensions()).thenReturn(extensions);
        pluginBootstrap.stopExtensionSystem();

        verify(hiveMQExtensions, times(1)).extensionStop(eq("extension-1"), eq(false));
        verify(hiveMQExtensions, times(1)).extensionStop(eq("extension-2"), eq(false));
    }

    private static class TestHiveMQExtension implements HiveMQExtension {

        private final @NotNull String pluginId;
        private final @NotNull TemporaryFolder temporaryFolder;

        TestHiveMQExtension(final @NotNull String pluginId, final @NotNull TemporaryFolder temporaryFolder) {
            this.pluginId = pluginId;
            this.temporaryFolder = temporaryFolder;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setDisabled() {
            // ignore
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public int getStartPriority() {
            return 1000;
        }

        @Override
        public @NotNull String getName() {
            return pluginId + "-name";
        }

        @Override
        public String getAuthor() {
            return pluginId + "-author";
        }

        @Override
        public @NotNull String getVersion() {
            return pluginId + "-version";
        }

        @Override
        public @NotNull String getId() {
            return pluginId;
        }

        @Override
        public @NotNull Class<? extends ExtensionMain> getExtensionMainClazz() {
            return TestExtensionMain.class;
        }

        @Override
        public @Nullable IsolatedExtensionClassloader getExtensionClassloader() {
            return null;
        }

        @Override
        public @NotNull Path getExtensionFolderPath() {
            return temporaryFolder.getRoot().toPath();
        }

        @Override
        public String getPreviousVersion() {
            return null;
        }

        @Override
        public void setPreviousVersion(final String previousVersion) {
        }

        @Override
        public void start(
                final @NotNull ExtensionStartInput extensionStartInput,
                final @NotNull ExtensionStartOutput extensionStartOutput) {
        }

        @Override
        public void stop(
                final @NotNull ExtensionStopInput extensionStopInput,
                final @NotNull ExtensionStopOutput extensionStopOutput) {
        }

        @Override
        public void clean(final boolean disable) {
        }

        @Override
        public boolean isEmbedded() {
            return false;
        }
    }

    private static class TestExtensionMain implements ExtensionMain {

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {
        }
    }
}
