package com.hivemq.extensions.packets.disconnect;

import com.hivemq.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Robin Atherton
 */
public class ModifiableDisconnectPacketImpl implements ModifiableDisconnectPacket {

    private final @NotNull FullConfigurationService configurationService;
    private boolean modified = false;

    private long sessionExpiryInterval;
    private String reasonString;
    private String serverReference;
    private final @Nullable ModifiableUserPropertiesImpl userProperties;

    public ModifiableDisconnectPacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull DISCONNECT originalDisconnect) {
        this.configurationService = fullConfigurationService;
        this.userProperties = new ModifiableUserPropertiesImpl(
                originalDisconnect.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
        this.reasonString = originalDisconnect.getReasonString();
        this.sessionExpiryInterval = originalDisconnect.getSessionExpiryInterval();
        this.serverReference = originalDisconnect.getServerReference();
    }

    @Override
    public synchronized void setReasonString(final @NotNull String reasonString) {
        this.reasonString = reasonString;
        modified = true;
    }

    @Override
    public synchronized void setSessionExpiryInterval(final long sessionExpiryInterval) {
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
        this.serverReference = serverReference;
        modified = true;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public String getServerReference() {
        return this.serverReference;
    }

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
