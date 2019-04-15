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

package com.hivemq.extensions.parameters.start;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.parameters.ExtensionInformationImpl;

import java.util.Map;
import java.util.Optional;

/**
 * @author Georg Held
 */
public class ExtensionStartStopInputImpl implements ExtensionStartInput, ExtensionStopInput {
    private final HiveMQExtension hiveMQExtension;
    private final Map<String, HiveMQExtension> enabledHiveMQPlugins;

    public ExtensionStartStopInputImpl(@NotNull final HiveMQExtension hiveMQExtension, @NotNull final Map<String, HiveMQExtension> enabledHiveMQPlugins) {
        this.hiveMQExtension = hiveMQExtension;
        this.enabledHiveMQPlugins = enabledHiveMQPlugins;
    }

    @Override
    @NotNull
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformationImpl(this.hiveMQExtension);
    }

    @Override
    @NotNull
    public Map<String, ExtensionInformation> getEnabledExtensions() {
        return this.enabledHiveMQPlugins.entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, e -> new ExtensionInformationImpl(e.getValue())));
    }

    @Override
    @NotNull
    public Optional<String> getPreviousVersion() {
        return Optional.ofNullable(hiveMQExtension.getPreviousVersion());
    }
}
