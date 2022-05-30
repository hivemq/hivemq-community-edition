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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * A classloader which is meant to be used for the isolated HiveMQ extensions. This is a parent-last
 * classloader which means, classes which are available locally have priority over classes in the parent classloaders.
 * <p>
 * It's not possible to access classes from a sibling IsolatedPluginClassloader because the parent-last approach gives
 * us some kind of (soft) isolation.
 */
public class IsolatedExtensionClassloader extends URLClassLoader {

    private static final Logger log = LoggerFactory.getLogger(IsolatedExtensionClassloader.class);

    @VisibleForTesting
    private static final ImmutableSet<String> restrictedPackages = new ImmutableSet.Builder<String>().add(
            // JDK
            "java.",
            "javax.annotation",
            "jdk.",

            // HiveMQ
            "com.hivemq.extension.sdk.api",

            // HiveMQ dependencies
            "org.slf4j",
            "com.codahale.metrics"
    ).build();

    private static final ImmutableSet<Class<?>> classesWithStaticContext =
            new ImmutableSet.Builder<Class<?>>().add(Services.class, Builders.class).build();

    private static final ImmutableSet<String> classNamesWithStaticContext =
            new ImmutableSet.Builder<String>().add(Services.class.getCanonicalName(), Builders.class.getCanonicalName())
                    .build();

    private final @Nullable ClassLoader delegate;

    public IsolatedExtensionClassloader(@NotNull final URL @NotNull [] classpath, @NotNull final ClassLoader parent) {
        super(classpath, parent);
        this.delegate = null;
    }

    public IsolatedExtensionClassloader(@NotNull final ClassLoader delegate, @NotNull final ClassLoader parent) {
        super(new URL[]{}, parent);
        this.delegate = delegate;
    }

    public void loadClassesWithStaticContext() {
        for (final Class<?> staticClass : classesWithStaticContext) {
            try (final InputStream resourceAsStream = staticClass.getResourceAsStream(
                    staticClass.getSimpleName() + ".class")) {
                if (resourceAsStream != null) {
                    final byte[] bytes = resourceAsStream.readAllBytes();
                    defineClass(staticClass.getCanonicalName(), bytes, 0, bytes.length);
                }
            } catch (final IOException e) {
                log.error("Not able to load extension class files for classes with static context", e);
            }
        }
    }

    @Override
    protected synchronized @NotNull Class<?> loadClass(@NotNull final String name, final boolean resolve)
            throws ClassNotFoundException {
        if (delegate != null) {
            return delegate.loadClass(name);
        }

        // first, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        // load static accessors only from child classloader
        if (classNamesWithStaticContext.contains(name)) {
            // don't catch this ClassNotFoundException, we want it to be propagated
            c = findClass(name);
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }

        if (mustLoadFromParentClassloader(name)) {
            try {
                // try the parent first
                c = super.loadClass(name, resolve);
            } catch (final ClassNotFoundException e) {
                // checking local
                c = findClass(name);
            }
        } else {
            try {
                // checking local
                c = findClass(name);
            } catch (final ClassNotFoundException e) {
                // checking parent (this call to loadClass may eventually call findClass again, in case the parent doesn't find anything)
                c = super.loadClass(name, resolve);
            }
        }

        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    private boolean mustLoadFromParentClassloader(@NotNull final String name) {
        for (final String packageName : restrictedPackages) {
            if (name.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable URL getResource(@NotNull final String name) {
        URL url;
        if (delegate != null) {
            url = delegate.getResource(name);
        } else {
            url = findResource(name);
        }
        if (url == null) {
            // this call to getResource may eventually call findResource again, in case the parent doesn't find anything
            url = super.getResource(name);
        }
        return url;
    }

    @Override
    public @NotNull Enumeration<URL> getResources(@NotNull final String name) throws IOException {
        // similar to super, but local resources are enumerated before parent resources
        final Enumeration<URL> localUrls;
        if (delegate != null) {
            localUrls = delegate.getResources(name);
        } else {
            localUrls = findResources(name);
        }
        Enumeration<URL> parentUrls = null;
        if (getParent() != null) {
            parentUrls = getParent().getResources(name);
        }

        final List<URL> urls = new ArrayList<>();
        if (localUrls != null) {
            while (localUrls.hasMoreElements()) {
                urls.add(localUrls.nextElement());
            }
        }
        if (parentUrls != null) {
            while (parentUrls.hasMoreElements()) {
                urls.add(parentUrls.nextElement());
            }
        }
        return new Enumeration<>() {
            final Iterator<URL> iterator = urls.iterator();

            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @NotNull
            public URL nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    @Nullable
    public InputStream getResourceAsStream(@NotNull final String name) {
        final URL url = getResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (final IOException e) {
            return null;
        }
    }
}
