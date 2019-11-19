package com.hivemq.extensions.packets.pubcomp;

import com.google.common.base.Preconditions;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.pubcomp.ModifiablePubcompPacket;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extension.sdk.api.packets.publish.PubcompReasonCode;
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
        reasonCode = PubcompReasonCode.valueOf(pubcomp.getReasonCode().name());
        packetIdentifier = pubcomp.getPacketIdentifier();
        reasonString = pubcomp.getReasonString();
        userProperties = new ModifiableUserPropertiesImpl(
                pubcomp.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    public ModifiablePubcompPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PubcompPacket pubcomp) {

        this.configurationService = configurationService;
        reasonCode = pubcomp.getReasonCode();
        packetIdentifier = pubcomp.getPacketIdentifier();
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
        if (reasonString != null) {
            Preconditions.checkState(
                    getReasonCode() != PubcompReasonCode.SUCCESS,
                    "Reason string must not be set when reason code is successful");
        }
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
