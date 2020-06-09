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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @author Georg Held
 */
abstract public class AbstractPluginTest {
    protected File createValidPlugin(final TemporaryFolder temporaryFolder, final String pluginFolder, final String pluginName) throws Exception {
        final File validPluginFolder = temporaryFolder.newFolder(pluginFolder, pluginName);
        new File(validPluginFolder, "hivemq-extension.xml").createNewFile();
        new File(validPluginFolder, "validPlugin.jar").createNewFile();
        return validPluginFolder;
    }

    protected ExtensionStartOutput getTestPluginStartOutput() {
        return reason -> {
        };
    }

    protected ExtensionStartInput getTestPluginStartInput() {
        return new ExtensionStartInput() {
            @NotNull
            @Override
            public ExtensionInformation getExtensionInformation() {
                return new ExtensionInformation() {
                    @NotNull
                    @Override
                    public String getId() {
                        return "id";
                    }

                    @NotNull
                    @Override
                    public String getName() {
                        return "name";
                    }

                    @NotNull

                    @Override
                    public String getVersion() {
                        return "1";
                    }

                    @Override
                    public Optional<String> getAuthor() {
                        return Optional.of("me");
                    }

                    @Override
                    public File getExtensionHomeFolder() {
                        return null;
                    }
                };
            }

            @Override
            public @NotNull ServerInformation getServerInformation() {
                return Mockito.mock(ServerInformation.class);
            }

            @NotNull
            @Override
            @SuppressWarnings("unchecked")
            public Map<String, ExtensionInformation> getEnabledExtensions() {
                return Collections.EMPTY_MAP;
            }

            @Override
            public Optional<String> getPreviousVersion() {
                return Optional.of("0");
            }
        };
    }
}
