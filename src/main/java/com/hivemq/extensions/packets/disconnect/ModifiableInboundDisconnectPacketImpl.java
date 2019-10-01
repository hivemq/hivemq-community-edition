package com.hivemq.extensions.packets.disconnect;

import com.google.common.base.Preconditions;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableInboundDisconnectPacket;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Robin Atherton
 */
public class ModifiableInboundDisconnectPacketImpl implements ModifiableInboundDisconnectPacket {

    private final @NotNull FullConfigurationService configurationService;
    private boolean modified = false;
    private @NotNull DisconnectReasonCode reasonCode;


    private long sessionExpiryInterval;
    private String reasonString;
    private String serverReference;
    private final @Nullable ModifiableUserPropertiesImpl userProperties;

    public ModifiableInboundDisconnectPacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull DISCONNECT originalDisconnect) {
        this.configurationService = fullConfigurationService;
        this.reasonCode = DisconnectReasonCode.valueOf(originalDisconnect.getReasonCode().name());
        this.userProperties = new ModifiableUserPropertiesImpl(
                originalDisconnect.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
        this.reasonString = originalDisconnect.getReasonString();
        this.sessionExpiryInterval = originalDisconnect.getSessionExpiryInterval();
        this.serverReference = originalDisconnect.getServerReference();
    }


    @Override
    public void setReasonString(final @NotNull String reasonString) {
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.reasonString = reasonString;
        modified = true;
    }

    @Override
    public void setReasonCode(final @NotNull DisconnectReasonCode reasonCode) {
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());

        if (Objects.equals(this.reasonCode, reasonCode)) {
            return;
        }
        this.reasonCode = reasonCode;
        modified = true;
    }

    @Override
    public synchronized void setSessionExpiryInterval(final long sessionExpiryInterval) {
        if (!modified && this.sessionExpiryInterval == 0) {
            return;
        }
        this.sessionExpiryInterval = sessionExpiryInterval;
        final long configuredMaximum = configurationService.mqttConfiguration().maxSessionExpiryInterval();
        checkArgument(sessionExpiryInterval >= 0, "Session expiry interval must NOT be less than 0");
        checkArgument(
                sessionExpiryInterval < configuredMaximum,
                "Expiry interval must be less than the configured maximum of" + configuredMaximum);
        modified = true;
    }

    @Override
    public synchronized void setServerReference(final @NotNull String serverReference) {
        PluginBuilderUtil.checkServerReference(serverReference, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.serverReference, serverReference)) {
            return;
        }
        this.serverReference = serverReference;
        modified = true;
    }

    @Override
    public boolean isModified() {
        return modified || userProperties.isModified();
    }

    @NotNull
    @Override
    public String getServerReference() {
        return this.serverReference;
    }

    @NotNull
    @Override
    public DisconnectReasonCode getReasonCode() {
        return reasonCode;
    }

    @NotNull
    @Override
    public String getReasonString() {
        return this.reasonString;
    }

    @Override
    public long getSessionExpiryInterval() {
        return this.sessionExpiryInterval;
    }

    @Override
    public @NotNull ModifiableUserProperties getUserProperties() {
        return this.userProperties;
    }
}
