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

import com.google.common.collect.ImmutableList;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQPluginEvent;

import java.nio.file.Path;

/**
 * The extension loader is responsible for searching extension implementations in a given folder or in a given Set of
 * Resources. This is most useful if you have a folder with extension folders to read.
 * <p>
 * All extensions loaded with these PluginLoader have their <b>own classloader</b>, so essentially there's an classpath
 * isolation between all extensions.
 *
 * @author Dominik Obermaier
 * @author Georg Held
 * @author Christoph Schäbel
 */
public interface PluginLoader {

    /**
     * Loads extension implementations from a given folder. The folder must exist but may be empty.
     * <p>
     * Note that only valid extension folders are considered, it's not possible to add .class files or other resources
     * from the given folder.
     *
     * @param pluginFolder       the folder to search extension folders from
     * @param permissive         is a not existing extension folder allowed
     * @param desiredPluginClass the desired extension superclass to search implementations for
     * @return a Collection of {@link HiveMQExtension} from the extension folder.
     * @throws java.lang.NullPointerException     if <code>null</code> is passed to any parameter
     * @throws java.lang.IllegalArgumentException If the folder does not exist HiveMQ is not able to read the contents
     *                                            of the folder
     */
    @ReadOnly
    @NotNull <T extends ExtensionMain> ImmutableList<HiveMQPluginEvent> loadPlugins(
            @NotNull final Path pluginFolder,
            boolean permissive,
            @NotNull final Class<T> desiredPluginClass);

    /**
     * Loads a single extension.
     *
     * @param pluginFolder a valid extension folder.
     * @return An Optional of a loaded extension. Empty if loading fails or extension <id> already known.
     */
    @Nullable <T extends ExtensionMain> HiveMQPluginEvent processSinglePluginFolder(
            @NotNull final Path pluginFolder,
            @NotNull final Class<T> desiredClass);
}
