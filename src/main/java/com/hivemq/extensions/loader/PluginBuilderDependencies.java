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

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;

import java.util.function.Supplier;

/**
 * Defines all dependencies which are accessible via {@link Builders} in an extension
 *
 * @author Christoph Schäbel
 */
public interface PluginBuilderDependencies {

    /**
     * @param classLoader the {@link IsolatedPluginClassloader} for this extension
     * @return a {@link ImmutableMap} which contains all dependencies which are accessible
     * via {@link Builders} in an extension
     */
    @NotNull
    ImmutableMap<String, Supplier<Object>> getDependenciesMap(@NotNull IsolatedPluginClassloader classLoader);

}
