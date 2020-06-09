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

import com.google.common.base.Preconditions;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrec.ModifiablePubrecPacket;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class ModifiablePubrecPacketImpl implements ModifiablePubrecPacket {

    private final int packetIdentifier;
    private @NotNull AckReasonCode reasonCode;
    private @Nullable String reasonString;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private final @NotNull FullConfigurationService configurationService;
    private boolean modified = false;

    public ModifiablePubrecPacketImpl(
            final @NotNull PubrecPacketImpl packet,
            final @NotNull FullConfigurationService configurationService) {

        packetIdentifier = packet.packetIdentifier;
        reasonCode = packet.reasonCode;
        reasonString = packet.reasonString;
        userProperties = new ModifiableUserPropertiesImpl(
                packet.userProperties.asInternalList(), configurationService.securityConfiguration().validateUTF8());

        this.configurationService = configurationService;
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
    public void setReasonCode(final @NotNull AckReasonCode reasonCode) {
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        if (this.reasonCode == reasonCode) {
            return;
        }
        final Mqtt5PubRecReasonCode newReasonCode = Mqtt5PubRecReasonCode.from(reasonCode);
        final Mqtt5PubRecReasonCode oldReasonCode = Mqtt5PubRecReasonCode.from(this.reasonCode);
        final boolean switched = newReasonCode.isError() != oldReasonCode.isError();
        Preconditions.checkState(
                !switched, "Reason code must not switch from successful to unsuccessful or vice versa");
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
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }

    public @NotNull PubrecPacketImpl copy() {
        return new PubrecPacketImpl(packetIdentifier, reasonCode, reasonString, userProperties.copy());
    }

    public @NotNull ModifiablePubrecPacketImpl update(final @NotNull PubrecPacketImpl pubrecPacket) {
        return new ModifiablePubrecPacketImpl(pubrecPacket, configurationService);
    }
}
