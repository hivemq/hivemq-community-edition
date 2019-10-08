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
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Robin Atherton
 */
public class ModifiableInboundDisconnectPacketImpl implements ModifiableInboundDisconnectPacket {

    private final @NotNull FullConfigurationService configurationService;
    private boolean modified = false;
    private @NotNull DisconnectReasonCode reasonCode;


    private long sessionExpiryInterval;
    private @NotNull String reasonString;
    private @NotNull String serverReference;
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
    public synchronized void setReasonString(final @NotNull String reasonString) {
        Preconditions.checkNotNull(reasonString, "Reason string must never be null");
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.reasonString = reasonString;
        modified = true;
    }

    @Override
    public synchronized void setReasonCode(final @NotNull DisconnectReasonCode reasonCode) {
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        if (Objects.equals(this.reasonCode, reasonCode)) {
            return;
        }
        this.reasonCode = reasonCode;
        modified = true;
    }

    @Override
    public synchronized void setServerReference(final @NotNull String serverReference) {
        Preconditions.checkNotNull(serverReference, "Server reference must never be null");
        PluginBuilderUtil.checkServerReference(serverReference, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.serverReference, serverReference)) {
            return;
        }
        this.serverReference = serverReference;
        modified = true;
    }

    @Override
    public synchronized void setSessionExpiryInterval(final long sessionExpiryInterval) {
        if (sessionExpiryInterval == this.sessionExpiryInterval) {
            return;
        }
        checkState(this.sessionExpiryInterval != 0, "Session expiry interval must not be set when a client connected with session expiry interval = '0'");
        checkArgument(sessionExpiryInterval >= 0, "Session expiry interval must be greater than 0");
        final long configuredMaximum = configurationService.mqttConfiguration().maxSessionExpiryInterval();
        checkArgument(sessionExpiryInterval < configuredMaximum, "Session expiry interval must not be greater than the configured maximum of " + configuredMaximum);
        this.sessionExpiryInterval = sessionExpiryInterval;
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
