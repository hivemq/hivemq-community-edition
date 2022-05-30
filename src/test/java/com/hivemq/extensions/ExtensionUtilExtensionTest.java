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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.TestExtensionUtil;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Georg Held
 */
public class ExtensionUtilExtensionTest extends AbstractExtensionTest {

    @Rule
    public final @NotNull TemporaryFolder folder = new TemporaryFolder();

    @Test(timeout = 5000)
    public void test_empty_extension_folder() throws Exception {
        final File emptyFolder = folder.newFolder("emptyFolder");
        assertFalse(ExtensionUtil.isValidExtensionFolder(emptyFolder.toPath(), true));
    }

    @Test(timeout = 5000)
    public void test_null_folder() {
        assertFalse(ExtensionUtil.isValidExtensionFolder(new File("some-path").toPath(), true));
    }

    @Test(timeout = 5000)
    public void test_valid_extension_folder() throws Exception {
        final File validExtensionFolder =
                TestExtensionUtil.createValidExtension(folder.newFolder("extensions"), "validExtension");

        assertTrue(ExtensionUtil.isValidExtensionFolder(validExtensionFolder.toPath(), true));
    }

    @Test(timeout = 5000)
    public void test_folder_jar_only() throws Exception {
        final File validExtensionFolder =
                TestExtensionUtil.createValidExtension(folder.newFolder("extensions"), "validExtension");
        assertTrue(new File(validExtensionFolder, "hivemq-extension.xml").delete());

        assertFalse(ExtensionUtil.isValidExtensionFolder(validExtensionFolder.toPath(), true));
    }

    @Test(timeout = 5000)
    public void test_folder_xml_only() throws Exception {
        final File validExtensionFolder =
                TestExtensionUtil.createValidExtension(folder.newFolder("extension"), "validExtension");
        assertTrue(new File(validExtensionFolder, "extension.jar").delete());

        assertFalse(ExtensionUtil.isValidExtensionFolder(validExtensionFolder.toPath(), true));
    }

    @Test
    public void test_extension_disable() throws Exception {
        final File validExtensionFolder =
                TestExtensionUtil.createValidExtension(folder.newFolder("extensions"), "validExtension");

        final boolean result = ExtensionUtil.disableExtensionFolder(validExtensionFolder.toPath());

        assertTrue(result);

        assertTrue(new File(validExtensionFolder, "DISABLED").exists());
    }
}
