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

package com.hivemq.mqtt.message.puback;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;

/**
 * The MQTT PUBACK message
 *
 * @author Dominik Obermaier
 * @author Waldemar Ruck
 * @since 1.4
 */
public class PUBACK extends MqttMessageWithUserProperties.MqttMessageWithIdAndReasonCode<Mqtt5PubAckReasonCode>
        implements Mqtt3PUBACK, Mqtt5PUBACK {

    //MQTT 3
    public PUBACK(final int packetIdentifier) {
        super(packetIdentifier, Mqtt5PubAckReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    //MQTT 5
    public PUBACK(
            final int packetIdentifier,
            final @NotNull Mqtt5PubAckReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties) {

        super(packetIdentifier, reasonCode, reasonString, userProperties);
    }

    @Override
    public @NotNull MessageType getType() {
        return MessageType.PUBACK;
    }

    public static @NotNull PUBACK createPubackFrom(final @NotNull PubackPacket packet) {

        final int packetIdentifier = packet.getPacketIdentifier();
        final Mqtt5PubAckReasonCode reasonCode = Mqtt5PubAckReasonCode.from(packet.getReasonCode());

        final String reasonString = packet.getReasonString().orElse(null);

        final ImmutableList.Builder<MqttUserProperty> userPropertyBuilder = ImmutableList.builder();
        for (final UserProperty userProperty : packet.getUserProperties().asList()) {
            userPropertyBuilder.add(new MqttUserProperty(userProperty.getName(), userProperty.getValue()));
        }
        final Mqtt5UserProperties mqtt5UserProperties = Mqtt5UserProperties.of(userPropertyBuilder.build());

        return new PUBACK(packetIdentifier, reasonCode, reasonString, mqtt5UserProperties);
    }
}
