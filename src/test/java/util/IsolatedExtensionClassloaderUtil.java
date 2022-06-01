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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import javassist.ClassPool;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates JAR files for a given class on the fly and loads them via the {@link IsolatedExtensionClassloader}.
 */
public class IsolatedExtensionClassloaderUtil {

    /**
     * Loads the given class from a JAR file.
     * <p>
     * All referenced classes of the given class are explicitly loaded as well.
     *
     * @param temporaryFolder the folder to create the JAR file in
     * @param mainClass       the class to be loaded
     * @param <T>             the type of the loaded class
     * @return the loaded class
     */
    public static <T> @NotNull Class<T> loadClass(final @NotNull Path temporaryFolder,
            final @NotNull Class<T> mainClass) throws Exception {
        final String mainClassName = mainClass.getName();

        // get referenced classes from same package
        final Set<String> referencedClasses = new HashSet<>();
        for (final String referencedClassName : ClassPool.getDefault().get(mainClassName).getClassFile().getConstPool().getClassNames()) {
            final String className = referencedClassName.replace("/", ".");
            if (!className.equals(mainClassName) && className.startsWith(mainClass.getPackageName())) {
                referencedClasses.add(className);
            }
        }

        // create JAR file
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClass(mainClass);
        for (final String clazz : referencedClasses) {
            javaArchive.addClass(clazz);
        }
        final File jarFile = createJarFile(temporaryFolder, mainClass, javaArchive);

        try (final IsolatedExtensionClassloader cl = buildClassLoader(new URL[]{jarFile.toURI().toURL()})) {
            for (final String clazz : referencedClasses) {
                cl.loadClass(clazz);
            }
            //noinspection unchecked
            return (Class<T>) cl.loadClass(mainClassName);
        }
    }

    /**
     * Creates a JAR file from a given class and returns an instance of that class.
     *
     * @param temporaryFolder the folder to create the JAR file in
     * @param clazz           the class to be compiled into the JAR file
     * @param <T>             the type of the loaded class
     * @return an instance of the given class (using the default constructor)
     */
    public static <T> @NotNull T loadInstance(final @NotNull Path temporaryFolder,
            final @NotNull Class<T> clazz) throws Exception {
        try (final IsolatedExtensionClassloader cl = buildClassLoader(temporaryFolder, new Class[]{clazz})) {
            return loadInstance(cl, clazz);
        }
    }

    /**
     * Loads an instance of the given class from an {@link IsolatedExtensionClassloader}.
     *
     * @param classLoader the {@link IsolatedExtensionClassloader} to load the class from
     * @param clazz       the class to be loaded
     * @param <T>         the type of the loaded class
     * @return an instance of the given class (using the default constructor)
     */
    public static <T> @NotNull T loadInstance(final @NotNull IsolatedExtensionClassloader classLoader,
            final @NotNull Class<T> clazz) throws Exception {
        //noinspection unchecked
        final Class<T> isolatedClazz = (Class<T>) classLoader.loadClass(clazz.getName());
        return isolatedClazz.getDeclaredConstructor().newInstance();
    }

    /**
     * Creates an empty {@link IsolatedExtensionClassloader} with the default parameters.
     *
     * @return a default instance of {@link IsolatedExtensionClassloader}
     */
    public static @NotNull IsolatedExtensionClassloader buildClassLoader() {
        return buildClassLoader(new URL[]{});
    }

    /**
     * Creates a JAR file for multiple classes and returns an {@link IsolatedExtensionClassloader} for this file.
     *
     * @param temporaryFolder the folder to create the JAR file in
     * @param classes         the classes to be compiled into the JAR file
     * @return an {@link IsolatedExtensionClassloader} for the given classes
     */
    public static @NotNull IsolatedExtensionClassloader buildClassLoader(final @NotNull Path temporaryFolder,
            final @NotNull Class<?> @NotNull [] classes) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClasses(classes);
        final File jarFile = createJarFile(temporaryFolder, classes[0], javaArchive);
        return buildClassLoader(new URL[]{jarFile.toURI().toURL()});
    }

    private static @NotNull IsolatedExtensionClassloader buildClassLoader(final @NotNull URL @NotNull [] classpath) {
        return new IsolatedExtensionClassloader(classpath, Thread.currentThread().getContextClassLoader());
    }

    private static @NotNull File createJarFile(final @NotNull Path temporaryFolder, final @NotNull Class<?> mainClass, final JavaArchive javaArchive) {
        // just in case someone uses this with an inner class like TestClass$1
        final String jarFileName = mainClass.getSimpleName().replace("$", "_") + ".jar";
        final File jarFile = temporaryFolder.resolve(jarFileName).toFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);
        return jarFile;
    }
}
