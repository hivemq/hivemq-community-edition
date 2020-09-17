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
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * @author Georg Held
 * @author Christoph Schäbel
 * @author Silvio Giebl
 */
public class HiveMQExtensionImpl extends AbstractHiveMQExtension {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HiveMQExtensionImpl.class);

    public HiveMQExtensionImpl(
            final @NotNull HiveMQExtensionEntity pluginEntity, final @NotNull Path pluginFolderPath,
            final @NotNull ExtensionMain extensionMain, final boolean enabled) {
        super(pluginEntity.getId(), pluginEntity.getVersion(), pluginEntity.getName(),
                pluginEntity.getAuthor(), pluginEntity.getPriority(),
                pluginEntity.getStartPriority(), extensionMain, enabled,
                pluginFolderPath);
    }

    @Override
    public @Nullable IsolatedExtensionClassloader getExtensionClassloader() {
        return extensionMain != null ? (IsolatedExtensionClassloader) extensionMain.getClass().getClassLoader() : null;
    }

    @Override
    public @NotNull Logger getLogger() {
        return log;
    }

    @Override
    public boolean isEmbedded() {
        return false;
    }
}
