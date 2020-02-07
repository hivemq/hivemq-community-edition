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
package com.hivemq.extensions.packets.pubrec;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.mqtt.message.pubrec.PUBREC;

import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
@Immutable
public class PubrecPacketImpl implements PubrecPacket {

    private final int packetIdentifier;
    private final @NotNull AckReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubrecPacketImpl(final @NotNull PUBREC pubrec) {
        packetIdentifier = pubrec.getPacketIdentifier();
        reasonCode = pubrec.getReasonCode().toAckReasonCode();
        reasonString = pubrec.getReasonString();
        userProperties = pubrec.getUserProperties().getPluginUserProperties();
    }

    public PubrecPacketImpl(final @NotNull PubrecPacket pubrecPacket) {
        packetIdentifier = pubrecPacket.getPacketIdentifier();
        reasonCode = pubrecPacket.getReasonCode();
        reasonString = pubrecPacket.getReasonString().orElse(null);
        userProperties = pubrecPacket.getUserProperties();
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull AckReasonCode getReasonCode() {
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
