package com.hivemq.extensions.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

/**
 * @author Robin Atherton
 */
public class DisconnectPacketImpl implements DisconnectPacket {

    private final @NotNull DisconnectReasonCode reasonCode;
    private final @NotNull String serverReference;
    private final @NotNull String reasonString;
    private final @NotNull UserProperties userProperties;
    private final long sessionExpiryInterval;

    public DisconnectPacketImpl(
            @NotNull final DISCONNECT disconnect) {
        this.serverReference = disconnect.getServerReference();
        this.reasonString = disconnect.getReasonString();
        this.reasonCode = DisconnectReasonCode.valueOf(disconnect.getReasonCode().name());
        this.userProperties = disconnect.getUserProperties().getPluginUserProperties();
        this.sessionExpiryInterval = disconnect.getSessionExpiryInterval();
    }

    public DisconnectPacketImpl(final @NotNull DisconnectPacket disconnectPacket) {
        this.serverReference = disconnectPacket.getServerReference();
        this.reasonString = disconnectPacket.getReasonString();
        this.reasonCode = disconnectPacket.getReasonCode();
        this.userProperties = disconnectPacket.getUserProperties();
        this.sessionExpiryInterval = disconnectPacket.getSessionExpiryInterval();
    }

    public String getServerReference() {
        return this.serverReference;
    }

    @Override
    public DisconnectReasonCode getReasonCode() {
        return reasonCode;
    }

    public String getReasonString() {
        return this.reasonString;
    }

    public long getSessionExpiryInterval() {
        return this.sessionExpiryInterval;
    }

    public @NotNull UserProperties getUserProperties() {
        return userProperties;
    }

}
