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

package com.hivemq.mqtt.message.disconnect;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 *
 * @since 1.4
 */
@Immutable
public class DISCONNECT extends MqttMessageWithUserProperties.MqttMessageWithReasonCode<Mqtt5DisconnectReasonCode> implements Mqtt3DISCONNECT, Mqtt5DISCONNECT{

    private final String serverReference;
    private final long sessionExpiryInterval;

    //MQTT 3
    public DISCONNECT() {
        super(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        sessionExpiryInterval = SESSION_EXPIRY_NOT_SET;
        serverReference = null;
    }

    public DISCONNECT(@NotNull final Mqtt5DisconnectReasonCode reasonCode,
                      @Nullable final String reasonString,
                      @NotNull final Mqtt5UserProperties userProperties,
                      @Nullable final String serverReference,
                      final long sessionExpiryInterval) {
        super(reasonCode, reasonString, userProperties);
        this.serverReference = serverReference;
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    @Override
    public String getServerReference() {
        return serverReference;
    }

    @NotNull
    @Override
    public MessageType getType() {
        return MessageType.DISCONNECT;
    }
}
