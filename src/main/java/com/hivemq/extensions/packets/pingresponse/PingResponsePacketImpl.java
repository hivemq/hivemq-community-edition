package com.hivemq.extensions.packets.pingresponse;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.pingresponse.PingResponsePacket;
import com.hivemq.mqtt.message.PINGRESP;

/**
 * @author Robin Atherton
 * @since 4.2.0
 */
public class PingResponsePacketImpl implements PingResponsePacket {

    private final @NotNull PINGRESP pingresp;

    public PingResponsePacketImpl(final @NotNull PINGRESP pingresp) {
        this.pingresp = pingresp;
    }

    public PINGRESP getPingresp() {
        return pingresp;
    }
}
