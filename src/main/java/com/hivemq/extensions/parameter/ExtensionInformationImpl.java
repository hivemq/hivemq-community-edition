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
package com.hivemq.extensions.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extensions.HiveMQExtension;

import java.io.File;
import java.util.Optional;

/**
 * @author Georg Held
 */
public class ExtensionInformationImpl implements ExtensionInformation {

    private final @NotNull HiveMQExtension hiveMQExtension;

    public ExtensionInformationImpl(final @NotNull HiveMQExtension hiveMQExtension) {
        this.hiveMQExtension = hiveMQExtension;
    }

    @Override
    public @NotNull String getId() {
        return this.hiveMQExtension.getId();
    }

    @Override
    public @NotNull String getName() {
        return this.hiveMQExtension.getName();
    }

    @Override
    public @NotNull String getVersion() {
        return this.hiveMQExtension.getVersion();
    }

    @Override
    public @NotNull Optional<String> getAuthor() {
        return Optional.ofNullable(this.hiveMQExtension.getAuthor());
    }

    @Override
    public @NotNull File getExtensionHomeFolder() {
        return this.hiveMQExtension.getExtensionFolderPath().toFile();
    }
}
