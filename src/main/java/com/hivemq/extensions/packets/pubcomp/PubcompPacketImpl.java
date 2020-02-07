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
package com.hivemq.extensions.packets.pubcomp;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompReasonCode;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;

import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
@Immutable
public class PubcompPacketImpl implements PubcompPacket {

    private final int packetIdentifier;
    private final @NotNull PubcompReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubcompPacketImpl(final @NotNull PUBCOMP pubcomp) {
        packetIdentifier = pubcomp.getPacketIdentifier();
        reasonCode = pubcomp.getReasonCode().toPubcompReasonCode();
        reasonString = pubcomp.getReasonString();
        userProperties = pubcomp.getUserProperties().getPluginUserProperties();
    }

    public PubcompPacketImpl(final @NotNull PubcompPacket pubcompPacket) {
        packetIdentifier = pubcompPacket.getPacketIdentifier();
        reasonCode = pubcompPacket.getReasonCode();
        reasonString = pubcompPacket.getReasonString().orElse(null);
        userProperties = pubcompPacket.getUserProperties();
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull PubcompReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public @NotNull UserProperties getUserProperties() {
        return userProperties;
    }
}
