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
package com.hivemq.extensions.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
public class DisconnectPacketImpl implements DisconnectPacket {

    final @NotNull DisconnectReasonCode reasonCode;
    final @Nullable String reasonString;
    final long sessionExpiryInterval;
    final @Nullable String serverReference;
    final @NotNull UserPropertiesImpl userProperties;

    public DisconnectPacketImpl(
            final @NotNull DisconnectReasonCode reasonCode,
            final @Nullable String reasonString,
            final long sessionExpiryInterval,
            final @Nullable String serverReference,
            final @NotNull UserPropertiesImpl userProperties) {

        this.reasonCode = reasonCode;
        this.reasonString = reasonString;
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.serverReference = serverReference;
        this.userProperties = userProperties;
    }

    public DisconnectPacketImpl(final @NotNull DISCONNECT disconnect) {
        this(
                disconnect.getReasonCode().toDisconnectReasonCode(),
                disconnect.getReasonString(),
                disconnect.getSessionExpiryInterval(),
                disconnect.getServerReference(),
                UserPropertiesImpl.of(disconnect.getUserProperties().asList()));
    }

    @Override
    public @NotNull DisconnectReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public @NotNull Optional<Long> getSessionExpiryInterval() {
        return (sessionExpiryInterval == DISCONNECT.SESSION_EXPIRY_NOT_SET) ? Optional.empty() :
                Optional.of(sessionExpiryInterval);
    }

    @Override
    public @NotNull Optional<String> getServerReference() {
        return Optional.ofNullable(serverReference);
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
        if (!(o instanceof DisconnectPacketImpl)) {
            return false;
        }
        final DisconnectPacketImpl that = (DisconnectPacketImpl) o;
        return (reasonCode == that.reasonCode) &&
                Objects.equals(reasonString, that.reasonString) &&
                (sessionExpiryInterval == that.sessionExpiryInterval) &&
                Objects.equals(serverReference, that.serverReference) &&
                userProperties.equals(that.userProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reasonCode, reasonString, sessionExpiryInterval, serverReference, userProperties);
    }
}
