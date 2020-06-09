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
package com.hivemq.extensions.packets.suback;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.suback.SubackPacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.suback.SUBACK;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
public class SubackPacketImpl implements SubackPacket {

    final @NotNull ImmutableList<SubackReasonCode> reasonCodes;
    final @Nullable String reasonString;
    final int packetIdentifier;
    final @NotNull UserPropertiesImpl userProperties;

    public SubackPacketImpl(
            final @NotNull ImmutableList<SubackReasonCode> reasonCodes,
            final @Nullable String reasonString,
            final int packetIdentifier,
            final @NotNull UserPropertiesImpl userProperties) {

        this.reasonCodes = reasonCodes;
        this.reasonString = reasonString;
        this.packetIdentifier = packetIdentifier;
        this.userProperties = userProperties;
    }

    public SubackPacketImpl(final @NotNull SUBACK subAck) {
        final ImmutableList.Builder<SubackReasonCode> builder = ImmutableList.builder();
        subAck.getReasonCodes().forEach(code -> builder.add(code.toSubackReasonCode()));
        reasonCodes = builder.build();
        reasonString = subAck.getReasonString();
        packetIdentifier = subAck.getPacketIdentifier();
        userProperties = UserPropertiesImpl.of(subAck.getUserProperties().asList());
    }

    @Override
    public @NotNull ImmutableList<SubackReasonCode> getReasonCodes() {
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
        if (!(o instanceof SubackPacketImpl)) {
            return false;
        }
        final SubackPacketImpl that = (SubackPacketImpl) o;
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
