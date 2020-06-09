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
package com.hivemq.mqtt.message.unsuback;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.packets.unsuback.UnsubackPacketImpl;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;

import java.util.List;

/**
 * The MQTT UNSUBACK message
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 * @since 1.4
 */
@Immutable
public class UNSUBACK extends MqttMessageWithUserProperties.MqttMessageWithIdAndReasonCodes<Mqtt5UnsubAckReasonCode>
        implements Mqtt3UNSUBACK, Mqtt5UNSUBACK {

    //MQTT 3
    public UNSUBACK(final int packetIdentifier, final @NotNull Mqtt5UnsubAckReasonCode... entries) {
        super(packetIdentifier, ImmutableList.copyOf(entries), null, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    //MQTT 3
    public UNSUBACK(final int packetIdentifier, final @NotNull List<Mqtt5UnsubAckReasonCode> grantedQos) {
        this(packetIdentifier, grantedQos, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    //MQTT 5
    public UNSUBACK(
            final int packetIdentifier,
            final @NotNull List<Mqtt5UnsubAckReasonCode> grantedQos,
            final @Nullable String reasonString) {

        this(packetIdentifier, grantedQos, reasonString, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    //MQTT 5
    public UNSUBACK(
            final int packetIdentifier,
            final @NotNull List<Mqtt5UnsubAckReasonCode> grantedQos,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties) {

        super(packetIdentifier, ImmutableList.copyOf(grantedQos), reasonString, userProperties);
    }

    //MQTT 5
    public UNSUBACK(
            final int packetIdentifier,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final @NotNull Mqtt5UnsubAckReasonCode... grantedQos) {

        super(packetIdentifier, ImmutableList.copyOf(grantedQos), reasonString, userProperties);
    }

    @Override
    public @NotNull MessageType getType() {
        return MessageType.UNSUBACK;
    }

    public static @NotNull UNSUBACK from(final @NotNull UnsubackPacketImpl packet) {
        final ImmutableList.Builder<Mqtt5UnsubAckReasonCode> reasonCodesBuilder = ImmutableList.builder();
        packet.getReasonCodes().forEach(reasonCode -> reasonCodesBuilder.add(Mqtt5UnsubAckReasonCode.from(reasonCode)));

        return new UNSUBACK(
                packet.getPacketIdentifier(),
                reasonCodesBuilder.build(),
                packet.getReasonString().orElse(null),
                Mqtt5UserProperties.of(packet.getUserProperties().asInternalList()));
    }
}
