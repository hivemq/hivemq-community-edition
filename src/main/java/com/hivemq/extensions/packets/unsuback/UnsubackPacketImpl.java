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
package com.hivemq.extensions.packets.unsuback;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackPacket;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
public class UnsubackPacketImpl implements UnsubackPacket {

    final @NotNull ImmutableList<UnsubackReasonCode> reasonCodes;
    final @Nullable String reasonString;
    final int packetIdentifier;
    final @NotNull UserPropertiesImpl userProperties;

    public UnsubackPacketImpl(
            final @NotNull ImmutableList<UnsubackReasonCode> reasonCodes,
            final @Nullable String reasonString,
            final int packetIdentifier,
            final @NotNull UserPropertiesImpl userProperties) {

        this.reasonCodes = reasonCodes;
        this.reasonString = reasonString;
        this.packetIdentifier = packetIdentifier;
        this.userProperties = userProperties;
    }

    public UnsubackPacketImpl(final @NotNull UNSUBACK unsuback) {
        final ImmutableList.Builder<UnsubackReasonCode> builder = ImmutableList.builder();
        unsuback.getReasonCodes().forEach(code -> builder.add(code.toUnsubackReasonCode()));
        reasonCodes = builder.build();
        reasonString = unsuback.getReasonString();
        packetIdentifier = unsuback.getPacketIdentifier();
        userProperties = UserPropertiesImpl.of(unsuback.getUserProperties().asList());
    }

    @Override
    public @NotNull ImmutableList<UnsubackReasonCode> getReasonCodes() {
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
    public @NotNull UserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnsubackPacketImpl)) {
            return false;
        }
        final UnsubackPacketImpl that = (UnsubackPacketImpl) o;
        return reasonCodes.equals(that.reasonCodes) &&
                Objects.equals(reasonString, that.reasonString) &&
                (packetIdentifier == that.packetIdentifier) &&
                userProperties.equals(that.userProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reasonCodes, reasonString, packetIdentifier, userProperties);
    }
}
