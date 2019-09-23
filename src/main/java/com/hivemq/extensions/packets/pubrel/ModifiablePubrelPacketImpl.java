package com.hivemq.extensions.packets.pubrel;

import com.google.common.base.Preconditions;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PubrelReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrel.ModifiablePubrelPacket;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.pubrel.PUBREL;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 */
public class ModifiablePubrelPacketImpl extends PubrelPacketImpl implements ModifiablePubrelPacket {

    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private boolean modified = false;

    private @Nullable String reasonString;

    public ModifiablePubrelPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PUBREL pubrel) {

        super(pubrel);
        this.configurationService = configurationService;
        this.reasonString = pubrel.getReasonString();
        this.userProperties = new ModifiableUserPropertiesImpl(
                pubrel.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    @NotNull
    @Override
    public Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public void setReasonString(final @Nullable String reasonString) {
        if (reasonString != null) {
            Preconditions.checkState(
                    getReasonCode() != PubrelReasonCode.SUCCESS,
                    "Reason string must not be set when reason code is successful");
        }
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.modified = true;
        this.reasonString = reasonString;
    }

    @NotNull
    @Override
    public ModifiableUserProperties getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }
}
