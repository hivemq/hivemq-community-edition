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

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class UserPropertyImpl implements UserProperty {

    @NotNull
    private final String name;

    @NotNull
    private final String value;

    public UserPropertyImpl(@NotNull final String name, @NotNull final String value) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        this.name = name;
        this.value = value;
    }

    @NotNull
    public static UserPropertyImpl convert(@NotNull final MqttUserProperty mqttUserProperty) {
        Preconditions.checkNotNull(mqttUserProperty, "property must not be null");
        return new UserPropertyImpl(mqttUserProperty.getName(), mqttUserProperty.getValue());
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getValue() {
        return value;
    }
}
