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

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extensions.exception.ExtensionLoadingException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class ExtensionStaticInitializerImpl implements ExtensionStaticInitializer {

    private static final String SERVICES_CLASS = Services.class.getCanonicalName();
    private static final String BUILDERS_CLASS = Builders.class.getCanonicalName();

    @NotNull
    private final ExtensionServicesDependencies servicesDependencies;

    @NotNull
    private final ExtensionBuilderDependencies builderDependencies;

    @Inject
    public ExtensionStaticInitializerImpl(@NotNull final ExtensionServicesDependencies servicesDependencies,
                                          @NotNull final ExtensionBuilderDependencies builderDependencies) {
        this.servicesDependencies = servicesDependencies;
        this.builderDependencies = builderDependencies;
    }

    public void initialize(@NotNull final String pluginId, @NotNull final ClassLoader classLoader) throws ExtensionLoadingException {
        checkNotNull(pluginId, "extension id must not be null");
        checkNotNull(classLoader, "classLoader must not be null");

        initializeServices(pluginId, classLoader);
        initializeBuilders(pluginId, classLoader);
    }

    private void initializeServices(@NotNull final String pluginId,
                                    @NotNull final ClassLoader classLoader) throws ExtensionLoadingException {

        try {

            final Class<?> servicesClass = classLoader.loadClass(SERVICES_CLASS);
            final Field servicesField = servicesClass.getDeclaredField("services");
            servicesField.setAccessible(true);
            final ImmutableMap<String, Object> dependencies = servicesDependencies.getDependenciesMap(classLoader);
            servicesField.set(null, dependencies);

        } catch (final Exception e) {
            throw new ExtensionLoadingException("Not able to initialize Services for extension with id " + pluginId, e);
        }

    }

    private void initializeBuilders(@NotNull final String pluginId,
                                    @NotNull final ClassLoader classLoader) throws ExtensionLoadingException {

        try {

            final Class<?> buildersClass = classLoader.loadClass(BUILDERS_CLASS);
            final Field buildersField = buildersClass.getDeclaredField("builders");
            buildersField.setAccessible(true);
            final ImmutableMap<String, Supplier<Object>> dependencies = builderDependencies.getDependenciesMap();
            buildersField.set(null, dependencies);

        } catch (final Exception e) {
            throw new ExtensionLoadingException("Not able to initialize Builders for extension with id " + pluginId, e);
        }

    }


}
