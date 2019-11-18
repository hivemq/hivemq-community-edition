package com.hivemq.extensions.packets.disconnect;

import com.google.common.base.Preconditions;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableInboundDisconnectPacket;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Robin Atherton
 */
public class ModifiableInboundDisconnectPacketImpl implements ModifiableInboundDisconnectPacket {

    private final @NotNull FullConfigurationService configurationService;

    private @NotNull DisconnectReasonCode reasonCode;
    private @Nullable String reasonString;
    private long sessionExpiryInterval;
    private final long originalSessionExpiryInterval;
    private final @Nullable String serverReference;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private boolean modified = false;

    public ModifiableInboundDisconnectPacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService,
            final @NotNull DISCONNECT originalDisconnect) {

        this.configurationService = fullConfigurationService;
        this.reasonCode = DisconnectReasonCode.valueOf(originalDisconnect.getReasonCode().name());
        this.reasonString = originalDisconnect.getReasonString();
        this.sessionExpiryInterval = originalDisconnect.getSessionExpiryInterval();
        originalSessionExpiryInterval = sessionExpiryInterval; // FIXME wrong
        this.serverReference = originalDisconnect.getServerReference();
        this.userProperties = new ModifiableUserPropertiesImpl(
                originalDisconnect.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    @Override
    public @NotNull DisconnectReasonCode getReasonCode() {
        return reasonCode;
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
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public synchronized void setReasonString(final @Nullable String reasonString) {
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.reasonString = reasonString;
        modified = true;
    }

    @Override
    public @NotNull Optional<Long> getSessionExpiryInterval() {
        return (sessionExpiryInterval == Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET) ? Optional.empty() :
                Optional.of(sessionExpiryInterval);
    }

    @Override
    public synchronized void setSessionExpiryInterval(final @Nullable Long sessionExpiryInterval) {
        final long interval =
                (sessionExpiryInterval == null) ? Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET : sessionExpiryInterval;
        if (this.sessionExpiryInterval == interval) {
            return;
        }
        checkState( // FIXME wrong
                originalSessionExpiryInterval != 0,
                "Session expiry interval must not be set when a client connected with session expiry interval = '0'");
        checkArgument(interval >= 0, "Session expiry interval must be greater than 0");
        final long configuredMaximum = configurationService.mqttConfiguration().maxSessionExpiryInterval();
        checkArgument(
                interval < configuredMaximum,
                "Session expiry interval must not be greater than the configured maximum of " +
                        configuredMaximum); // TODO
        this.sessionExpiryInterval = interval;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getServerReference() {
        return Optional.ofNullable(serverReference);
    }

    @Override
    public @NotNull ModifiableUserProperties getUserProperties() {
        return this.userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }
}
