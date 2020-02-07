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

package com.hivemq.mqtt.message.mqtt5;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Silvio Giebl
 * @author Lukas Brandl
 */
public class Mqtt5UserPropertiesBuilder {

    private ImmutableList.Builder<MqttUserProperty> listBuilder;

    Mqtt5UserPropertiesBuilder() {
    }

    Mqtt5UserPropertiesBuilder(@NotNull final Mqtt5UserProperties userProperties) {
        listBuilder = ImmutableList.builder();
        listBuilder.addAll(userProperties.asList());
    }

    @NotNull
    public Mqtt5UserPropertiesBuilder add(@NotNull final MqttUserProperty userProperty) {
        if (listBuilder == null) {
            listBuilder = ImmutableList.builder();
        }
        listBuilder.add(userProperty);
        return this;
    }

    @NotNull
    public Mqtt5UserProperties build() {
        return Mqtt5UserProperties.build(listBuilder);
    }

}
