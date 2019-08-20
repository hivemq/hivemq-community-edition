package com.hivemq.extensions.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

/**
 * @author Robin Atherton
 */
public class DisconnectPacketImpl implements DisconnectPacket {

    private final @NotNull DISCONNECT disconnect;

    public DisconnectPacketImpl(
            @NotNull final DISCONNECT disconnect) {
        this.disconnect = disconnect;
    }

    public DISCONNECT getDisconnect() {
        return disconnect;
    }

    public String getServerReference() {
        return disconnect.getServerReference();
    }

    public String getReasonString() {
        return disconnect.getReasonString();
    }

    public long getSessionExpiryInterval() {
        return disconnect.getSessionExpiryInterval();
    }

    public @NotNull UserProperties getUserProperties() {
        return disconnect.getUserProperties().getPluginUserProperties();
    }

}
