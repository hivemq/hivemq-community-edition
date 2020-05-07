/*
 * Copyright 2020 dc-square GmbH
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

package util;

import com.hivemq.extension.sdk.api.ExtensionMain;
import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * @author Georg Held
 */
public class TestExtensionUtil {

    public static final String validExtensionXML = "<hivemq-extension>" +
            "<id>%s</id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    public static void writeValidExtensionXML(final File pluginXml, final String pluginId) throws IOException {
        FileUtils.writeStringToFile(pluginXml, String.format(validExtensionXML, pluginId), Charset.defaultCharset());
    }

    public static File createValidExtension(
            final File extensionsFolder, final String pluginId,
            final boolean createJar, final boolean enable) throws Exception {
        final File validExtensionsFolder = new File(extensionsFolder, pluginId + (enable ? "" : ".disabled"));
        final File extensionXml = new File(validExtensionsFolder, "hivemq-extension.xml");

        writeValidExtensionXML(extensionXml, pluginId);

        if (createJar) {
            new File(validExtensionsFolder, "extension.jar").createNewFile();
        }
        return validExtensionsFolder;
    }

    public static File createValidExtension(
            final File extensionsFolder, final String pluginId)
            throws Exception {
        return createValidExtension(extensionsFolder, pluginId, true, true);
    }

    public static Path shrinkwrapExtension(
            final File extensionsFolder, final String pluginId, final Class<? extends ExtensionMain> mainClazz,
            final boolean enable) throws Exception {
        final File validPlugin = createValidExtension(extensionsFolder, pluginId, false, enable);
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, mainClazz);
        javaArchive.as(ZipExporter.class).exportTo(new File(validPlugin, "extension.jar"));

        return validPlugin.toPath();
    }
}
