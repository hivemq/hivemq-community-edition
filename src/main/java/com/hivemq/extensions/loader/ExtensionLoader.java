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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensionEvent;

import java.nio.file.Path;

/**
 * The extension loader is responsible for searching extension implementations
 * in a given folder or in a given Set of Resources. This is most useful
 * if you have a folder with extension folders to read.
 * <p>
 * All extensions loaded with these ExtensionLoader have their <b>own classloader</b>,
 * so essentially there's a classpath isolation between all extensions.
 */
public interface ExtensionLoader {

    /**
     * Loads extension implementations from a given folder. The folder must exist but may be empty.
     * <p>
     * Note that only valid extension folders are considered,
     * it's not possible to add .class files or other resources from the given folder.
     *
     * @param extensionFolder       the folder to search extension folders from
     * @param permissive            is a not existing extension folder allowed
     * @return a Collection of {@link HiveMQExtension} from the extension folder.
     * @throws java.lang.NullPointerException     if <code>null</code> is passed to any parameter
     * @throws java.lang.IllegalArgumentException If the folder does not exist HiveMQ is not able to read the contents
     *                                            of the folder
     */
    @ReadOnly
    @NotNull ImmutableCollection<HiveMQExtensionEvent> loadExtensions(final @NotNull Path extensionFolder, boolean permissive);

    /**
     * Loads a single extension.
     *
     * @param extensionFolder a valid extension folder.
     * @return An Optional of a loaded extension. Empty if loading fails or extension <id> already known.
     */
    @Nullable HiveMQExtensionEvent processSingleExtensionFolder(final @NotNull Path extensionFolder);

    @Nullable HiveMQExtensionEvent loadEmbeddedExtension(@NotNull EmbeddedExtension extensionMain);
}
