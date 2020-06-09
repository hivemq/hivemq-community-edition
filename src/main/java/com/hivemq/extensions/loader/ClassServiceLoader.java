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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.annotations.ReadOnly;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A Service loader implementation which is similar to the JDK default {@link java.util.ServiceLoader}.
 * <p>
 * The main difference is, that this extension service loader does not create instances of the classes found but will
 * return the implementation class.
 * <p>
 * All classes which are loaded by this service loader are initialized.
 *
 * @author Dominik Obermaier
 */
@Singleton
public class ClassServiceLoader {


    private static final String META_INF_SERVICES = "META-INF/services/";

    /**
     * Loads all classes defined in the <code>META-INF/services/CLASS_TO_LOAD</code> file. The classes
     * get initialized when loading. Comments in the file (prefixed with a # character) are ignored.
     *
     * @param classToLoad the class to load. Typically this is an interface.
     * @param classLoader the classloader to load the classes from
     * @param <S>         The type of the class to load
     * @return an immutable Iterable which contains all classes found via the service loader mechanism. All classes get
     * initialized.
     * @throws IOException                    If an IO error happens
     * @throws ClassNotFoundException         If the found class can not be found and thus can't be initialized and
     *                                        loaded
     * @throws java.lang.NullPointerException If <code>null</code> is passed to any parameter
     */
    @ReadOnly
    @NotNull
    public <S> Iterable<Class<? extends S>> load(@NotNull final Class<S> classToLoad, @NotNull final ClassLoader classLoader) throws IOException, ClassNotFoundException {

        checkNotNull(classToLoad, "Class to load mus not be null");
        checkNotNull(classLoader, "Classloader must not be null");

        final ImmutableList.Builder<Class<? extends S>> services = ImmutableList.builder();

        final Enumeration<URL> urls = classLoader.getResources(META_INF_SERVICES + classToLoad.getName());

        while (urls.hasMoreElements()) {

            final URL url = urls.nextElement();

            try (final InputStream is = url.openStream();
                 final InputStreamReader isr = new InputStreamReader(is, UTF_8);
                 final BufferedReader r = new BufferedReader((isr))) {

                // Read until the stream is empty
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    line = stripComments(line);
                    final String name = line.trim();
                    if (!(name.isEmpty())) {

                        final Class<?> clazz = Class.forName(name, true, classLoader);
                        services.add(clazz.asSubclass(classToLoad));
                    }
                }
            }
        }
        return services.build();
    }

    @NotNull
    private String stripComments(@NotNull final String line) {
        String tempLine = line;
        final int comment = line.indexOf('#');
        if (comment >= 0) {
            tempLine = line.substring(0, comment);
        }
        return tempLine;
    }

}
