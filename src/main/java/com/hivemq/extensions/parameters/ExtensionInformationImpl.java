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

package com.hivemq.extensions.parameters;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extensions.HiveMQExtension;

import java.io.File;
import java.util.Optional;

/**
 * @author Georg Held
 */
public class ExtensionInformationImpl implements ExtensionInformation {

    @NotNull
    private final HiveMQExtension hiveMQExtension;

    public ExtensionInformationImpl(@NotNull final HiveMQExtension hiveMQExtension) {
        this.hiveMQExtension = hiveMQExtension;
    }

    @NotNull
    @Override
    public String getId() {
        return this.hiveMQExtension.getId();
    }

    @NotNull
    @Override
    public String getName() {
        return this.hiveMQExtension.getName();
    }

    @NotNull
    @Override
    public String getVersion() {
        return this.hiveMQExtension.getVersion();
    }

    @NotNull
    @Override
    public Optional<String> getAuthor() {
        return Optional.ofNullable(this.hiveMQExtension.getAuthor());
    }

    @NotNull
    @Override
    public File getExtensionHomeFolder() {
        return this.hiveMQExtension.getPluginFolderPath().toFile();
    }
}
