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
package com.hivemq.extensions.packets.pubrec;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.pubrec.PUBREC;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
@Immutable
public class PubrecPacketImpl implements PubrecPacket {

    final int packetIdentifier;
    final @NotNull AckReasonCode reasonCode;
    final @Nullable String reasonString;
    final @NotNull UserPropertiesImpl userProperties;

    public PubrecPacketImpl(
            final int packetIdentifier,
            final @NotNull AckReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull UserPropertiesImpl userProperties) {

        this.packetIdentifier = packetIdentifier;
        this.reasonCode = reasonCode;
        this.reasonString = reasonString;
        this.userProperties = userProperties;
    }

    public PubrecPacketImpl(final @NotNull PUBREC pubrec) {
        this(
                pubrec.getPacketIdentifier(),
                pubrec.getReasonCode().toAckReasonCode(),
                pubrec.getReasonString(),
                UserPropertiesImpl.of(pubrec.getUserProperties().asList()));
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
    public @NotNull UserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PubrecPacketImpl)) {
            return false;
        }
        final PubrecPacketImpl that = (PubrecPacketImpl) o;
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
