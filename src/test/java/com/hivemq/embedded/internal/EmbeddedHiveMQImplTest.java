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
package com.hivemq.embedded.internal;

import com.google.inject.Injector;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.RandomPortGenerator;
import util.TestExtensionUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EmbeddedHiveMQImplTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final String extensionName = "extension-1";
    private File data;
    private File extensions;
    private File conf;
    private int randomPort;

    @Before
    public void setUp() throws Exception {
        data = tmp.newFolder("data");
        extensions = tmp.newFolder("extensions");
        conf = tmp.newFolder("conf");
        randomPort = RandomPortGenerator.get();

        final String noListenerConfig = "" +
                "<hivemq>\n" +
                "    <listeners>\n" +
                "        <tcp-listener>\n" +
                "            <port>" + randomPort + "</port>\n" +
                "            <bind-address>0.0.0.0</bind-address>\n" +
                "        </tcp-listener>\n" +
                "    </listeners>\n" +
                "</hivemq>";
        FileUtils.write(new File(conf, "config.xml"), noListenerConfig, StandardCharsets.UTF_8);

        TestExtensionUtil.shrinkwrapExtension(extensions, extensionName, Main.class, true);
    }

    @Test
    public void embeddedHiveMQ_readsConfig() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        embeddedHiveMQ.start().join();

        final Injector injector = embeddedHiveMQ.getInjector();
        final ListenerConfigurationService listenerConfigurationService =
                injector.getInstance(ListenerConfigurationService.class);
        final List<Listener> listeners = listenerConfigurationService.getListeners();

        assertEquals(1, listeners.size());
        assertEquals(randomPort, listeners.get(0).getPort());

        embeddedHiveMQ.stop().join();
    }

    @Test
    public void embeddedHiveMQ_usesDataFolder() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        embeddedHiveMQ.start().join();
        embeddedHiveMQ.stop().join();

        final File[] files = data.listFiles((d, n) -> "persistence".equals(n));
        assertEquals(1, files.length);
    }

    @Test
    public void embeddedHiveMQ_usesExtensionsFolder() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        embeddedHiveMQ.start().join();

        final Injector injector = embeddedHiveMQ.getInjector();
        final HiveMQExtensions hiveMQExtensions = injector.getInstance(HiveMQExtensions.class);

        final HiveMQExtension extension = hiveMQExtensions.getExtension(extensionName);
        assertNotNull(extension);

        embeddedHiveMQ.stop().join();
    }

    public static class Main implements ExtensionMain {

        @Override
        public void extensionStart(
                @NotNull ExtensionStartInput extensionStartInput, @NotNull ExtensionStartOutput extensionStartOutput) {

        }

        @Override
        public void extensionStop(
                @NotNull ExtensionStopInput extensionStopInput, @NotNull ExtensionStopOutput extensionStopOutput) {

        }
    }
}