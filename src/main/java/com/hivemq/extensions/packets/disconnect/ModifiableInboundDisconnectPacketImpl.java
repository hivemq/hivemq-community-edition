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
package com.hivemq.extensions.packets.disconnect;

import com.google.common.base.Preconditions;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableInboundDisconnectPacket;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class ModifiableInboundDisconnectPacketImpl implements ModifiableInboundDisconnectPacket {

    private final @NotNull FullConfigurationService configurationService;

    private @NotNull DisconnectReasonCode reasonCode;
    private @Nullable String reasonString;
    private long sessionExpiryInterval;
    private final long originalSessionExpiryInterval;
    private final @Nullable String serverReference;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private boolean modified = false;

    public ModifiableInboundDisconnectPacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull DISCONNECT originalDisconnect,
            final long originalSessionExpiryInterval) {

        configurationService = fullConfigurationService;
        reasonCode = originalDisconnect.getReasonCode().toDisconnectReasonCode();
        reasonString = originalDisconnect.getReasonString();
        sessionExpiryInterval = originalDisconnect.getSessionExpiryInterval();
        this.originalSessionExpiryInterval = originalSessionExpiryInterval;
        serverReference = originalDisconnect.getServerReference();
        userProperties = new ModifiableUserPropertiesImpl(
                originalDisconnect.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    public ModifiableInboundDisconnectPacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull DisconnectPacket disconnectPacket,
            final long originalSessionExpiryInterval) {

        configurationService = fullConfigurationService;
        reasonCode = disconnectPacket.getReasonCode();
        reasonString = disconnectPacket.getReasonString().orElse(null);
        sessionExpiryInterval = disconnectPacket.getSessionExpiryInterval().orElse(Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET);
        this.originalSessionExpiryInterval = originalSessionExpiryInterval;
        serverReference = disconnectPacket.getServerReference().orElse(null);
        userProperties = new ModifiableUserPropertiesImpl(
                (InternalUserProperties) disconnectPacket.getUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    @Override
    public @NotNull DisconnectReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public synchronized void setReasonCode(final @NotNull DisconnectReasonCode reasonCode) {
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        Preconditions.checkArgument(
                reasonCode != DisconnectReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                "Reason code %s must not be used for disconnect packets.", reasonCode);
        Preconditions.checkArgument(
                Mqtt5DisconnectReasonCode.from(reasonCode).canBeSentByClient(),
                "Reason code %s must not be used for inbound disconnect packets from a client to the server.",
                reasonCode);
        if (Objects.equals(this.reasonCode, reasonCode)) {
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
    public synchronized void setReasonString(final @Nullable String reasonString) {
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
    public synchronized void setSessionExpiryInterval(final @Nullable Long sessionExpiryInterval) {
        final long interval =
                (sessionExpiryInterval == null) ? Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET : sessionExpiryInterval;
        if (this.sessionExpiryInterval == interval) {
            return;
        }
        checkArgument(interval >= 0, "Session expiry interval must be greater than 0");
        final long configuredMaximum = configurationService.mqttConfiguration().maxSessionExpiryInterval();
        checkArgument(
                interval < configuredMaximum,
                "Session expiry interval must not be greater than the configured maximum of " + configuredMaximum);
        if (interval > 0) {
            checkState(
                    originalSessionExpiryInterval != 0,
                    "Session expiry interval must not be set when a client connected with session expiry interval = '0'");
        }
        this.sessionExpiryInterval = interval;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getServerReference() {
        return Optional.ofNullable(serverReference);
    }

    @Override
    public @NotNull ModifiableUserProperties getUserProperties() {
        return this.userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }
}
