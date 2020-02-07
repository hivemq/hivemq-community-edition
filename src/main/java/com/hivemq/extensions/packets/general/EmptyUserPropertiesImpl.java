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

package com.hivemq.extensions.packets.general;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Georg Held
 */
public class EmptyUserPropertiesImpl implements UserProperties, InternalUserProperties {

    public static final EmptyUserPropertiesImpl INSTANCE = new EmptyUserPropertiesImpl();

    @NotNull
    @Override
    public Optional<String> getFirst(@NotNull final String key) {
        return Optional.empty();
    }

    @NotNull
    @Override
    public List<String> getAllForName(@NotNull final String key) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<UserProperty> asList() {
        return Collections.emptyList();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @NotNull
    @Override
    public InternalUserProperties consolidate() {
        return this;
    }

    @Override
    public @NotNull ImmutableList<MqttUserProperty> asImmutableList() {
        return ImmutableList.of();
    }
}
