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
package com.hivemq.configuration.reader;

import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.io.File;
import java.util.Optional;

/**
 * A wrapper class for the configuration file used
 * for initial configuration of HiveMQ on startup
 */
public class ConfigurationFile {

    private final Optional<File> configurationFile;

    public ConfigurationFile(@Nullable final File configurationFile) {
        this.configurationFile = Optional.ofNullable(configurationFile);
    }

    public Optional<File> file() {
        return configurationFile;
    }
}
