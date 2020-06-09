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
package com.hivemq.extensions.packets.pubcomp;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.pubcomp.ModifiablePubcompPacket;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompReasonCode;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class ModifiablePubcompPacketImpl implements ModifiablePubcompPacket {

    private final int packetIdentifier;
    private final @NotNull PubcompReasonCode reasonCode;
    private @Nullable String reasonString;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private final @NotNull FullConfigurationService configurationService;
    private boolean modified = false;

    public ModifiablePubcompPacketImpl(
            final @NotNull PubcompPacketImpl packet,
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
    public @NotNull PubcompReasonCode getReasonCode() {
        return reasonCode;
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

    public @NotNull PubcompPacketImpl copy() {
        return new PubcompPacketImpl(packetIdentifier, reasonCode, reasonString, userProperties.copy());
    }

    public @NotNull ModifiablePubcompPacketImpl update(final @NotNull PubcompPacketImpl packet) {
        return new ModifiablePubcompPacketImpl(packet, configurationService);
    }
}
