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

package com.hivemq.extensions.classloader;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.classloader.ClassLoaderTestClass;
import com.hivemq.extension.sdk.api.services.Services;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.OnTheFlyCompilationUtil;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class IsolatedExtensionClassloaderTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private @NotNull File folder;

    @Before
    public void setUp() throws Exception {
        folder = temporaryFolder.newFolder();
    }

    /**
     * This test contains pure magic. It does the following:
     * 1. Finds out the Java source file
     * 2. Copy the Java source file
     * 3. Modify the Java source file
     * 4. Compile the Java source file
     * 5. Load the just compiled Java source file
     */
    @Test
    public void test_modified_class_loaded() throws Exception {
        final File javaSrcFile = getJavaSrcFileForClassFile(ClassLoadedClass.class);
        final File file = temporaryFolder.newFile(ClassLoadedClass.class.getSimpleName() + ".java");
        FileUtils.copyFile(javaSrcFile, file);

        replaceFileContent(file, "original", "modified");

        // actually compile the file
        OnTheFlyCompilationUtil.compileJavaFile(file, folder);

        final Class<?> aClass;
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(new URL[]{folder.toURI().toURL()}, getClass().getClassLoader())) {
            aClass = loader.loadClass(ClassLoadedClass.class.getCanonicalName());
        }

        // we can't cast to ClassLoadedClass because the parent classloader already has the "original" class loaded
        // casting would result in a ClassCastException!
        final Object classLoadedClass = aClass.getDeclaredConstructor().newInstance();
        final String output = (String) aClass.getDeclaredMethod("get").invoke(classLoadedClass);

        assertEquals("modified", output);

        // now let's check that the original class is not affected.
        // we're loading from the parent classloader
        final ClassLoadedClass originalClassloadedClass = new ClassLoadedClass();
        assertNotEquals(originalClassloadedClass.get(), output);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_original_class_loaded_delegate() throws Exception {
        final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(getClass().getClassLoader(), null);

        final Class<?> aClass = loader.loadClass(ClassLoadedClass.class.getCanonicalName());

        final String output = (String) aClass.getDeclaredMethod("get").invoke(new ClassLoadedClass());

        assertEquals("original", output);
        loader.close();
    }

    @Test
    public void test_restricted_class_loaded_from_parent() throws Exception {
        final File javaSrcFile = getJavaSrcFileForClassFile(ClassLoaderTestClass.class);
        final File file = temporaryFolder.newFile(ClassLoaderTestClass.class.getSimpleName() + ".java");
        FileUtils.copyFile(javaSrcFile, file);

        replaceFileContent(file, "original", "modified");

        // actually compile the file
        OnTheFlyCompilationUtil.compileJavaFile(file, folder);

        final Class<?> aClass;
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(new URL[]{folder.toURI().toURL()}, getClass().getClassLoader())) {
            aClass = loader.loadClass(ClassLoaderTestClass.class.getCanonicalName());
        }

        // we can't cast to ClassLoadedClass because the parent classloader already has the "original" class loaded
        // (casting would result in a ClassCastException)
        final Object classLoadedClass = aClass.getDeclaredConstructor().newInstance();
        final String output = (String) aClass.getDeclaredMethod("get").invoke(classLoadedClass);

        assertEquals("original", output);

        // now let's check that the original class is not affected (we're loading from the parent classloader)
        final ClassLoaderTestClass originalClassloadedClass = new ClassLoaderTestClass();
        assertEquals("original", originalClassloadedClass.get());
    }

    @Test
    public void test_restricted_class_loaded_from_parent_not_found_fallback_to_child() throws Exception {
        final File javaSrcFile = getJavaSrcFileForClassFile(ClassLoadedClass.class);
        final File file = temporaryFolder.newFile(ClassLoadedClass.class.getSimpleName() + ".java");
        FileUtils.copyFile(javaSrcFile, file);

        replaceFileContent(file, "package com.hivemq.extensions.classloader;", "package com.hivemq.extensions.api.test;");
        replaceFileContent(file, "original", "modified");

        // actually compile the file
        OnTheFlyCompilationUtil.compileJavaFile(file, folder);

        final Class<?> aClass;
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(new URL[]{folder.toURI().toURL()}, getClass().getClassLoader())) {
            aClass = loader.loadClass("com.hivemq.extensions.api.test.ClassLoadedClass");
        }

        // the parent classloader should not know this class
        try {
            getClass().getClassLoader().loadClass("com.hivemq.extensions.api.test.ClassLoadedClass");
            fail();
        } catch (final ClassNotFoundException e) {
            //expected, no-op
        }

        // invoke the get() method
        final Object classLoadedClass = aClass.getDeclaredConstructor().newInstance();
        final String output = (String) aClass.getDeclaredMethod("get").invoke(classLoadedClass);

        assertEquals("modified", output);
    }

    @Test
    public void test_static_context_is_always_loaded_from_child() throws Exception {
        final Class<?> servicesClassParent = getClass().getClassLoader().loadClass(Services.class.getCanonicalName());

        final Field servicesFieldParent = servicesClassParent.getDeclaredField("services");
        servicesFieldParent.setAccessible(true);
        final ImmutableMap<String, Object> dependenciesParent = ImmutableMap.of("key", "original");
        servicesFieldParent.set(null, dependenciesParent);

        final URL serviceUrl = Services.class.getResource("Services.class");
        assertNotNull(serviceUrl);
        final String path = serviceUrl.toExternalForm();
        final URL folder = new URL(path.replace(Services.class.getCanonicalName().replace(".", File.separator) + ".class", ""));

        final Class<?> servicesClassIsolated;
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(new URL[]{folder.toURI().toURL()}, getClass().getClassLoader())) {
            servicesClassIsolated = loader.loadClass(Services.class.getCanonicalName());
        }

        final Field servicesFieldIsolated = servicesClassIsolated.getDeclaredField("services");
        servicesFieldIsolated.setAccessible(true);
        final ImmutableMap<String, Object> dependencies = ImmutableMap.of("key", "modified");
        servicesFieldIsolated.set(null, dependencies);

        // now let's check that the original class is not affected
        // (we're loading from the parent classloader and from the isolated classloader)
        //noinspection unchecked
        assertEquals("original", ((Map<String, Object>) servicesFieldParent.get(null)).get("key"));
        //noinspection unchecked
        assertEquals("modified", ((Map<String, Object>) servicesFieldIsolated.get(null)).get("key"));
    }

    @Test
    public void test_get_resource() throws Exception {
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(new URL[]{folder.toURI().toURL()}, getClass().getClassLoader())) {
            final URL resource = loader.getResource("logback-test.xml");
            assertNotNull(resource);
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_get_resource_delegate() throws Exception {
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(getClass().getClassLoader(), null)) {
            final URL resource = loader.getResource("logback-test.xml");
            assertNotNull(resource);
        }
    }

    @Test
    public void test_get_resources() throws Exception {
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(new URL[]{folder.toURI().toURL()}, getClass().getClassLoader())) {
            final Enumeration<URL> resource = loader.getResources("logback-test.xml");
            assertNotNull(resource);
            assertTrue(resource.hasMoreElements());
            assertNotNull(resource.nextElement());
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_get_resources_delegate() throws Exception {
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(getClass().getClassLoader(), null)) {
            final Enumeration<URL> resource = loader.getResources("logback-test.xml");
            assertNotNull(resource);
            assertTrue(resource.hasMoreElements());
            assertNotNull(resource.nextElement());
        }
    }

    @Test
    public void test_get_resources_as_stream() throws Exception {
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(new URL[]{folder.toURI().toURL()}, getClass().getClassLoader())) {
            final InputStream resource = loader.getResourceAsStream("logback-test.xml");
            assertNotNull(resource);
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_get_resources_as_stream_delegate() throws Exception {
        try (final IsolatedExtensionClassloader loader = new IsolatedExtensionClassloader(getClass().getClassLoader(), null)) {
            final InputStream resource = loader.getResourceAsStream("logback-test.xml");
            assertNotNull(resource);
        }
    }

    private void replaceFileContent(
            final @NotNull File file,
            final @NotNull String original,
            final @NotNull String modified) throws Exception {
        String content = FileUtils.readFileToString(file, UTF_8);
        content = content.replaceAll(original, modified);
        FileUtils.writeStringToFile(file, content, UTF_8);
    }

    private File getJavaSrcFileForClassFile(final Class<?> clazz) {
        final File f = new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
        File gradleHivemqParentFolder = f.getParentFile();
        while (!gradleHivemqParentFolder.getAbsolutePath().equals("/") && !gradleHivemqParentFolder.getAbsolutePath().endsWith("out")) {
            gradleHivemqParentFolder = gradleHivemqParentFolder.getParentFile();
        }
        gradleHivemqParentFolder = gradleHivemqParentFolder.getParentFile();
        final File gradleSrcFolder = new File(gradleHivemqParentFolder, "src");
        File gradleTestFolder = new File(gradleSrcFolder, "test");
        if (!gradleTestFolder.exists()) {
            gradleTestFolder = new File(gradleSrcFolder, "core/test");
        }
        final File gradleJavaFolder = new File(gradleTestFolder, "java");

        final File gradleFile = new File(gradleJavaFolder, clazz.getCanonicalName().replace(".", File.separator) + ".java");
        if (gradleFile.exists()) {
            return gradleFile;
        }

        final File hivemqParentFolder = f.getParentFile().getParentFile().getParentFile();
        final File srcFolder = new File(hivemqParentFolder, "src");
        final File testFolder = new File(srcFolder, "test");
        final File javaFolder = new File(testFolder, "java");

        return new File(javaFolder, clazz.getCanonicalName().replace(".", File.separator) + ".java");
    }
}
