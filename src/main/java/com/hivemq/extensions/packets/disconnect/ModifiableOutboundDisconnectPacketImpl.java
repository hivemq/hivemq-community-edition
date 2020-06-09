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

import com.google.common.base.Preconditions;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableOutboundDisconnectPacket;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@ThreadSafe
public class ModifiableOutboundDisconnectPacketImpl implements ModifiableOutboundDisconnectPacket {

    private @NotNull DisconnectReasonCode reasonCode;
    private @Nullable String reasonString;
    private final long sessionExpiryInterval;
    private @Nullable String serverReference;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private final @NotNull FullConfigurationService configurationService;
    private boolean modified = false;

    public ModifiableOutboundDisconnectPacketImpl(
            final @NotNull DisconnectPacketImpl packet,
            final @NotNull FullConfigurationService configurationService) {

        reasonCode = packet.reasonCode;
        reasonString = packet.reasonString;
        sessionExpiryInterval = packet.sessionExpiryInterval;
        serverReference = packet.serverReference;
        userProperties = new ModifiableUserPropertiesImpl(
                packet.userProperties.asInternalList(), configurationService.securityConfiguration().validateUTF8());

        this.configurationService = configurationService;
    }

    @Override
    public @NotNull DisconnectReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public void setReasonCode(final @NotNull DisconnectReasonCode reasonCode) {
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        Preconditions.checkArgument(
                reasonCode != DisconnectReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                "Reason code %s must not be used for disconnect packets.", reasonCode);
        Preconditions.checkArgument(
                Mqtt5DisconnectReasonCode.from(reasonCode).canBeSentByServer(),
                "Reason code %s must not be used for outbound disconnect packets from the server to a client.",
                reasonCode);
        if (this.reasonCode == reasonCode) {
            return;
        }
        this.reasonCode = reasonCode;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public void setReasonString(final @Nullable String reasonString) {
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.reasonString = reasonString;
        modified = true;
    }

    @Override
    public @NotNull Optional<Long> getSessionExpiryInterval() {
        return (sessionExpiryInterval == Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET) ? Optional.empty() :
                Optional.of(sessionExpiryInterval);
    }

    @Override
    public @NotNull Optional<String> getServerReference() {
        return Optional.ofNullable(serverReference);
    }

    @Override
    public void setServerReference(final @Nullable String serverReference) {
        PluginBuilderUtil.checkServerReference(
                serverReference, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.serverReference, serverReference)) {
            return;
        }
        this.serverReference = serverReference;
        modified = true;
    }

    @Override
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }

    public @NotNull DisconnectPacketImpl copy() {
        return new DisconnectPacketImpl(
                reasonCode, reasonString, sessionExpiryInterval, serverReference, userProperties.copy());
    }

    public @NotNull ModifiableOutboundDisconnectPacketImpl update(final @NotNull DisconnectPacketImpl packet) {
        return new ModifiableOutboundDisconnectPacketImpl(packet, configurationService);
    }
}
