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

package com.hivemq.mqtt.message.pubcomp;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5PubCompReasonCode;

/**
 * The MQTT PUBCOMP message
 *
 * @author Dominik Obermaier
 * @author Waldemar Ruck
 * @since 1.4
 */
public class PUBCOMP extends MqttMessageWithUserProperties.MqttMessageWithIdAndReasonCode<Mqtt5PubCompReasonCode>
        implements Mqtt3PUBCOMP, Mqtt5PUBCOMP {

    //MQTT 3
    public PUBCOMP(final int packetIdentifier) {
        super(packetIdentifier, Mqtt5PubCompReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    //MQTT 5
    public PUBCOMP(
            final int packetIdentifier,
            final @NotNull Mqtt5PubCompReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties) {

        super(packetIdentifier, reasonCode, reasonString, userProperties);
    }

    @Override
    public @NotNull MessageType getType() {
        return MessageType.PUBCOMP;
    }

    public static @NotNull PUBCOMP createPubcompFrom(final @NotNull PubcompPacket packet) {

        final int packetIdentifier = packet.getPacketIdentifier();
        final Mqtt5PubCompReasonCode reasonCode = Mqtt5PubCompReasonCode.from(packet.getReasonCode());

        final String reasonString = packet.getReasonString().orElse(null);

        final ImmutableList.Builder<MqttUserProperty> userPropertyBuilder = ImmutableList.builder();
        for (final UserProperty userProperty : packet.getUserProperties().asList()) {
            userPropertyBuilder.add(new MqttUserProperty(userProperty.getName(), userProperty.getValue()));
        }
        final Mqtt5UserProperties mqtt5UserProperties = Mqtt5UserProperties.of(userPropertyBuilder.build());

        return new PUBCOMP(packetIdentifier, reasonCode, reasonString, mqtt5UserProperties);
    }
}
