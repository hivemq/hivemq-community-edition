package com.hivemq.extensions.packets.puback;

import com.google.common.base.Preconditions;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.puback.ModifiablePubackPacket;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.puback.PUBACK;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 */
public class ModifiablePubackPacketImpl implements ModifiablePubackPacket {

    private final @NotNull FullConfigurationService configurationService;

    private @NotNull AckReasonCode reasonCode;
    private @Nullable String reasonString;
    private int packetIdentifier;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private boolean modified = false;

    public ModifiablePubackPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PUBACK puback) {

        this.configurationService = configurationService;
        this.reasonCode = AckReasonCode.valueOf(puback.getReasonCode().name());
        this.reasonString = puback.getReasonString();
        this.packetIdentifier = puback.getPacketIdentifier();
        this.userProperties = new ModifiableUserPropertiesImpl(puback.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    public ModifiablePubackPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PubackPacket pubackPacket) {

        this.configurationService = configurationService;
        this.reasonCode = AckReasonCode.valueOf(pubackPacket.getReasonCode().name());
        this.reasonString = pubackPacket.getReasonString().orElse(null);
        this.packetIdentifier = pubackPacket.getPacketIdentifier();
        this.userProperties = new ModifiableUserPropertiesImpl(
                (InternalUserProperties) pubackPacket.getUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    @Override
    public void setReasonCode(final @NotNull AckReasonCode reasonCode) {
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        final boolean switched = (reasonCode == AckReasonCode.SUCCESS && this.reasonCode != AckReasonCode.SUCCESS) ||
                (reasonCode != AckReasonCode.SUCCESS && this.reasonCode == AckReasonCode.SUCCESS);
        Preconditions.checkState(!switched, "Reason code must not switch from successful to unsuccessful or vice versa");
        if (this.reasonCode == reasonCode) {
            return;
        }
        this.modified = true;
        this.reasonCode = reasonCode;
    }

    @Override
    public void setReasonString(final @Nullable String reasonString) {
        if (reasonString != null) {
            Preconditions.checkState(reasonCode != AckReasonCode.SUCCESS, "Reason string must not be set when reason code is successful");
        }
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.modified = true;
        this.reasonString = reasonString;
    }

    @Override
    public int getPacketIdentifier() {
        return this.packetIdentifier;
    }

    public @NotNull AckReasonCode getReasonCode() {
        return reasonCode;
    }

    @NotNull
    @Override
    public Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
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
