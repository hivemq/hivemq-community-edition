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
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class UserPropertiesImpl implements UserProperties, InternalUserProperties {

    @NotNull
    private final Mqtt5UserProperties userProperties;

    /**
     * Empty collection of User Properties.
     */
    public static final UserProperties NO_USER_PROPERTIES = new UserPropertiesImpl(Mqtt5UserProperties.NO_USER_PROPERTIES);

    public UserPropertiesImpl(@NotNull final Mqtt5UserProperties mqtt5UserProperties) {
        this.userProperties = mqtt5UserProperties;
    }

    @NotNull
    @Override
    public Optional<String> getFirst(@NotNull final String name) {
        return userProperties.asList().stream()
                .filter(userProperty -> userProperty.getName().equals(name))
                .findFirst()
                .map(MqttUserProperty::getValue);
    }

    @NotNull
    @Override
    public List<String> getAllForName(@NotNull final String name) {
        return userProperties.asList().stream()
                .filter(userProperty -> userProperty.getName().equals(name))
                .map(MqttUserProperty::getValue)
                .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public List<UserProperty> asList() {
        return userProperties.asList()
                .stream()
                .map((Function<MqttUserProperty, UserProperty>) mqttUserProperty -> UserPropertyImpl.convert(mqttUserProperty))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean isEmpty() {
        return userProperties.asList().isEmpty();
    }

    @Override
    public @NotNull InternalUserProperties consolidate() {
        return this;
    }

    @Override
    public @NotNull ImmutableList<MqttUserProperty> asImmutableList() {
        return userProperties.asList();
    }
}
