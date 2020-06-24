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

import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.OnTheFlyCompilationUtil;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class ClassServiceLoaderTest {

    public static String theInterface = "" +
            " public interface TheInterface {" +
            "   int doSomething();" +
            " }";

    public static String theImpl = "" +
            " public class TheImpl implements TheInterface {" +
            "        public int doSomething() {" +
            "            return 1;}" +
            " }";

    public static String theImpl2 = "" +
            " public class TheImpl2 implements TheInterface {" +
            "        public int doSomething() {" +
            "            return 2;}" +
            " }";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test_load_classes_from_jar_file_with_service_loader() throws Exception {

        /*
             This test tests the actual service loader mechanism with a real JAR file.
             The concrete steps are

             1. Compile the classes on the fly
             2. Create a JAR file with these classes
             3. Load the JAR file and test the service loader mechanism
         */

        //Compile classes on the fly
        final ClassLoader compile = OnTheFlyCompilationUtil.compile(
                new OnTheFlyCompilationUtil.StringJavaFileObject("TheInterface", theInterface),
                new OnTheFlyCompilationUtil.StringJavaFileObject("TheImpl", theImpl));

        //Creating the JAR file with the compiled classes + service loader

        final Class<?> interfaceClass = Class.forName("TheInterface", false, compile);
        final Class<?> implClass = Class.forName("TheImpl", false, compile);


        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addAsServiceProviderAndClasses(interfaceClass, implClass);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);


        final ClassServiceLoader classServiceLoader = new ClassServiceLoader();
        //This classloader contains the classes from the jar file
        final URLClassLoader cl = new URLClassLoader(new URL[]{jarFile.toURI().toURL()});

        final Iterable<? extends Class<?>> loadedClasses = classServiceLoader.load(Class.forName("TheInterface", true, cl), cl);

        assertEquals(1, Iterables.size(loadedClasses));
        //Although they have the same canonical name, they are not equal because they come from different classloaders
        assertEquals(implClass.getCanonicalName(), loadedClasses.iterator().next().getCanonicalName());
    }

    @Test
    public void test_load_classes_from_jar_file_with_service_loader_empty_services_file() throws Exception {

        //Compile classes on the fly
        final ClassLoader compile = OnTheFlyCompilationUtil.compile(new OnTheFlyCompilationUtil.StringJavaFileObject("TheInterface", theInterface));

        //Creating the JAR file with the compiled classes + service loader

        final Class<?> interfaceClass = Class.forName("TheInterface", false, compile);

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addAsServiceProviderAndClasses(interfaceClass);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);


        final ClassServiceLoader classServiceLoader = new ClassServiceLoader();
        //This classloader contains the classes from the jar file
        final URLClassLoader cl = new URLClassLoader(new URL[]{jarFile.toURI().toURL()});

        final Iterable<? extends Class<?>> loadedClasses = classServiceLoader.load(Class.forName("TheInterface", true, cl), cl);

        assertEquals(0, Iterables.size(loadedClasses));
    }

    @Test
    public void test_load_classes_from_jar_file_with_service_loader_multiple_classes() throws Exception {

        //Compile classes on the fly
        final ClassLoader compile = OnTheFlyCompilationUtil.compile(
                new OnTheFlyCompilationUtil.StringJavaFileObject("TheInterface", theInterface),
                new OnTheFlyCompilationUtil.StringJavaFileObject("TheImpl", theImpl),
                new OnTheFlyCompilationUtil.StringJavaFileObject("TheImpl2", theImpl2));

        //Creating the JAR file with the compiled classes + service loader

        final Class<?> interfaceClass = Class.forName("TheInterface", false, compile);
        final Class<?> implClass = Class.forName("TheImpl", false, compile);
        final Class<?> impl2Class = Class.forName("TheImpl2", false, compile);


        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsServiceProviderAndClasses(interfaceClass, implClass, impl2Class);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);


        final ClassServiceLoader classServiceLoader = new ClassServiceLoader();
        //This classloader contains the classes from the jar file
        final URLClassLoader cl = new URLClassLoader(new URL[]{jarFile.toURI().toURL()});

        final Iterable<? extends Class<?>> loadedClasses = classServiceLoader.load(Class.forName("TheInterface", true, cl), cl);

        assertEquals(2, Iterables.size(loadedClasses));
    }

    @Test
    public void test_load_classes_from_jar_file_with_service_loader_with_comments() throws Exception {

        //Compile classes on the fly
        final ClassLoader compile = OnTheFlyCompilationUtil.compile(
                new OnTheFlyCompilationUtil.StringJavaFileObject("TheInterface", theInterface),
                new OnTheFlyCompilationUtil.StringJavaFileObject("TheImpl", theImpl),
                new OnTheFlyCompilationUtil.StringJavaFileObject("TheImpl2", theImpl2));

        //Creating the JAR file with the compiled classes + service loader

        final Class<?> interfaceClass = Class.forName("TheInterface", false, compile);
        final Class<?> implClass = Class.forName("TheImpl", false, compile);
        final Class<?> impl2Class = Class.forName("TheImpl2", false, compile);

        final String fileContents = "#" + implClass.getCanonicalName() + "\n" +
                impl2Class.getCanonicalName() + " # Comment";
        final File servicesDescriptionFile = temporaryFolder.newFile();
        Files.write(fileContents, servicesDescriptionFile, StandardCharsets.UTF_8);

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).
                addAsResource(servicesDescriptionFile, "META-INF/services/" + interfaceClass.getCanonicalName()).
                addClasses(interfaceClass, implClass, impl2Class);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);


        final ClassServiceLoader classServiceLoader = new ClassServiceLoader();
        final URLClassLoader cl = new URLClassLoader(new URL[]{jarFile.toURI().toURL()});

        final Iterable<? extends Class<?>> loadedClasses = classServiceLoader.load(Class.forName("TheInterface", true, cl), cl);

        assertEquals(1, Iterables.size(loadedClasses));
        assertEquals(impl2Class.getCanonicalName(), loadedClasses.iterator().next().getCanonicalName());
    }

    @Test(expected = NullPointerException.class)
    public void test_class_to_load_null() throws Exception {
        final ClassServiceLoader loader = new ClassServiceLoader();
        loader.load(null, ClassLoader.getSystemClassLoader());
    }

    @Test(expected = NullPointerException.class)
    public void test_classloader_null() throws Exception {
        final ClassServiceLoader loader = new ClassServiceLoader();
        loader.load(Object.class, null);
    }
}