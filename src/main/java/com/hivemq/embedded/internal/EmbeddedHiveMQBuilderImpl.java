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

package com.hivemq.embedded.internal;

import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.embedded.EmbeddedHiveMQBuilder;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

/**
 * @author Georg Held
 */
public class EmbeddedHiveMQBuilderImpl implements EmbeddedHiveMQBuilder {

    private @Nullable Path configFolder = null;
    private @Nullable Path dataFolder = null;
    private @Nullable Path extensionsFolder = null;

    @Override
    public @NotNull EmbeddedHiveMQBuilder withConfigurationFolder(final @Nullable Path configFolder) {
        this.configFolder = configFolder;

        return this;
    }

    @Override
    public @NotNull EmbeddedHiveMQBuilder withDataFolder(final @Nullable Path dataFolder) {
        this.dataFolder = dataFolder;

        return this;
    }

    @Override
    public @NotNull EmbeddedHiveMQBuilder withExtensionsFolder(final @Nullable Path extensionsFolder) {
        this.extensionsFolder = extensionsFolder;

        return this;
    }

    @Override
    public @NotNull EmbeddedHiveMQ build() {
        // Shim for the old API
        final File confFile = configFolder == null ? null : configFolder.toFile();
        final File dataFile = dataFolder == null ? null : dataFolder.toFile();
        final File extensionsFile = extensionsFolder == null ? null : extensionsFolder.toFile();

        return new EmbeddedHiveMQImpl(confFile, dataFile, extensionsFile);
    }
}
