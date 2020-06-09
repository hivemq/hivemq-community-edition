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
package com.hivemq.extensions.packets.general;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.mqtt.message.ProtocolVersion;

/**
 * @author Georg Held
 */
public class MqttVersionUtil {
    @NotNull
    public static MqttVersion toMqttVersion(@NotNull final ProtocolVersion protocolVersion) {
        switch (protocolVersion) {
            case MQTTv3_1:
                return MqttVersion.V_3_1;
            case MQTTv3_1_1:
                return MqttVersion.V_3_1_1;
            case MQTTv5:
                return MqttVersion.V_5;
            default:
                throw new IllegalArgumentException();
        }
    }

    @NotNull
    public static ProtocolVersion toProtocolVersion(@NotNull final MqttVersion mqttVersion) {
        switch (mqttVersion) {
            case V_3_1:
                return ProtocolVersion.MQTTv3_1;
            case V_3_1_1:
                return ProtocolVersion.MQTTv3_1_1;
            case V_5:
                return ProtocolVersion.MQTTv5;
            default:
                throw new IllegalArgumentException();
        }
    }
}
