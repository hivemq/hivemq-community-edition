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

package com.hivemq.extensions;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
public class HiveMQEmbeddedExtensionImpl extends AbstractHiveMQExtension {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HiveMQEmbeddedExtensionImpl.class);

    private final @Nullable ClassLoader classLoader;

    public HiveMQEmbeddedExtensionImpl(
            final @NotNull String id,
            final @NotNull String version,
            final @NotNull String name,
            final @Nullable String author,
            final int priority,
            final int startPriority,
            final @NotNull ExtensionMain extensionMain,
            final boolean enabled) {
        super(id, version, name, author, priority,
                startPriority, extensionMain, enabled,
                new File(System.getProperty("java.io.tmpdir")).toPath());

        classLoader = extensionMain.getClass().getClassLoader();
    }

    @Override
    public @Nullable ClassLoader getExtensionClassloader() {
        return classLoader;
    }

    @Override
    public @NotNull Logger getLogger() {
        return log;
    }

    @Override
    public boolean isEmbedded() {
        return true;
    }
}
