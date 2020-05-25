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

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.config.HiveMQPluginXMLReader;
import com.hivemq.extensions.parameter.ExtensionStartOutputImpl;
import com.hivemq.extensions.parameter.ExtensionStartStopInputImpl;
import com.hivemq.extensions.parameter.ExtensionStopOutputImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import util.TestExtensionUtil;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertSame;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class HiveMQExtensionTest extends PluginAbstractTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    private File validPluginFolder;
    private HiveMQPluginEntity pluginEntityFromXML;
    private HiveMQExtension enabledStartPlugin;
    private Map<String, HiveMQExtension> enabledPlugins;
    private ExtensionStartOutputImpl pluginStartOutput;
    private ExtensionStartStopInputImpl enabledPluginStartInput;
    private ExtensionStopOutputImpl pluginStopOutput;
    private HiveMQExtension enabledStopPlugin;
    private HiveMQExtension enabledReasonPlugin;

    @Mock
    private ServerInformation serverInformation;

    @Before
    public void setUp() throws Exception {

        validPluginFolder = TestExtensionUtil.createValidExtension(tmpFolder.newFolder("extension"), "id");
        pluginEntityFromXML = HiveMQPluginXMLReader.getPluginEntityFromXML(validPluginFolder.toPath(), true).get();

        enabledStartPlugin =
                new HiveMQExtensionImpl(
                        pluginEntityFromXML, validPluginFolder.toPath(), new StartTestExtension(), true);
        enabledStopPlugin =
                new HiveMQExtensionImpl(pluginEntityFromXML, validPluginFolder.toPath(), new StopTestExtension(), true);
        enabledReasonPlugin =
                new HiveMQExtensionImpl(
                        pluginEntityFromXML, validPluginFolder.toPath(), new ReasonTestExtension(), true);


        enabledPlugins = Collections.singletonMap(enabledStartPlugin.getId(), enabledStartPlugin);
        pluginStartOutput = new ExtensionStartOutputImpl();
        pluginStopOutput = new ExtensionStopOutputImpl();
        enabledPluginStartInput =
                new ExtensionStartStopInputImpl(enabledStartPlugin, enabledPlugins, serverInformation);
    }

    @Test(timeout = 5000)
    public void test_instantiate_and_start() throws Throwable {
        enabledStartPlugin.start(enabledPluginStartInput, pluginStartOutput);
        assertTrue(StartTestExtension.start);
    }

    @Test(timeout = 5000)
    public void test_plugin_stop() throws Throwable {
        enabledStopPlugin.start(enabledPluginStartInput, pluginStartOutput);
        enabledStopPlugin.stop(enabledPluginStartInput, pluginStopOutput);

        assertTrue(StopTestExtension.stop);
    }

    @Test(timeout = 5000)
    public void test_start_reason_gets_set() throws Throwable {
        enabledReasonPlugin.start(enabledPluginStartInput, pluginStartOutput);

        assertTrue(pluginStartOutput.getReason().isPresent());
        assertSame(ReasonTestExtension.reason, pluginStartOutput.getReason().get());
    }

    public static class StartTestExtension implements ExtensionMain {

        private static boolean start = false;

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {
            start = true;
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {
        }
    }

    public static class StopTestExtension implements ExtensionMain {

        private static boolean stop = false;

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {
            stop = true;
        }
    }

    public static class ReasonTestExtension implements ExtensionMain {

        private static final String reason = "REASON";

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {
            output.preventExtensionStartup(reason);
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {
        }
    }
}