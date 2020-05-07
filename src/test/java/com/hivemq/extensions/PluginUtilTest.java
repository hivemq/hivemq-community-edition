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
public class PluginUtilTest extends PluginAbstractTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test(timeout = 5000)
    public void test_empty_plugin_folder() throws Exception {
        final File emptyFolder = folder.newFolder("emptyFolder");
        assertFalse(PluginUtil.isValidPluginFolder(emptyFolder.toPath(), true));
    }

    @Test(timeout = 5000)
    public void test_null_folder() throws Exception {
        assertFalse(PluginUtil.isValidPluginFolder(new File("some-path").toPath(), true));
    }

    @Test(timeout = 5000)
    public void test_valid_plugin_folder() throws Exception {
        final File validPluginFolder =
                TestExtensionUtil.createValidExtension(folder.newFolder("extensions"), "validPlugin");

        assertTrue(PluginUtil.isValidPluginFolder(validPluginFolder.toPath(), true));
    }

    @Test(timeout = 5000)
    public void test_folder_jar_only() throws Exception {
        final File validPluginFolder =
                TestExtensionUtil.createValidExtension(folder.newFolder("extensions"), "validPlugin");
        new File(validPluginFolder, "hivemq-extension.xml").delete();

        assertFalse(PluginUtil.isValidPluginFolder(validPluginFolder.toPath(), true));
    }

    @Test(timeout = 5000)
    public void test_folder_xml_only() throws Exception {
        final File validPluginFolder =
                TestExtensionUtil.createValidExtension(folder.newFolder("extension"), "validPlugin");
        new File(validPluginFolder, "extension.jar").delete();

        assertFalse(PluginUtil.isValidPluginFolder(validPluginFolder.toPath(), true));
    }

    @Test
    public void test_plugin_disable() throws Exception {

        final File validPluginFolder =
                TestExtensionUtil.createValidExtension(folder.newFolder("extensions"), "validPlugin");

        final boolean result = PluginUtil.disablePluginFolder(validPluginFolder.toPath());

        assertTrue(result);

        assertTrue(new File(validPluginFolder, "DISABLED").exists());
    }

}