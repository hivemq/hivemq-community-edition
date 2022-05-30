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
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Creates JAR files for a given class on the fly and loads them via the {@link IsolatedExtensionClassloader}.
 *
 * @author Silvio Giebl
 */
public class IsolatedExtensionClassloaderUtil {

    /**
     * Creates a JAR file of the given class in a temporary folder.
     * <p>
     * All subclasses of the given class are explicitly added to the file.
     *
     * @param temporaryFolder the folder to create the JAR file in
     * @param clazz           the class to be compiled into the JAR file
     * @return the created JAR file
     */
    public static @NotNull File createJarFile(final @NotNull TemporaryFolder temporaryFolder,
            final @NotNull Class<?> clazz) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClass(clazz);
        for (final String subclass : getSubclasses(clazz)) {
            javaArchive.addClass(subclass);
        }
        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);
        return jarFile;
    }

    /**
     * Loads the given class from a JAR file.
     * <p>
     * All subclasses of the given class are explicitly loaded as well.
     *
     * @param jarFile the JAR file to be used
     * @param clazz   the class to be loaded
     * @param <T>     the type of the loaded class
     * @return the loaded class
     */
    public static <T> @NotNull Class<T> loadIsolatedClass(final @NotNull File jarFile,
            final @NotNull Class<?> clazz) throws Exception {
        try (final IsolatedExtensionClassloader cl = getIsolatedExtensionClassloader(jarFile)) {
            //noinspection unchecked
            final Class<T> isolatedClass = (Class<T>) cl.loadClass(clazz.getName());
            for (final String subclass : getSubclasses(clazz)) {
                cl.loadClass(subclass);
            }
            return isolatedClass;
        }
    }

    private static @NotNull List<String> getSubclasses(final @NotNull Class<?> clazz) throws Exception {
        final List<String> subClasses = new ArrayList<>();
        final Set<String> subClassNames = ClassPool.getDefault().get(clazz.getName()).getClassFile().getConstPool().getClassNames();
        for (final String subClassName : subClassNames) {
            final String className = subClassName.replaceAll("/", ".");
            if (!className.startsWith("[L")) {
                subClasses.add(className);
            }
        }
        return subClasses;
    }

    /**
     * Creates a JAR file from a given class and returns an instance of that class.
     *
     * @param temporaryFolder the folder to create the JAR file in
     * @param clazz           the class to be compiled into the JAR file
     * @param <T>             the type of the loaded class
     * @return an instance of the given class (using the default constructor)
     */
    public static <T> @NotNull T loadIsolated(final @NotNull TemporaryFolder temporaryFolder,
            final @NotNull Class<T> clazz) throws Exception {
        final IsolatedExtensionClassloader cl = buildClassLoader(temporaryFolder, new Class[]{clazz});
        return instanceFromClassloader(cl, clazz);
    }

    /**
     * Creates a JAR file for multiple classes and returns an {@link IsolatedExtensionClassloader} for this file.
     *
     * @param temporaryFolder the folder to create the JAR file in
     * @param classes         the classes to be compiled into the JAR file
     * @return an {@link IsolatedExtensionClassloader} for the given classes
     */
    public static @NotNull IsolatedExtensionClassloader buildClassLoader(final @NotNull TemporaryFolder temporaryFolder,
            final @NotNull Class<?> @NotNull [] classes) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClasses(classes);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        return getIsolatedExtensionClassloader(jarFile);
    }

    /**
     * Creates an instance of the given class from an {@link IsolatedExtensionClassloader}.
     *
     * @param classLoader the {@link IsolatedExtensionClassloader} to load the class from
     * @param clazz       the class to be loaded
     * @param <T>         the type of the loaded class
     * @return an instance of the given class (using the default constructor)
     */
    public static <T> @NotNull T instanceFromClassloader(final @NotNull IsolatedExtensionClassloader classLoader,
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
    public static @NotNull IsolatedExtensionClassloader getIsolatedExtensionClassloader() {
        return new IsolatedExtensionClassloader(new URL[]{}, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates an {@link IsolatedExtensionClassloader} for the given JAR file.
     *
     * @param jarFile the JAR file to be used
     * @return an instance of {@link IsolatedExtensionClassloader}
     */
    public static @NotNull IsolatedExtensionClassloader getIsolatedExtensionClassloader(final @NotNull File jarFile) throws Exception {
        return new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
    }
}
