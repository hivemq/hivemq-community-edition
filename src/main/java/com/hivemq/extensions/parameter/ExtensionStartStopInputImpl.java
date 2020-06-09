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

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extensions.HiveMQExtension;

import java.util.Map;
import java.util.Optional;

/**
 * @author Georg Held
 */
public class ExtensionStartStopInputImpl implements ExtensionStartInput, ExtensionStopInput {

    private final @NotNull HiveMQExtension extension;
    private final @NotNull Map<String, HiveMQExtension> enabledExtensions;
    private final @NotNull ServerInformation serverInformation;

    public ExtensionStartStopInputImpl(
            final @NotNull HiveMQExtension extension,
            final @NotNull Map<String, HiveMQExtension> enabledExtensions,
            final @NotNull ServerInformation serverInformation) {

        this.extension = extension;
        this.enabledExtensions = enabledExtensions;
        this.serverInformation = serverInformation;
    }

    @Override
    public @NotNull ExtensionInformation getExtensionInformation() {
        return new ExtensionInformationImpl(extension);
    }

    @Override
    public @NotNull ServerInformation getServerInformation() {
        return serverInformation;
    }

    @Override
    public @NotNull Map<String, @NotNull ExtensionInformation> getEnabledExtensions() {
        return enabledExtensions.entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, e -> new ExtensionInformationImpl(e.getValue())));
    }

    @Override
    public @NotNull Optional<String> getPreviousVersion() {
        return Optional.ofNullable(extension.getPreviousVersion());
    }
}
