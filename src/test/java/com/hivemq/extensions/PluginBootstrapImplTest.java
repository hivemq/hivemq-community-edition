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

package com.hivemq.extensions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.loader.PluginLifecycleHandler;
import com.hivemq.extensions.loader.PluginLifecycleHandlerImpl;
import com.hivemq.extensions.loader.PluginLoader;
import com.hivemq.extensions.services.auth.Authenticators;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;

import java.nio.file.Path;
import java.util.HashMap;

import static org.mockito.Mockito.*;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class PluginBootstrapImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public InitFutureUtilsExecutorRule executorRule = new InitFutureUtilsExecutorRule();

    @Mock
    private PluginLoader pluginLoader;

    @Mock
    HiveMQExtensions hiveMQExtensions;

    @Mock
    ShutdownHooks shutdownHooks;

    @Mock
    Authenticators authenticators;

    private PluginBootstrapImpl pluginBootstrap;
    private PluginLifecycleHandler pluginLifecycleHandler;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);


        pluginLifecycleHandler = new PluginLifecycleHandlerImpl(hiveMQExtensions, MoreExecutors.newDirectExecutorService());
        pluginBootstrap = new PluginBootstrapImpl(pluginLoader, new SystemInformationImpl(), pluginLifecycleHandler,
                hiveMQExtensions, shutdownHooks,authenticators);
    }

    @Test
    public void test_startPluginSystem_shutdown_hook_registered() {

        when(pluginLoader.loadPlugins(any(Path.class), anyBoolean(), any(Class.class))).thenReturn(ImmutableList.of());
        pluginBootstrap.startPluginSystem();

        verify(shutdownHooks).add(any(HiveMQShutdownHook.class));
    }

    @Test
    public void test_stopPluginSystem_all_enabled_plugins_stopped() {

        final HashMap<String, HiveMQExtension> extensions = Maps.newHashMap();

        extensions.put("extension-1", new TestHiveMQExtension("extension-1", temporaryFolder));
        extensions.put("extension-2", new TestHiveMQExtension("extension-2", temporaryFolder));

        when(hiveMQExtensions.getEnabledHiveMQExtensions()).thenReturn(extensions);
        pluginBootstrap.stopPluginSystem();

        verify(hiveMQExtensions, times(1)).extensionStop(eq("extension-1"), eq(false));
        verify(hiveMQExtensions, times(1)).extensionStop(eq("extension-2"), eq(false));
    }

    private static class TestHiveMQExtension implements HiveMQExtension {

        private final String pluginId;
        private final TemporaryFolder temporaryFolder;

        TestHiveMQExtension(final String pluginId, final TemporaryFolder temporaryFolder) {

            this.pluginId = pluginId;
            this.temporaryFolder = temporaryFolder;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setDisabled() {
            //ignore
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public int getStartPriority() {
            return 1000;
        }

        @NotNull
        @Override
        public String getName() {
            return pluginId + "-name";
        }

        @Override
        public String getAuthor() {
            return pluginId + "-author";
        }

        @NotNull
        @Override
        public String getVersion() {
            return pluginId + "-version";
        }

        @NotNull
        @Override
        public String getId() {
            return pluginId;
        }

        @NotNull
        @Override
        public Class<? extends ExtensionMain> getPluginMainClazz() {
            return TestExtensionMain.class;
        }

        @Nullable
        @Override
        public IsolatedPluginClassloader getPluginClassloader() {
            return null;
        }

        @NotNull
        @Override
        public Path getPluginFolderPath() {
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
        public void start(final @NotNull ExtensionStartInput extensionStartInput, final @NotNull ExtensionStartOutput extensionStartOutput) {

        }

        @Override
        public void stop(final @NotNull ExtensionStopInput extensionStopInput, final @NotNull ExtensionStopOutput extensionStopOutput) {

        }

        @Override
        public void clean(final boolean disable) {

        }
    }

    private static class TestExtensionMain implements ExtensionMain {

        @Override
        public void extensionStart(final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {

        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {

        }
    }
}