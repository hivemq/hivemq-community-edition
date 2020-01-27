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

package util;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;

/**
 * @author Silvio Giebl
 */
public class IsolatedPluginClassLoaderUtil {

    public static <T> @NotNull T loadIsolated(
            final @NotNull TemporaryFolder temporaryFolder, final @NotNull Class<T> clazz) throws Exception {

        final IsolatedPluginClassloader isolatedPluginClassloader =
                buildClassLoader(temporaryFolder, new Class[]{clazz});

        return instanceFromClassloader(isolatedPluginClassloader, clazz);
    }

    public static @NotNull IsolatedPluginClassloader buildClassLoader(
            final @NotNull TemporaryFolder temporaryFolder, final @NotNull Class[] classes) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class).addClasses(classes);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        return new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()},
                IsolatedPluginClassLoaderUtil.class.getClassLoader());
    }

    @SuppressWarnings("unchecked")
    public static <T> @NotNull T instanceFromClassloader(
            final @NotNull IsolatedPluginClassloader classLoader, final @NotNull Class<T> clazz) throws Exception {

        final Class<T> isolatedClazz = (Class<T>) classLoader.loadClass(clazz.getName());

        return isolatedClazz.getDeclaredConstructor().newInstance();
    }

}
