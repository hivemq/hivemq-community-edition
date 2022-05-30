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

package com.hivemq.extensions.config;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.HiveMQExtensionEntity;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Optional;

import static org.junit.Assert.*;

public class HiveMQExtensionXMLReaderTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test_unmarschal_plugin_meta() throws Exception {
        final File extensionXML = temporaryFolder.newFile("hivemq-extension.xml");
        FileUtils.writeStringToFile(extensionXML, "<hivemq-extension>" + //
                "<id>some-id</id>" + //
                "<name>Some Name</name>" + //
                "<version>1.0.0</version>" + //
                "<priority>1000</priority>" + //
                "<author>Some Author</author>" + //
                "</hivemq-extension>", Charset.defaultCharset());
        final Optional<HiveMQExtensionEntity> optionalPluginEntityFromXML =
                HiveMQExtensionXMLReader.getExtensionEntityFromXML(extensionXML.toPath().getParent(), true);
        assertTrue(optionalPluginEntityFromXML.isPresent());
        final HiveMQExtensionEntity hiveMQExtensionEntity = optionalPluginEntityFromXML.get();
        assertEquals("some-id", hiveMQExtensionEntity.getId());
        assertEquals("Some Name", hiveMQExtensionEntity.getName());
        assertEquals("1.0.0", hiveMQExtensionEntity.getVersion());
        assertEquals(1000, hiveMQExtensionEntity.getPriority());
        assertEquals("Some Author", hiveMQExtensionEntity.getAuthor());
    }

    @Test(timeout = 5000)
    public void test_missing_id_in_plugin_meta() throws Exception {
        final File extensionXML = temporaryFolder.newFile("hivemq-extension.xml");
        FileUtils.writeStringToFile(extensionXML, "<hivemq-extension>" + //
                "<name>Some Name</name>" + //
                "<version>1.0.0</version>" + //
                "<priority>1000</priority>" + //
                "</hivemq-extension>", Charset.defaultCharset());
        final Optional<HiveMQExtensionEntity> optionalExtensionEntityFromXML =
                HiveMQExtensionXMLReader.getExtensionEntityFromXML(extensionXML.toPath().getParent(), true);
        assertFalse(optionalExtensionEntityFromXML.isPresent());
    }
}
