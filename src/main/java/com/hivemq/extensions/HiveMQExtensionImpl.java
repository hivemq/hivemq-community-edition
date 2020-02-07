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

package com.hivemq.extensions;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.util.Checkpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Georg Held
 * @author Christoph Schäbel
 * @author Silvio Giebl
 */
public class HiveMQExtensionImpl implements HiveMQExtension {

    private static final Logger log = LoggerFactory.getLogger(HiveMQExtensionImpl.class);

    private final @NotNull String id;
    private final @NotNull String version;
    private final @NotNull String name;
    private final @Nullable String author;
    private final @NotNull Path pluginFolderPath;
    private final int priority;
    private final @NotNull AtomicBoolean enabled;
    private @Nullable ExtensionMain extensionMain;
    private @Nullable String previousVersion;

    public HiveMQExtensionImpl(
            final @NotNull HiveMQPluginEntity pluginEntity, final @NotNull Path pluginFolderPath,
            final @NotNull ExtensionMain extensionMain, final boolean enabled) {

        this.id = pluginEntity.getId();
        this.version = pluginEntity.getVersion();
        this.name = pluginEntity.getName();
        this.author = pluginEntity.getAuthor();
        this.pluginFolderPath = pluginFolderPath;
        this.priority = pluginEntity.getPriority();
        this.enabled = new AtomicBoolean(enabled);
        this.extensionMain = extensionMain;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @Nullable String getAuthor() {
        return author;
    }

    @Override
    public @NotNull Path getPluginFolderPath() {
        return this.pluginFolderPath;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public @Nullable Class<? extends ExtensionMain> getPluginMainClazz() {
        return extensionMain != null ? extensionMain.getClass() : null;
    }

    @Override
    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
    public void setDisabled() {
        enabled.set(false);
    }

    @Override
    public @Nullable IsolatedPluginClassloader getPluginClassloader() {
        return extensionMain != null ? (IsolatedPluginClassloader) extensionMain.getClass().getClassLoader() : null;
    }

    @Override
    public @Nullable String getPreviousVersion() {
        return this.previousVersion;
    }

    @Override
    public void setPreviousVersion(@Nullable final String previousVersion) {
        this.previousVersion = previousVersion;
    }

    @Override
    public void start(
            final @NotNull ExtensionStartInput extensionStartInput, final @NotNull ExtensionStartOutput extensionStartOutput) {

        if (extensionMain != null) {
            extensionMain.extensionStart(extensionStartInput, extensionStartOutput);
        }
    }

    @Override
    public void stop(final @NotNull ExtensionStopInput extensionStopInput, final @NotNull ExtensionStopOutput extensionStopOutput) {
        if (extensionMain != null) {
            extensionMain.extensionStop(extensionStopInput, extensionStopOutput);
        }
    }

    @Override
    public void clean(final boolean disable) {
        extensionMain = null;

        if (disable) {
            final boolean disabled;
            try {
                if (pluginFolderPath.toFile().exists()) {
                    disabled = PluginUtil.disablePluginFolder(pluginFolderPath);
                } else {
                    log.trace(
                            "Extension folder {} was already removed and cannot be disabled, continuing normally",
                            pluginFolderPath.toFile().getAbsolutePath());
                    disabled = true;
                }
                if (!disabled) {
                    log.warn("Could not disable extension folder {}.", pluginFolderPath);
                }
            } catch (final IOException e) {
                log.warn("Could not disable extension folder {}, reason {}", pluginFolderPath, e.getMessage());
                log.trace("Original exception", e);
            }
            Checkpoints.checkpoint("extension-disabled");
        }
    }

}
