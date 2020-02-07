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
package com.hivemq.extensions.packets.suback;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.suback.SubackPacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;

import java.util.List;
import java.util.Optional;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
public class SubackPacketImpl implements SubackPacket {

    private final @NotNull ImmutableList<SubackReasonCode> reasonCodes;
    private final @Nullable String reasonString;
    private final int packetIdentifier;
    private final @NotNull UserProperties userProperties;

    public SubackPacketImpl(final @NotNull SUBACK subAck) {
        final ImmutableList.Builder<SubackReasonCode> builder = ImmutableList.builder();
        for (final Mqtt5SubAckReasonCode code : subAck.getReasonCodes()) {
            builder.add(code.toSubackReasonCode());
        }
        reasonCodes = builder.build();
        reasonString = subAck.getReasonString();
        packetIdentifier = subAck.getPacketIdentifier();
        userProperties = subAck.getUserProperties().getPluginUserProperties();
    }

    public SubackPacketImpl(final @NotNull SubackPacket subackPacket) {
        reasonCodes = ImmutableList.copyOf(subackPacket.getReasonCodes());
        reasonString = subackPacket.getReasonString().orElse(null);
        packetIdentifier = subackPacket.getPacketIdentifier();
        userProperties = subackPacket.getUserProperties();
    }

    @Override
    public @Immutable @NotNull List<@NotNull SubackReasonCode> getReasonCodes() {
        return reasonCodes;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @Immutable @NotNull UserProperties getUserProperties() {
        return userProperties;
    }
}
