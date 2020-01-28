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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 * @since 1.4
 */
@Immutable
public class DISCONNECT extends MqttMessageWithUserProperties.MqttMessageWithReasonCode<Mqtt5DisconnectReasonCode>
        implements Mqtt3DISCONNECT, Mqtt5DISCONNECT {

    private final @Nullable String serverReference;
    private final long sessionExpiryInterval;

    //MQTT 3
    public DISCONNECT() {
        super(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        sessionExpiryInterval = SESSION_EXPIRY_NOT_SET;
        serverReference = null;
    }

    public DISCONNECT(
            final @NotNull Mqtt5DisconnectReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final @Nullable String serverReference,
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
    public @Nullable String getServerReference() {
        return serverReference;
    }

    @Override
    public @NotNull MessageType getType() {
        return MessageType.DISCONNECT;
    }

    public static @NotNull DISCONNECT createDisconnectFrom(final @NotNull DisconnectPacket packet) {
        final Mqtt5DisconnectReasonCode reasonCode = Mqtt5DisconnectReasonCode.from(packet.getReasonCode());
        final String reasonString = packet.getReasonString().orElse(null);
        final String serverReference = packet.getServerReference().orElse(null);
        final long sessionExpiryInterval = packet.getSessionExpiryInterval().orElse(SESSION_EXPIRY_NOT_SET);
        final ImmutableList.Builder<MqttUserProperty> userPropertyBuilder = ImmutableList.builder();
        for (final UserProperty userProperty : packet.getUserProperties().asList()) {
            userPropertyBuilder.add(new MqttUserProperty(userProperty.getName(), userProperty.getValue()));
        }
        final Mqtt5UserProperties mqtt5UserProperties = Mqtt5UserProperties.of(userPropertyBuilder.build());
        return new DISCONNECT(reasonCode, reasonString, mqtt5UserProperties, serverReference, sessionExpiryInterval);
    }
}
