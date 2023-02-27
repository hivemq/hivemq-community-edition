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

package com.hivemq.extensions.loader;

import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.AbstractExtensionTest;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensionEntity;
import com.hivemq.extensions.HiveMQExtensionEvent;
import com.hivemq.extensions.HiveMQExtensionImpl;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.config.HiveMQExtensionXMLReader;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtensionLoaderImplExtensionTest extends AbstractExtensionTest {

    private static final @NotNull String invalidExtensionXML = "<hivemq-extension>" +
            "<id>invalid-extension1</id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    private static final @NotNull String invalidExtensionXML2 = "<hivemq-extension>" +
            "<id></id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    private static final @NotNull String validExtensionXML1 = "<hivemq-extension>" +
            "<id>extension1</id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    private static final @NotNull String validExtensionXML2 = "<hivemq-extension>" +
            "<id>extension2</id>" +
            "<name>Some Name</name>" +
            "<version>1.2.3-Version</version>" +
            "<priority>1000</priority>" +
            "</hivemq-extension>";

    @Rule
    public @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull ClassServiceLoader classServiceLoader = mock(ClassServiceLoader.class);
    private final @NotNull ServerInformation serverInformation = mock(ServerInformation.class);
    private final @NotNull ExtensionStaticInitializer staticInitializer = mock(ExtensionStaticInitializer.class);
    private final @NotNull ArgumentCaptor<ClassLoader> captor = ArgumentCaptor.forClass(ClassLoader.class);

    private @NotNull ExtensionLoaderImpl extensionLoader;
    private @NotNull ExtensionLoaderImpl realExtensionLoader;
    private @NotNull HiveMQExtensions hiveMQExtensions;

    @Before
    public void setUp() throws Exception {
        hiveMQExtensions = new HiveMQExtensions(serverInformation);
        extensionLoader = new ExtensionLoaderImpl(classServiceLoader,
                hiveMQExtensions,
                new HiveMQExtensionFactoryImpl(),
                staticInitializer);
        realExtensionLoader = new ExtensionLoaderImpl(new ClassServiceLoader(),
                hiveMQExtensions,
                new HiveMQExtensionFactoryImpl(),
                staticInitializer);
    }

    /***************************
     * loadFromUrls(...) Tests *
     ***************************/

    @Test
    public void test_load_from_empty_urls() {
        final Optional<Class<? extends ExtensionMain>> extensionMain =
                extensionLoader.loadFromUrls(new ArrayList<>(), "test-extension");

        assertFalse(extensionMain.isPresent());
    }

    @Test
    public void test_loaded_with_isolated_extension_classloader() throws Exception {
        final File folder = temporaryFolder.newFolder();

        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        extensionLoader.loadFromUrls(Collections.singletonList(folder.toURI().toURL()), "test-extension");

        verify(classServiceLoader).load(eq(ExtensionMain.class), captor.capture());
        assertTrue(captor.getValue() instanceof IsolatedExtensionClassloader);
    }

    /**
     * We're generating an implementation of the ExtensionMain on the fly, we save it on the file system and then load
     * it into an extension classloader.
     * <p>
     * We're verifying the (extensions) classloader contains our on-the-fly generated class.
     */
    @Test
    public void test_classloader_loads_urls_loadable() throws Exception {
        final File folder = temporaryFolder.newFolder();
        try (final DynamicType.Unloaded<ExtensionMain> extensionMainImpl = new ByteBuddy().subclass(ExtensionMain.class)
                .name("extensionMainImpl")
                .make()) {
            extensionMainImpl.saveIn(folder);
        }

        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        extensionLoader.loadFromUrls(Collections.singletonList(folder.toURI().toURL()), "test-extension");

        // get the actual classloader for the "extension"
        verify(classServiceLoader).load(eq(ExtensionMain.class), captor.capture());
        final ClassLoader value = captor.getValue();

        ClassNotFoundException expected = null;
        try {
            Class.forName("extensionMainImpl");
        } catch (final ClassNotFoundException e) {
            expected = e;
        }
        assertNotNull(expected);

        // if we can load this class without exception, class loading worked
        final Class<?> extensionMainClass = Class.forName("extensionMainImpl", false, value);
        assertTrue(ExtensionMain.class.isAssignableFrom(extensionMainClass));
    }

    @Test
    public void test_classloader_loads_urls() throws Exception {
        final File folder = temporaryFolder.newFolder();

        final List<Class<? extends ExtensionMain>> classes = new ArrayList<>();
        classes.add(TestExtensionMainImpl.class);
        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(classes);

        final Optional<Class<? extends ExtensionMain>> loadedExtension =
                extensionLoader.loadFromUrls(Collections.singletonList(folder.toURI().toURL()), "test-extension");

        assertTrue(loadedExtension.isPresent());
        final Class<? extends ExtensionMain> extensionClass = loadedExtension.get();
        assertTrue(ExtensionMain.class.isAssignableFrom(extensionClass));
        assertEquals(TestExtensionMainImpl.class, extensionClass);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_load_from_urls_list_of_urls_null() {
        extensionLoader.loadFromUrls(null, "test-extension");
    }

    /**************************
     * loadExtensions(...) Tests *
     **************************/

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_load_extensions_folder_null() {
        extensionLoader.loadExtensions(null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_load_extensions_folder_does_not_exist() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder();
        assertTrue(extensionFolder.delete());
        extensionLoader.loadExtensions(extensionFolder.toPath(), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_load_extensions_folder_not_readable() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder();
        assertTrue(extensionFolder.setReadable(false));
        extensionLoader.loadExtensions(extensionFolder.toPath(), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_load_extensions_folder_is_not_a_folder() throws Exception {
        final File extensionFolder = temporaryFolder.newFile();
        extensionLoader.loadExtensions(extensionFolder.toPath(), false);
    }

    @Test
    public void loadExtensions_folderDoesNotExist_permissive() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder();
        assertTrue(extensionFolder.delete());
        final @NotNull ImmutableSet<HiveMQExtensionEvent> hiveMQExtensionEvents =
                extensionLoader.loadExtensions(extensionFolder.toPath(), true);

        assertTrue(hiveMQExtensionEvents.isEmpty());
    }

    @Test
    public void loadExtensions_folderNotReadable_permissive() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder();
        assertTrue(extensionFolder.setReadable(false));
        final @NotNull ImmutableSet<HiveMQExtensionEvent> hiveMQExtensionEvents =
                extensionLoader.loadExtensions(extensionFolder.toPath(), true);

        assertTrue(hiveMQExtensionEvents.isEmpty());
    }

    @Test
    public void loadExtensions_folderIsNotAFolder_permissive() throws Exception {
        final File extensionFolder = temporaryFolder.newFile();
        final @NotNull ImmutableSet<HiveMQExtensionEvent> hiveMQExtensionEvents =
                extensionLoader.loadExtensions(extensionFolder.toPath(), true);

        assertTrue(hiveMQExtensionEvents.isEmpty());
    }

    @Test
    public void test_load_extensions_from_empty_folder() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder();
        final Collection<HiveMQExtensionEvent> extensions =
                extensionLoader.loadExtensions(extensionFolder.toPath(), false);

        assertTrue(extensions.isEmpty());
    }

    @Test
    public void test_load_extensions_folder_has_no_jar_but_class_file() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension-1");
        try (final DynamicType.Unloaded<ExtensionMain> extensionMainImpl = new ByteBuddy().subclass(ExtensionMain.class)
                .name("extensionMainImpl")
                .make()) {
            extensionMainImpl.saveIn(extensionFolder);
        }

        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());

        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        extensionLoader.loadExtensions(extensionFolder.toPath(), false);

        // only JAR files are considered (class files are not allowed)
        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));
    }

    @Test
    public void test_load_extensions_folder_contains_jar_file() throws Exception {
        final File extensionsFolder = temporaryFolder.newFolder("extension");
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");
        final File file = new File(extensionFolder, "extension.jar");
        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        javaArchive.as(ZipExporter.class).exportTo(file);

        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        extensionLoader.loadExtensions(extensionsFolder.toPath(), false);

        // let's verify that the extension was loaded
        verify(classServiceLoader).load(any(), any(ClassLoader.class));
    }

    @Test
    public void test_load_extensions_folder_contain_wrong_id_in_xml_disabling_failed() throws Exception {
        final File extensionsFolder = temporaryFolder.newFolder("extension");
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");
        final File file = new File(extensionFolder, "extension.jar");
        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                invalidExtensionXML,
                Charset.defaultCharset());
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        javaArchive.as(ZipExporter.class).exportTo(file);

        assertTrue(extensionFolder.setWritable(false));

        final @NotNull ImmutableSet<HiveMQExtensionEvent> extensionEvents =
                extensionLoader.loadExtensions(extensionsFolder.toPath(), false);
        assertEquals(0, extensionEvents.size());

        // let's verify that the extension was not loaded
        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));

        final File[] listFiles = extensionFolder.listFiles();
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
    public void test_load_extensions_folder_contain_wrong_id_in_xml() throws Exception {
        final File extensionsFolder = temporaryFolder.newFolder("extension");
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");
        final File file = new File(extensionFolder, "extension.jar");
        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                invalidExtensionXML,
                Charset.defaultCharset());
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        javaArchive.as(ZipExporter.class).exportTo(file);

        final @NotNull ImmutableSet<HiveMQExtensionEvent> extensionEvents =
                extensionLoader.loadExtensions(extensionsFolder.toPath(), false);
        assertEquals(0, extensionEvents.size());

        // let's verify that the extension was not loaded
        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));

        final File[] listFiles = extensionFolder.listFiles();
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
    public void test_load_extensions_folder_contains_two_extension_folders() throws Exception {
        final File extensionsFolder = temporaryFolder.newFolder("extension");
        final File extensionFolder1 = temporaryFolder.newFolder("extension", "extension1");
        final File extensionFolder2 = temporaryFolder.newFolder("extension", "extension2");

        final File file = new File(extensionFolder1, "extension.jar");
        final File file2 = new File(extensionFolder2, "extension.jar");
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(extensionFolder1.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        FileUtils.writeStringToFile(extensionFolder2.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML2,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);
        javaArchive.as(ZipExporter.class).exportTo(file2);

        when(classServiceLoader.load(eq(ExtensionMain.class), any(ClassLoader.class))).thenReturn(new ArrayList<>());

        extensionLoader.loadExtensions(extensionsFolder.toPath(), false);

        // let's verify that the extension was not loaded
        verify(classServiceLoader, times(2)).load(any(), any(ClassLoader.class));
    }

    /*******************************************
     * processSingleExtensionFolder(...) Tests *
     *******************************************/

    @Test
    public void test_process_single_extension_folder_xml_invalid() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");

        final File file = new File(extensionFolder, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                invalidExtensionXML2,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQExtensionEvent event = extensionLoader.processSingleExtensionFolder(extensionFolder.toPath());
        assertNull(event);

        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));
    }

    @Test
    public void test_process_single_extension_folder_state_already_known() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");

        final File file = new File(extensionFolder, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        hiveMQExtensions.addHiveMQExtension(new HiveMQExtensionImpl(new HiveMQExtensionEntity("extension1",
                "my_extension",
                "1.0.0",
                1,
                1,
                "author"), extensionFolder.toPath(), new TestExtensionMainImpl(), true));

        final HiveMQExtensionEvent event = extensionLoader.processSingleExtensionFolder(extensionFolder.toPath());
        assertNull(event);

        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));
    }

    @Test
    public void test_process_single_extension_folder_and_extension_same_folder_other_id_disabled() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");

        final File file = new File(extensionFolder, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        assertTrue(extensionFolder.toPath().resolve("DISABLED").toFile().createNewFile());

        javaArchive.as(ZipExporter.class).exportTo(file);

        hiveMQExtensions.addHiveMQExtension(new HiveMQExtensionImpl(new HiveMQExtensionEntity("extension-other-id",
                "my_extension",
                "1.0.0",
                1,
                1,
                "author"), extensionFolder.toPath(), new TestExtensionMainImpl(), false));

        final HiveMQExtensionEvent event = extensionLoader.processSingleExtensionFolder(extensionFolder.toPath());
        assertNull(event);

        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));
    }

    @Test
    public void test_process_single_extension_folder_and_extension_other_folder_same_id_enabling() throws Exception {
        final File extensionFolder1 = temporaryFolder.newFolder("extension", "extension1");
        final File extensionFolder2 = temporaryFolder.newFolder("extension", "extension2");

        hiveMQExtensions.addHiveMQExtension(new HiveMQExtensionImpl(new HiveMQExtensionEntity("extension1",
                "my_extension",
                "1.0.0",
                1,
                1,
                "author"), extensionFolder1.toPath(), new TestExtensionMainImpl(), false));

        final File file = new File(extensionFolder2, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(extensionFolder2.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQExtensionEvent event = extensionLoader.processSingleExtensionFolder(extensionFolder2.toPath());
        assertNull(event);

        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));
    }

    @Test
    public void test_process_single_extension_folder_and_extension_other_folder_same_id_disabling() throws Exception {
        final File extensionFolder1 = temporaryFolder.newFolder("extension", "extension1");
        final File extensionFolder2 = temporaryFolder.newFolder("extension", "extension2");

        hiveMQExtensions.addHiveMQExtension(new HiveMQExtensionImpl(new HiveMQExtensionEntity("extension1",
                "my_extension",
                "1.0.0",
                1,
                1,
                "author"), extensionFolder1.toPath(), new TestExtensionMainImpl(), true));

        final File file = new File(extensionFolder2, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(extensionFolder2.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        assertTrue(extensionFolder2.toPath().resolve("DISABLED").toFile().createNewFile());

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQExtensionEvent event = extensionLoader.processSingleExtensionFolder(extensionFolder2.toPath());
        // NO DISABLE EVENT HERE IS VERY IMPORTANT
        assertNull(event);

        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));
    }

    @Test
    public void test_process_single_extension_folder_disabled() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");

        final File file = new File(extensionFolder, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        assertTrue(extensionFolder.toPath().resolve("DISABLED").toFile().createNewFile());

        javaArchive.as(ZipExporter.class).exportTo(file);

        hiveMQExtensions.addHiveMQExtension(new HiveMQExtensionImpl(new HiveMQExtensionEntity("extension1",
                "my_extension",
                "1.0.0",
                1,
                1,
                "author"), extensionFolder.toPath(), new TestExtensionMainImpl(), true));

        final HiveMQExtensionEvent event = extensionLoader.processSingleExtensionFolder(extensionFolder.toPath());
        assertNotNull(event);

        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));
    }

    @Test
    public void test_process_single_extension_folder_known_id_enabled() throws Exception {
        hiveMQExtensions = Mockito.mock(HiveMQExtensions.class);

        extensionLoader = new ExtensionLoaderImpl(classServiceLoader,
                hiveMQExtensions,
                new HiveMQExtensionFactoryImpl(),
                staticInitializer);

        when(hiveMQExtensions.isHiveMQExtensionKnown(anyString(), any(Path.class), anyBoolean())).thenReturn(false);
        when(hiveMQExtensions.isHiveMQExtensionEnabled(anyString())).thenReturn(true);
        when(hiveMQExtensions.isHiveMQExtensionIDKnown(anyString())).thenReturn(true);

        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");

        final File file = new File(extensionFolder, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQExtensionEvent event = extensionLoader.processSingleExtensionFolder(extensionFolder.toPath());
        assertNull(event);

        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));
    }

    @Test
    public void test_process_single_extension_folder_different_id_enabled() throws Exception {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");

        final File file = new File(extensionFolder, "extension.jar");

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);

        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                invalidExtensionXML,
                Charset.defaultCharset());

        javaArchive.as(ZipExporter.class).exportTo(file);

        final HiveMQExtensionEvent event = extensionLoader.processSingleExtensionFolder(extensionFolder.toPath());
        assertNull(event);

        verify(classServiceLoader, never()).load(any(), any(ClassLoader.class));

        final File[] listFiles = extensionFolder.listFiles();
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
     * loadSingleExtension(...) Tests *
     *******************************/

    @Test(timeout = 5000)
    public void test_load_single_extension_load_and_instantiate_enabled() throws Throwable {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");
        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        final File file = new File(extensionFolder, "extension.jar");
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainImpl.class);
        javaArchive.as(ZipExporter.class).exportTo(file);

        final Optional<HiveMQExtensionEntity> extensionEntityFromXML =
                HiveMQExtensionXMLReader.getExtensionEntityFromXML(extensionFolder.toPath(), true);
        assertTrue(extensionEntityFromXML.isPresent());
        final HiveMQExtension hiveMQExtension =
                realExtensionLoader.loadSingleExtension(extensionFolder.toPath(), extensionEntityFromXML.get());

        assertNotNull(hiveMQExtension);
        hiveMQExtension.start(super.getTestExtensionStartInput(), super.getTestExtensionStartOutput());
        assertTrue(hiveMQExtension.isEnabled());
    }

    @Test(timeout = 5000)
    public void test_load_single_extension_load_and_instantiate_no_noarg_constructor() throws Throwable {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");
        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        final File file = new File(extensionFolder, "extension.jar");
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMainConstructorParamImpl.class);
        javaArchive.as(ZipExporter.class).exportTo(file);

        final Optional<HiveMQExtensionEntity> extensionEntityFromXML =
                HiveMQExtensionXMLReader.getExtensionEntityFromXML(extensionFolder.toPath(), true);
        assertTrue(extensionEntityFromXML.isPresent());
        final HiveMQExtension hiveMQExtension =
                realExtensionLoader.loadSingleExtension(extensionFolder.toPath(), extensionEntityFromXML.get());

        assertNull(hiveMQExtension);
    }

    @Test(timeout = 5000)
    public void test_load_single_extension_when_load_constructor_throws_exception_then_extension_is_not_loaded()
            throws Exception {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");
        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        final File file = new File(extensionFolder, "extension.jar");
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestConstructorThrowsExceptionExtensionMain.class);
        javaArchive.as(ZipExporter.class).exportTo(file);

        final Optional<HiveMQExtensionEntity> extensionEntityFromXML =
                HiveMQExtensionXMLReader.getExtensionEntityFromXML(extensionFolder.toPath(), true);
        assertTrue(extensionEntityFromXML.isPresent());
        final HiveMQExtension hiveMQExtension =
                realExtensionLoader.loadSingleExtension(extensionFolder.toPath(), extensionEntityFromXML.get());

        assertNull(hiveMQExtension);
    }

    @Test(timeout = 5000)
    public void test_load_single_extension_when_load_constructor_throws_error_then_extension_is_not_loaded()
            throws Exception {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");
        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        final File file = new File(extensionFolder, "extension.jar");
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestConstructorThrowsErrorExtensionMain.class);
        javaArchive.as(ZipExporter.class).exportTo(file);

        final Optional<HiveMQExtensionEntity> extensionEntityFromXML =
                HiveMQExtensionXMLReader.getExtensionEntityFromXML(extensionFolder.toPath(), true);
        assertTrue(extensionEntityFromXML.isPresent());

        final HiveMQExtension hiveMQExtension =
                realExtensionLoader.loadSingleExtension(extensionFolder.toPath(), extensionEntityFromXML.get());

        assertNull(hiveMQExtension);
    }

    @Test(timeout = 5000)
    public void test_load_single_extension_when_init_class_throws_error_then_extension_is_not_loaded()
            throws Exception {
        final File extensionFolder = temporaryFolder.newFolder("extension", "extension1");
        FileUtils.writeStringToFile(extensionFolder.toPath().resolve("hivemq-extension.xml").toFile(),
                validExtensionXML1,
                Charset.defaultCharset());
        final File file = new File(extensionFolder, "extension.jar");
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestClassInitThrowsErrorExtensionMain.class);
        javaArchive.as(ZipExporter.class).exportTo(file);

        final Optional<HiveMQExtensionEntity> extensionEntityFromXML =
                HiveMQExtensionXMLReader.getExtensionEntityFromXML(extensionFolder.toPath(), true);
        assertTrue(extensionEntityFromXML.isPresent());
        final HiveMQExtension hiveMQExtension =
                realExtensionLoader.loadSingleExtension(extensionFolder.toPath(), extensionEntityFromXML.get());

        assertNull(hiveMQExtension);
    }

    public static class TestExtensionMainImpl implements ExtensionMain {

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {
        }
    }

    public static class TestExtensionMainConstructorParamImpl implements ExtensionMain {

        private final @NotNull String badString;

        public TestExtensionMainConstructorParamImpl(final @NotNull String badString) {
            this.badString = badString;
        }

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {
            System.out.println(badString);
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {
        }
    }
}
