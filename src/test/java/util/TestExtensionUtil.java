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

package util;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.assertTrue;

public class TestExtensionUtil {

    public static final String validExtensionXML = "<hivemq-extension>" + //
            "<id>%s</id>" + //
            "<name>Some Name</name>" + //
            "<version>1.2.3-Version</version>" + //
            "<priority>1000</priority>" + //
            "<start-priority>500</start-priority>" + //
            "</hivemq-extension>";

    public static @NotNull File createValidExtension(
            final @NotNull File extensionsFolder, final @NotNull String extensionId) throws Exception {
        return createValidExtension(extensionsFolder, extensionId, true, true);
    }

    public static @NotNull File createValidExtension(
            final @NotNull File extensionsFolder,
            final @NotNull String extensionId,
            final boolean createJar,
            final boolean enable) throws Exception {
        final File validExtensionsFolder = new File(extensionsFolder, extensionId + (enable ? "" : ".disabled"));

        final File xmlFile = new File(validExtensionsFolder, "hivemq-extension.xml");
        FileUtils.writeStringToFile(xmlFile,
                String.format(validExtensionXML, extensionId),
                Charset.defaultCharset());

        if (createJar) {
            final File jarFile = new File(validExtensionsFolder, "extension.jar");
            assertTrue(jarFile.createNewFile());
        }
        return validExtensionsFolder;
    }

    public static void shrinkwrapExtension(
            final @NotNull File extensionsFolder,
            final @NotNull String extensionId,
            final @NotNull Class<? extends ExtensionMain> mainClazz,
            final boolean enable) throws Exception {
        final File validExtension = createValidExtension(extensionsFolder, extensionId, false, enable);
        final JavaArchive javaArchive =
                ShrinkWrap.create(JavaArchive.class).addAsServiceProviderAndClasses(ExtensionMain.class, mainClazz);
        javaArchive.as(ZipExporter.class).exportTo(new File(validExtension, "extension.jar"));
    }
}
