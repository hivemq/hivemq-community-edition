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
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;

/**
 * A helper interface for methods to enable copy on write for {@link UserProperties}. of the extension system.
 *
 * @author Georg Held
 */
public interface InternalUserProperties extends UserProperties {

    /**
     * @return a flat copy of the implementing class. Merging all internal state in the process.
     */
    @NotNull
    InternalUserProperties consolidate();

    /**
     * @return all {@link MqttUserProperty}s as an {@link ImmutableList}, can be used for {@link
     * Mqtt5UserProperties#of(ImmutableList)}.
     */
    @NotNull
    ImmutableList<MqttUserProperty> asImmutableList();

    @NotNull
    default Mqtt5UserProperties toMqtt5UserProperties() {
        return Mqtt5UserProperties.of(asImmutableList());
    }
}
