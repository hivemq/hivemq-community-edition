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
package com.hivemq.extensions.packets.pubrel;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.pubrel.PUBREL;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
@Immutable
public class PubrelPacketImpl implements PubrelPacket {

    final int packetIdentifier;
    final @NotNull PubrelReasonCode reasonCode;
    final @Nullable String reasonString;
    final @NotNull UserPropertiesImpl userProperties;

    public PubrelPacketImpl(
            final int packetIdentifier,
            final @NotNull PubrelReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull UserPropertiesImpl userProperties) {

        this.packetIdentifier = packetIdentifier;
        this.reasonCode = reasonCode;
        this.reasonString = reasonString;
        this.userProperties = userProperties;
    }

    public PubrelPacketImpl(final @NotNull PUBREL pubrel) {
        this(
                pubrel.getPacketIdentifier(),
                pubrel.getReasonCode().toPubrelReasonCode(),
                pubrel.getReasonString(),
                UserPropertiesImpl.of(pubrel.getUserProperties().asList()));
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull PubrelReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public @NotNull UserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PubrelPacketImpl)) {
            return false;
        }
        final PubrelPacketImpl that = (PubrelPacketImpl) o;
        return (packetIdentifier == that.packetIdentifier) &&
                (reasonCode == that.reasonCode) &&
                Objects.equals(reasonString, that.reasonString) &&
                userProperties.equals(that.userProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packetIdentifier, reasonCode, reasonString, userProperties);
    }
}
