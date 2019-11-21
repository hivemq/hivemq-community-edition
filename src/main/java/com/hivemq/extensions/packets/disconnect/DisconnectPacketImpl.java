package com.hivemq.extensions.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.Optional;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
public class DisconnectPacketImpl implements DisconnectPacket {

    private final @NotNull DisconnectReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final long sessionExpiryInterval;
    private final @Nullable String serverReference;
    private final @NotNull UserProperties userProperties;

    public DisconnectPacketImpl(final @NotNull DISCONNECT disconnect) {
        reasonCode = DisconnectReasonCode.valueOf(disconnect.getReasonCode().name());
        reasonString = disconnect.getReasonString();
        sessionExpiryInterval = disconnect.getSessionExpiryInterval();
        serverReference = disconnect.getServerReference();
        userProperties = disconnect.getUserProperties().getPluginUserProperties();
    }

    public DisconnectPacketImpl(final @NotNull DisconnectPacket disconnectPacket) {
        reasonCode = disconnectPacket.getReasonCode();
        reasonString = disconnectPacket.getReasonString().orElse(null);
        serverReference = disconnectPacket.getServerReference().orElse(null);
        sessionExpiryInterval = disconnectPacket.getSessionExpiryInterval().orElse(Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET);
        userProperties = disconnectPacket.getUserProperties();
    }

    @Override
    public @NotNull DisconnectReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public @NotNull Optional<Long> getSessionExpiryInterval() {
        return (sessionExpiryInterval == Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET) ? Optional.empty() :
                Optional.of(sessionExpiryInterval);
    }

    @Override
    public @NotNull Optional<String> getServerReference() {
        return Optional.ofNullable(serverReference);
    }

    @Override
    public @NotNull UserProperties getUserProperties() {
        return userProperties;
    }
}
