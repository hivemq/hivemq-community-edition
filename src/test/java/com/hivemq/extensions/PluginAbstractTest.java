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
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @author Georg Held
 */
abstract public class PluginAbstractTest {

    private static final String validPluginXML = "<hivemq-extension>" +
            "<id>%s</id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    private static void writeValidPluginXML(final File pluginXml, final String pluginId) throws IOException {
        FileUtils.writeStringToFile(pluginXml, String.format(validPluginXML, pluginId), Charset.defaultCharset());
    }

    protected File createValidPlugin(final TemporaryFolder temporaryFolder, final String pluginFolder, final String pluginId, final boolean createJar, final boolean enable) throws Exception {
        final File validPluginFolder = temporaryFolder.newFolder(pluginFolder, pluginId + (enable ? "" : ".disabled"));
        writeValidPluginXML(new File(validPluginFolder, "hivemq-extension.xml"), pluginId);

        if (createJar) {
            new File(validPluginFolder, "validPlugin.jar").createNewFile();
        }
        return validPluginFolder;
    }

    protected File createValidPlugin(final TemporaryFolder temporaryFolder, final String pluginFolder, final String pluginId) throws Exception {
        return createValidPlugin(temporaryFolder, pluginFolder, pluginId, true, true);
    }

    protected Path shrinkwrapPlugin(final TemporaryFolder tmpFolder, final String hmqPluginFolder, final String pluginId, final Class<? extends ExtensionMain> mainClazz, final boolean enable) throws Exception {
        final File validPlugin = createValidPlugin(tmpFolder, hmqPluginFolder, pluginId, false, enable);
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, mainClazz);
        javaArchive.as(ZipExporter.class).exportTo(new File(validPlugin, "extension.jar"));

        return validPlugin.toPath();
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
                        return new File("/tmp");
                    }
                };
            }

            @NotNull
            @Override
            @SuppressWarnings("unchecked")
            public Map<String, ExtensionInformation> getEnabledExtensions() {
                return Collections.EMPTY_MAP;
            }

            @Override
            public @NotNull ServerInformation getServerInformation() {
                return Mockito.mock(ServerInformation.class);
            }

            @Override
            public Optional<String> getPreviousVersion() {
                return Optional.of("0");
            }
        };
    }
}
