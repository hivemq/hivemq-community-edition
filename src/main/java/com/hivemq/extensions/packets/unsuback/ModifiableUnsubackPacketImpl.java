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

package com.hivemq.extensions.packets.unsuback;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.unsuback.ModifiableUnsubackPacket;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackPacket;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackReasonCode;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class ModifiableUnsubackPacketImpl implements ModifiableUnsubackPacket {

    private final @NotNull FullConfigurationService configurationService;

    private @NotNull ImmutableList<UnsubackReasonCode> reasonCodes;
    private @Nullable String reasonString;
    private final int packetIdentifier;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private boolean modified = false;

    public ModifiableUnsubackPacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull UNSUBACK unsuback) {

        this.configurationService = fullConfigurationService;
        final ImmutableList.Builder<UnsubackReasonCode> builder = ImmutableList.builder();
        for (final Mqtt5UnsubAckReasonCode code : unsuback.getReasonCodes()) {
            builder.add(code.toUnsubackReasonCode());
        }
        reasonCodes = builder.build();
        reasonString = unsuback.getReasonString();
        packetIdentifier = unsuback.getPacketIdentifier();
        userProperties = new ModifiableUserPropertiesImpl(
                unsuback.getUserProperties().getPluginUserProperties(),
                fullConfigurationService.securityConfiguration().validateUTF8());
    }

    public ModifiableUnsubackPacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull UnsubackPacket unsubackPacket) {

        this.configurationService = fullConfigurationService;
        reasonCodes = ImmutableList.copyOf(unsubackPacket.getReasonCodes());
        reasonString = unsubackPacket.getReasonString().orElse(null);
        packetIdentifier = unsubackPacket.getPacketIdentifier();
        userProperties = new ModifiableUserPropertiesImpl(
                (InternalUserProperties) unsubackPacket.getUserProperties(),
                fullConfigurationService.securityConfiguration().validateUTF8());
    }

    @Override
    public @Immutable @NotNull List<@NotNull UnsubackReasonCode> getReasonCodes() {
        return reasonCodes;
    }

    @Override
    public void setReasonCodes(final @NotNull List<@NotNull UnsubackReasonCode> reasonCodes) {
        Preconditions.checkNotNull(reasonCodes, "Reason codes must never be null.");
        if (reasonCodes.size() != this.reasonCodes.size()) {
            throw new IllegalArgumentException("The amount of reason codes must not be changed.");
        }
        for (int i = 0; i < reasonCodes.size(); i++) {
            Preconditions.checkNotNull(reasonCodes.get(i), "Reason code (at index %s) must never be null.", i);
            final Mqtt5UnsubAckReasonCode oldReasonCode = Mqtt5UnsubAckReasonCode.from(this.reasonCodes.get(i));
            final Mqtt5UnsubAckReasonCode newReasonCode = Mqtt5UnsubAckReasonCode.from(reasonCodes.get(i));
            Preconditions.checkState(newReasonCode.isError() == oldReasonCode.isError(),
                    "Reason code (at index %s) must not switch from successful to unsuccessful or vice versa.", i);
        }
        if (Objects.equals(this.reasonCodes, reasonCodes)) {
            return;
        }
        this.reasonCodes = ImmutableList.copyOf(reasonCodes);
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
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull ModifiableUserProperties getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }
}
