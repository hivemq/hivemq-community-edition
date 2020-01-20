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
package com.hivemq.extensions.packets.pubrel;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.pubrel.ModifiablePubrelPacket;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelReasonCode;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.pubrel.PUBREL;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class ModifiablePubrelPacketImpl implements ModifiablePubrelPacket {

    private final @NotNull FullConfigurationService configurationService;

    private final int packetIdentifier;
    private final @NotNull PubrelReasonCode reasonCode;
    private @Nullable String reasonString;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private boolean modified = false;

    public ModifiablePubrelPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PUBREL pubrel) {

        this.configurationService = configurationService;
        packetIdentifier = pubrel.getPacketIdentifier();
        reasonCode = pubrel.getReasonCode().toPubrelReasonCode();
        reasonString = pubrel.getReasonString();
        userProperties = new ModifiableUserPropertiesImpl(
                pubrel.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    public ModifiablePubrelPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PubrelPacket pubrelPacket) {

        this.configurationService = configurationService;
        packetIdentifier = pubrelPacket.getPacketIdentifier();
        reasonCode = pubrelPacket.getReasonCode();
        reasonString = pubrelPacket.getReasonString().orElse(null);
        userProperties = new ModifiableUserPropertiesImpl(
                (InternalUserProperties) pubrelPacket.getUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull PubrelReasonCode getReasonCode() {
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
    public @NotNull ModifiableUserProperties getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }
}
