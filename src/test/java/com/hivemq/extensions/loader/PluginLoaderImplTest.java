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

package com.hivemq.extensions.loader;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.*;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.config.HiveMQPluginXMLReader;
import net.bytebuddy.ByteBuddy;
import org.apache.commons.io.FileUtils;
import org.hamcrest.core.IsInstanceOf;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PluginLoaderImplTest extends PluginAbstractTest {

    private static final String invalidPluginXML = "<hivemq-extension>" +
            "<id>invalid-plugin1</id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    private static final String invalidPluginXML2 = "<hivemq-extension>" +
            "<id></id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    private static final String validPluginXML1 = "<hivemq-extension>" +
            "<id>plugin1</id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    private static final String validPluginXML2 = "<hivemq-extension>" +
            "<id>plugin2</id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private ClassServiceLoader classServiceLoader;

    @Mock
    private ServerInformation serverInformation;

    @Mock
    private PluginStaticInitializer staticInitializer;

    @Captor
    private ArgumentCaptor<ClassLoader> captor;

    private PluginLoaderImpl pluginLoader;
    private PluginLoaderImpl realPluginLoader;
    private HiveMQExtensions hiveMQExtensions;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        hiveMQExtensions = new HiveMQExtensions(serverInformation);
        pluginLoader = new PluginLoaderImpl(classServiceLoader, hiveMQExtensions, new HiveMQPluginFactoryImpl(),
                staticInitializer);
        realPluginLoader =
                new PluginLoaderImpl(new ClassServiceLoader(), hiveMQExtensions, new HiveMQPluginFactoryImpl(),
                        staticInitializer);
    }

    /***************************
     * loadFromUrls(...) Tests *
     ***************************/

    @Test
    public void test_load_from_empty_urls() {
        final Optional<Class<? extends ExtensionMain>> pluginMain =
                pluginLoader.loadFromUrls(new ArrayList<>(), ExtensionMain.class, "test-extension");

        assertFalse(pluginMain.isPresent());
    }

    @Test
    public void test_loaded_with_isolated_plugin_classloader() throws Exception {
        final File folder = temporaryFolder.newFolder();

        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        pluginLoader.loadFromUrls(
                Collections.singletonList(folder.toURI().toURL()), ExtensionMain.class, "test-extension");

        verify(classServiceLoader).load(eq(ExtensionMain.class), captor.capture());
        assertThat(captor.getValue(), IsInstanceOf.instanceOf(IsolatedPluginClassloader.class));
    }

    @Test
    public void test_classloader_loads_urls_loadable() throws Exception {

        /*
            We're generating an implementation of the HiveMQPluginModule on the fly,
            we save it on the file system and then load it into an extension classloader.

            We're verifying the (extensions) classloader contains our on-the-fly
            generated class
         */

        final File folder = temporaryFolder.newFolder();
        new ByteBuddy().
                subclass(ExtensionMain.class).
                name("PluginMainImpl").
                make().saveIn(folder);


        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        pluginLoader.loadFromUrls(
                Collections.singletonList(folder.toURI().toURL()), ExtensionMain.class, "test-extension");

        //Get the actual classloader for the "extension"
        verify(classServiceLoader).load(eq(ExtensionMain.class), captor.capture());
        final ClassLoader value = captor.getValue();


        ClassNotFoundException expected = null;
        try {
            Class.forName("PluginMainImpl");
        } catch (final ClassNotFoundException e) {
            expected = e;
        }
        assertNotNull(expected);

        //If we can load this class without exception, class loading worked!
        final Class<?> hiveMQPluginModuleImpl = Class.forName("PluginMainImpl", false, value);

        assertTrue(ExtensionMain.class.isAssignableFrom(hiveMQPluginModuleImpl));
    }

    @Test
    public void test_classloader_loads_urls() throws Exception {

        final File folder = temporaryFolder.newFolder();

        final List<Class<? extends ExtensionMain>> classes = new ArrayList<>();
        classes.add(TestExtensionMainImpl.class);
        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(classes);

        final Optional<Class<? extends ExtensionMain>> loadedPlugin =
                pluginLoader.loadFromUrls(Collections.singletonList(folder.toURI().toURL()), ExtensionMain.class,
                        "test-extension");

        assertTrue(loadedPlugin.isPresent());
        final Class<? extends ExtensionMain> pluginClass = loadedPlugin.get();
        assertTrue(ExtensionMain.class.isAssignableFrom(pluginClass));
        assertEquals(TestExtensionMainImpl.class, pluginClass);
    }

    @Test(expected = NullPointerException.class)
    public void test_load_from_urls_class_to_search_null() throws Exception {

        final File folder = temporaryFolder.newFolder();

        pluginLoader.loadFromUrls(Collections.singletonList(folder.toURI().toURL()), null, "test-extension");
    }

    @Test(expected = NullPointerException.class)
    public void test_load_from_urls_list_of_urls_null() {

        pluginLoader.loadFromUrls(null, ExtensionMain.class, "test-extension");
    }

    /**************************
     * loadPlugins(...) Tests *
     **************************/

    @Test(expected = NullPointerException.class)
    public void test_load_plugins_folder_null() {

        pluginLoader.loadPlugins(null, false, ExtensionMain.class);
    }

    @Test(expected = NullPointerException.class)
    public void test_load_plugins_desired_plugin_null() throws Exception {

        pluginLoader.loadPlugins(temporaryFolder.newFolder().toPath(), false, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_load_plugins_folder_does_not_exist() throws Exception {

        final File pluginFolder = temporaryFolder.newFolder();
        assertTrue(pluginFolder.delete());
        pluginLoader.loadPlugins(pluginFolder.toPath(), false, ExtensionMain.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_load_plugins_folder_not_readable() throws Exception {

        final File pluginFolder = temporaryFolder.newFolder();
        assertTrue(pluginFolder.setReadable(false));
        pluginLoader.loadPlugins(pluginFolder.toPath(), false, ExtensionMain.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_load_plugins_folder_is_not_a_folder() throws Exception {

        final File pluginFolder = temporaryFolder.newFile();
        pluginLoader.loadPlugins(pluginFolder.toPath(), false, ExtensionMain.class);
    }

    @Test
    public void loadPlugins_folderDoesNotExist_permissive() throws IOException {

        final File pluginFolder = temporaryFolder.newFolder();
        assertTrue(pluginFolder.delete());
        final ImmutableList<HiveMQPluginEvent> hiveMQPluginEvents =
                pluginLoader.loadPlugins(pluginFolder.toPath(), true, ExtensionMain.class);

        assertTrue(hiveMQPluginEvents.isEmpty());
    }

    @Test
    public void loadPlugins_folderNotReadable_permissive() throws IOException {

        final File pluginFolder = temporaryFolder.newFolder();
        assertTrue(pluginFolder.setReadable(false));
        final ImmutableList<HiveMQPluginEvent> hiveMQPluginEvents =
                pluginLoader.loadPlugins(pluginFolder.toPath(), true, ExtensionMain.class);

        assertTrue(hiveMQPluginEvents.isEmpty());
    }

    @Test
    public void loadPlugins_folderIsNotAFolder_permissive() throws IOException {

        final File pluginFolder = temporaryFolder.newFile();
        final ImmutableList<HiveMQPluginEvent> hiveMQPluginEvents =
                pluginLoader.loadPlugins(pluginFolder.toPath(), true, ExtensionMain.class);

        assertTrue(hiveMQPluginEvents.isEmpty());
    }

    @Test
    public void test_load_plugins_from_empty_folder() throws Exception {

        final File pluginFolder = temporaryFolder.newFolder();
        final Collection<HiveMQPluginEvent> extensions = pluginLoader.loadPlugins(pluginFolder.toPath(), false,
                ExtensionMain.class);

        assertTrue(extensions.isEmpty());
    }

    @Test
    public void test_load_plugins_folder_has_no_jar_but_class_file() throws Exception {

        final File pluginFolder = temporaryFolder.newFolder("extension", "extension-1");
        new ByteBuddy().
                subclass(ExtensionMain.class).
                name("PluginMainImpl").
                make().saveIn(pluginFolder);

        FileUtils.writeStringToFile(
                pluginFolder.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());

        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        pluginLoader.loadPlugins(pluginFolder.toPath(), false, ExtensionMain.class);

        //Only JAR files are considered. Class files are not allowed
        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));
    }

    @Test
    public void test_load_plugins_folder_contains_jar_file() throws Exception {
        final File pluginsFolder = temporaryFolder.newFolder("extension");
        final File pluginFolder = temporaryFolder.newFolder("extension", "plugin1");
        final File file = new File(pluginFolder, "extension.jar");
        FileUtils.writeStringToFile(
                pluginFolder.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        javaArchive.as(ZipExporter.class).exportTo(file);

        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        pluginLoader.loadPlugins(pluginsFolder.toPath(), false, ExtensionMain.class);

        //Let's verify that the extension was loaded
        verify(classServiceLoader).load(any(Class.class), any(ClassLoader.class));
    }

    @Test
    public void test_load_plugins_folder_contain_wrong_id_in_xml_disabling_failed() throws Exception {
        final File pluginsFolder = temporaryFolder.newFolder("extension");
        final File pluginFolder = temporaryFolder.newFolder("extension", "plugin1");
        final File file = new File(pluginFolder, "extension.jar");
        FileUtils.writeStringToFile(
                pluginFolder.toPath().resolve("hivemq-extension.xml").toFile(), invalidPluginXML,
                Charset.defaultCharset());
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        javaArchive.as(ZipExporter.class).exportTo(file);

        pluginFolder.setWritable(false);

        final ImmutableList<HiveMQPluginEvent> pluginEvents = pluginLoader.loadPlugins(pluginsFolder.toPath(), false,
                ExtensionMain.class);

        assertEquals(0, pluginEvents.size());

        //Let's verify that the extension was not loaded
        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

        final File[] listFiles = pluginFolder.listFiles();
        assertNotNull(listFiles);
        assertEquals(2, listFiles.length);

        boolean disabledFileFound = false;

        for (final File listFile : listFiles) {
            if (listFile != null && listFile.getName().equals("DISABLED")) {
                disabledFileFound = true;
            }
        }

        assertFalse(disabledFileFound);

    }

    @Test
    public void test_load_plugins_folder_contain_wrong_id_in_xml() throws Exception {
        final File pluginsFolder = temporaryFolder.newFolder("extension");
        final File pluginFolder = temporaryFolder.newFolder("extension", "plugin1");
        final File file = new File(pluginFolder, "extension.jar");
        FileUtils.writeStringToFile(
                pluginFolder.toPath().resolve("hivemq-extension.xml").toFile(), invalidPluginXML,
                Charset.defaultCharset());
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        javaArchive.as(ZipExporter.class).exportTo(file);

        final ImmutableList<HiveMQPluginEvent> pluginEvents = pluginLoader.loadPlugins(pluginsFolder.toPath(), false,
                ExtensionMain.class);

        assertEquals(0, pluginEvents.size());

        //Let's verify that the extension was not loaded
        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

        final File[] listFiles = pluginFolder.listFiles();
        assertNotNull(listFiles);
        assertEquals(2, listFiles.length);

        boolean disabledFileFound = false;

        for (final File listFile : listFiles) {
            if (listFile != null && listFile.getName().equals("DISABLED")) {
                disabledFileFound = true;
            }
        }

        assertFalse(disabledFileFound);

    }

    @Test
    public void test_load_plugins_folder_contains_two_plugin_folders() throws Exception {

        final File pluginsFolder = temporaryFolder.newFolder("extension");
        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");
        final File pluginFolder2 = temporaryFolder.newFolder("extension", "plugin2");


        final File file = new File(pluginFolder1, "extension.jar");
        final File file2 = new File(pluginFolder2, "extension.jar");
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(
                pluginFolder1.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());
        FileUtils.writeStringToFile(
                pluginFolder2.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML2,
                Charset.defaultCharset());


        javaArchive.as(ZipExporter.class).exportTo(file);
        javaArchive.as(ZipExporter.class).exportTo(file2);

        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        pluginLoader.loadPlugins(pluginsFolder.toPath(), false, ExtensionMain.class);

        //Let's verify that the extensions were loaded
        verify(classServiceLoader, times(2)).load(any(Class.class), any(ClassLoader.class));
    }

    /****************************************
     * processSinglePluginFolder(...) Tests *
     ****************************************/

    @Test
    public void test_process_single_plugin_folder_xml_invalid() throws Exception {

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");

        final File file = new File(pluginFolder1, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(
                pluginFolder1.toPath().resolve("hivemq-extension.xml").toFile(), invalidPluginXML2,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQPluginEvent event =
                pluginLoader.processSinglePluginFolder(pluginFolder1.toPath(), ExtensionMain.class);

        assertNull(event);

        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

    }

    @Test
    public void test_process_single_plugin_folder_state_already_known() throws Exception {

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");

        final File file = new File(pluginFolder1, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(
                pluginFolder1.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        hiveMQExtensions.addHiveMQPlugin(
                new HiveMQExtensionImpl(
                        new HiveMQPluginEntity("plugin1", "my_plugin", "1.0.0", 1, 1, "author"),
                        pluginFolder1.toPath(),
                        new TestExtensionMainImpl(),
                        true));

        final HiveMQPluginEvent event =
                pluginLoader.processSinglePluginFolder(pluginFolder1.toPath(), ExtensionMain.class);

        assertNull(event);

        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

    }

    @Test
    public void test_process_single_plugin_folder_and_plugin_same_folder_other_id_disabled() throws Exception {

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");

        final File file = new File(pluginFolder1, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(
                pluginFolder1.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());
        pluginFolder1.toPath().resolve("DISABLED").toFile().createNewFile();

        javaArchive.as(ZipExporter.class).exportTo(file);

        hiveMQExtensions.addHiveMQPlugin(
                new HiveMQExtensionImpl(
                        new HiveMQPluginEntity("plugin-other-id", "my_plugin", "1.0.0", 1, 1, "author"),
                        pluginFolder1.toPath(),
                        new TestExtensionMainImpl(),
                        false));

        final HiveMQPluginEvent event =
                pluginLoader.processSinglePluginFolder(pluginFolder1.toPath(), ExtensionMain.class);

        assertNull(event);

        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

    }

    @Test
    public void test_process_single_plugin_folder_and_plugin_other_folder_same_id_enabling() throws Exception {

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");
        final File pluginFolder2 = temporaryFolder.newFolder("extension", "plugin2");

        hiveMQExtensions.addHiveMQPlugin(
                new HiveMQExtensionImpl(
                        new HiveMQPluginEntity("plugin1", "my_plugin", "1.0.0", 1, 1, "author"),
                        pluginFolder1.toPath(),
                        new TestExtensionMainImpl(),
                        false));

        final File file = new File(pluginFolder2, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(
                pluginFolder2.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQPluginEvent event =
                pluginLoader.processSinglePluginFolder(pluginFolder2.toPath(), ExtensionMain.class);

        assertNull(event);

        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

    }

    @Test
    public void test_process_single_plugin_folder_and_plugin_other_folder_same_id_disabling() throws Exception {

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");
        final File pluginFolder2 = temporaryFolder.newFolder("extension", "plugin2");

        hiveMQExtensions.addHiveMQPlugin(
                new HiveMQExtensionImpl(
                        new HiveMQPluginEntity("plugin1", "my_plugin", "1.0.0", 1, 1, "author"),
                        pluginFolder1.toPath(),
                        new TestExtensionMainImpl(),
                        true));

        final File file = new File(pluginFolder2, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(
                pluginFolder2.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());
        pluginFolder2.toPath().resolve("DISABLED").toFile().createNewFile();

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQPluginEvent event =
                pluginLoader.processSinglePluginFolder(pluginFolder2.toPath(), ExtensionMain.class);

        //NO DISABLE EVENT HERE IS VERY IMPORTANT
        assertNull(event);

        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

    }

    @Test
    public void test_process_single_plugin_folder_disabled() throws Exception {

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");

        final File file = new File(pluginFolder1, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(
                pluginFolder1.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());
        pluginFolder1.toPath().resolve("DISABLED").toFile().createNewFile();

        javaArchive.as(ZipExporter.class).exportTo(file);

        hiveMQExtensions.addHiveMQPlugin(
                new HiveMQExtensionImpl(
                        new HiveMQPluginEntity("plugin1", "my_plugin", "1.0.0", 1, 1, "author"),
                        pluginFolder1.toPath(),
                        new TestExtensionMainImpl(),
                        true));

        final HiveMQPluginEvent event =
                pluginLoader.processSinglePluginFolder(pluginFolder1.toPath(), ExtensionMain.class);

        assertNotNull(event);

        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

    }

    @Test
    public void test_process_single_plugin_folder_known_id_enabled() throws Exception {

        hiveMQExtensions = Mockito.mock(HiveMQExtensions.class);

        pluginLoader = new PluginLoaderImpl(classServiceLoader, hiveMQExtensions, new HiveMQPluginFactoryImpl(),
                staticInitializer);

        when(hiveMQExtensions.isHiveMQExtensionKnown(anyString(), any(Path.class), anyBoolean())).thenReturn(false);
        when(hiveMQExtensions.isHiveMQExtensionEnabled(anyString())).thenReturn(true);
        when(hiveMQExtensions.isHiveMQPluginIDKnown(anyString())).thenReturn(true);

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");

        final File file = new File(pluginFolder1, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(
                pluginFolder1.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQPluginEvent event =
                pluginLoader.processSinglePluginFolder(pluginFolder1.toPath(), ExtensionMain.class);

        assertNull(event);

        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

    }

    @Test
    public void test_process_single_plugin_folder_different_id_enabled() throws Exception {

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");

        final File file = new File(pluginFolder1, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(
                pluginFolder1.toPath().resolve("hivemq-extension.xml").toFile(), invalidPluginXML,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQPluginEvent event =
                pluginLoader.processSinglePluginFolder(pluginFolder1.toPath(), ExtensionMain.class);

        assertNull(event);

        verify(classServiceLoader, never()).load(any(Class.class), any(ClassLoader.class));

        final File[] listFiles = pluginFolder1.listFiles();
        assertNotNull(listFiles);
        assertEquals(2, listFiles.length);

        boolean disabledFileFound = false;

        for (final File listFile : listFiles) {
            if (listFile != null && listFile.getName().equals("DISABLED")) {
                disabledFileFound = true;
            }
        }

        assertFalse(disabledFileFound);

    }

    /*******************************
     * loadSinglePlugin(...) Tests *
     *******************************/

    @Test(timeout = 5000)
    public void test_load_single_plugin_load_and_instantiate_enabled() throws Throwable {

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");
        FileUtils.writeStringToFile(
                pluginFolder1.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());
        final File file = new File(pluginFolder1, "extension.jar");
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);
        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQExtension hiveMQExtension = realPluginLoader.loadSinglePlugin(
                pluginFolder1.toPath(),
                HiveMQPluginXMLReader.getPluginEntityFromXML(pluginFolder1.toPath(), true).get(),
                ExtensionMain.class);

        assertNotNull(hiveMQExtension);
        hiveMQExtension.start(super.getTestPluginStartInput(), super.getTestPluginStartOutput());
        assertTrue(hiveMQExtension.isEnabled());
    }

    @Test(timeout = 5000)
    public void test_load_single_plugin_load_and_instantiate_no_noarg_constructor() throws Throwable {

        final File pluginFolder1 = temporaryFolder.newFolder("extension", "plugin1");
        FileUtils.writeStringToFile(
                pluginFolder1.toPath().resolve("hivemq-extension.xml").toFile(), validPluginXML1,
                Charset.defaultCharset());
        final File file = new File(pluginFolder1, "extension.jar");
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainConstructorParamImpl.class);
        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQExtension hiveMQExtension = realPluginLoader.loadSinglePlugin(
                pluginFolder1.toPath(),
                HiveMQPluginXMLReader.getPluginEntityFromXML(pluginFolder1.toPath(), true).get(),
                ExtensionMain.class);

        assertNull(hiveMQExtension);
    }

    public static class TestExtensionMainImpl implements ExtensionMain {

        @Override
        public void extensionStart(final ExtensionStartInput input, final ExtensionStartOutput output) {
        }

        @Override
        public void extensionStop(final ExtensionStopInput input, final ExtensionStopOutput output) {
        }
    }

    public static class TestExtensionMainConstructorParamImpl implements ExtensionMain {

        private final String badString;

        public TestExtensionMainConstructorParamImpl(final String badString) {
            this.badString = badString;
        }

        @Override
        public void extensionStart(final ExtensionStartInput input, final ExtensionStartOutput output) {
        }

        @Override
        public void extensionStop(final ExtensionStopInput input, final ExtensionStopOutput output) {
        }
    }
}