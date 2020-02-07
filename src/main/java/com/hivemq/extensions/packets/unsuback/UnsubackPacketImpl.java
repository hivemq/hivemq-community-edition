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

package com.hivemq.extensions.packets.unsuback;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackPacket;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;

import java.util.List;
import java.util.Optional;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class UnsubackPacketImpl implements UnsubackPacket {

    private final @NotNull ImmutableList<UnsubackReasonCode> reasonCodes;
    private final @Nullable String reasonString;
    private final int packetIdentifier;
    private final @NotNull UserProperties userProperties;

    public UnsubackPacketImpl(final @NotNull UNSUBACK unsuback) {
        final ImmutableList.Builder<UnsubackReasonCode> builder = ImmutableList.builder();
        for (final Mqtt5UnsubAckReasonCode code : unsuback.getReasonCodes()) {
            builder.add(code.toUnsubackReasonCode());
        }
        reasonCodes = builder.build();
        reasonString = unsuback.getReasonString();
        packetIdentifier = unsuback.getPacketIdentifier();
        userProperties = unsuback.getUserProperties().getPluginUserProperties();
    }

    public UnsubackPacketImpl(final @NotNull UnsubackPacket unsubackPacket) {
        reasonCodes = ImmutableList.copyOf(unsubackPacket.getReasonCodes());
        reasonString = unsubackPacket.getReasonString().orElse(null);
        packetIdentifier = unsubackPacket.getPacketIdentifier();
        userProperties = unsubackPacket.getUserProperties();
    }

    @Override
    public @Immutable @NotNull List<@NotNull UnsubackReasonCode> getReasonCodes() {
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
