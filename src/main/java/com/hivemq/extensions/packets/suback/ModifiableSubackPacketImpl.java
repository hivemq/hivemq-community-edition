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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.suback.ModifiableSubackPacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class ModifiableSubackPacketImpl implements ModifiableSubackPacket {

    private @NotNull ImmutableList<SubackReasonCode> reasonCodes;
    private @Nullable String reasonString;
    private final int packetIdentifier;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private final @NotNull FullConfigurationService configurationService;
    private boolean modified = false;

    public ModifiableSubackPacketImpl(
            final @NotNull SubackPacketImpl packet,
            final @NotNull FullConfigurationService configurationService) {

        reasonCodes = packet.reasonCodes;
        reasonString = packet.reasonString;
        packetIdentifier = packet.packetIdentifier;
        userProperties = new ModifiableUserPropertiesImpl(
                packet.userProperties.asInternalList(), configurationService.securityConfiguration().validateUTF8());

        this.configurationService = configurationService;
    }

    @Override
    public @NotNull ImmutableList<SubackReasonCode> getReasonCodes() {
        return reasonCodes;
    }

    @Override
    public void setReasonCodes(final @NotNull List<@NotNull SubackReasonCode> reasonCodes) {
        Preconditions.checkNotNull(reasonCodes, "Reason codes must never be null.");
        if (reasonCodes.size() != this.reasonCodes.size()) {
            throw new IllegalArgumentException("The amount of reason codes must not be changed.");
        }
        for (int i = 0; i < reasonCodes.size(); i++) {
            Preconditions.checkNotNull(reasonCodes.get(i), "Reason code (at index %s) must never be null.", i);
            final Mqtt5SubAckReasonCode oldReasonCode = Mqtt5SubAckReasonCode.from(this.reasonCodes.get(i));
            final Mqtt5SubAckReasonCode newReasonCode = Mqtt5SubAckReasonCode.from(reasonCodes.get(i));
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
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }

    public @NotNull SubackPacketImpl copy() {
        return new SubackPacketImpl(reasonCodes, reasonString, packetIdentifier, userProperties.copy());
    }

    public @NotNull ModifiableSubackPacketImpl update(final @NotNull SubackPacketImpl packet) {
        return new ModifiableSubackPacketImpl(packet, configurationService);
    }
}
