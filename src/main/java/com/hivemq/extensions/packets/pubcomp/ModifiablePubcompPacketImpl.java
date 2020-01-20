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
package com.hivemq.extensions.packets.pubcomp;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.pubcomp.ModifiablePubcompPacket;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompReasonCode;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class ModifiablePubcompPacketImpl implements ModifiablePubcompPacket {

    private final @NotNull FullConfigurationService configurationService;

    private final int packetIdentifier;
    private final @NotNull PubcompReasonCode reasonCode;
    private @Nullable String reasonString;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private boolean modified = false;

    public ModifiablePubcompPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PUBCOMP pubcomp) {

        this.configurationService = configurationService;
        packetIdentifier = pubcomp.getPacketIdentifier();
        reasonCode = pubcomp.getReasonCode().toPubcompReasonCode();
        reasonString = pubcomp.getReasonString();
        userProperties = new ModifiableUserPropertiesImpl(
                pubcomp.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    public ModifiablePubcompPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PubcompPacket pubcomp) {

        this.configurationService = configurationService;
        packetIdentifier = pubcomp.getPacketIdentifier();
        reasonCode = pubcomp.getReasonCode();
        reasonString = pubcomp.getReasonString().orElse(null);
        userProperties = new ModifiableUserPropertiesImpl(
                (InternalUserProperties) pubcomp.getUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
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
    public @NotNull ModifiableUserProperties getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }
}
