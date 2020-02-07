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

package com.hivemq.extensions.packets.connack;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.connack.ModifiableConnackPacket;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.connack.CONNACK;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Lukas Brandl
 */
@ThreadSafe
public class ModifiableConnackPacketImpl extends ConnackPacketImpl implements ModifiableConnackPacket {

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private final boolean requestResponseInformation;

    private boolean modified;

    private @NotNull ConnackReasonCode reasonCode;
    private @Nullable String reasonString;
    private @Nullable String responseInformation;
    private @Nullable String serverReference;


    public ModifiableConnackPacketImpl(@NotNull final FullConfigurationService configurationService, @NotNull final CONNACK connack, final boolean requestResponseInformation) {
        super(connack);
        this.configurationService = configurationService;
        this.requestResponseInformation = requestResponseInformation;
        this.userProperties = new ModifiableUserPropertiesImpl(connack.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
        this.reasonCode = connack.getReasonCode().toConnackReasonCode();
        this.reasonString = connack.getReasonString();
        this.responseInformation = connack.getResponseInformation();
        this.serverReference = connack.getServerReference();
        this.modified = false;
    }

    @Override
    public @NotNull ConnackReasonCode getReasonCode() {
        return this.reasonCode;
    }

    @Override
    public void setReasonCode(final @NotNull ConnackReasonCode reasonCode) {
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        final boolean switched = (reasonCode == ConnackReasonCode.SUCCESS && this.reasonCode != ConnackReasonCode.SUCCESS) ||
                (reasonCode != ConnackReasonCode.SUCCESS && this.reasonCode == ConnackReasonCode.SUCCESS);
        Preconditions.checkState(!switched, "Reason code must not switch from successful to unsuccessful or vice versa");
        if (reasonCode == this.reasonCode) {
            return;
        }
        this.modified = true;
        this.reasonCode = reasonCode;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(this.reasonString);
    }

    @Override
    public void setReasonString(final @Nullable String reasonString) {
        if (reasonString != null) {
            Preconditions.checkState(reasonCode != ConnackReasonCode.SUCCESS, "Reason string must not be set when reason code is successful");
        }
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.modified = true;
        this.reasonString = reasonString;
    }

    @Override
    public @NotNull Optional<String> getResponseInformation() {
        return Optional.ofNullable(this.responseInformation);
    }

    @Override
    public void setResponseInformation(final @Nullable String responseInformation) {
        PluginBuilderUtil.checkResponseInformation(responseInformation, requestResponseInformation, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.responseInformation, responseInformation)) {
            return;
        }
        this.modified = true;
        this.responseInformation = responseInformation;
    }

    @Override
    public @NotNull Optional<String> getServerReference() {
        return Optional.ofNullable(this.serverReference);
    }

    @Override
    public void setServerReference(final @Nullable String serverReference) {
        PluginBuilderUtil.checkServerReference(serverReference, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.serverReference, serverReference)) {
            return;
        }
        this.modified = true;
        this.serverReference = serverReference;

    }

    @NotNull
    @Override
    public ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }
}
