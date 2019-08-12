package com.hivemq.extensions.packets.pingrequest;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.pingrequest.PingRequestPacket;
import com.hivemq.mqtt.message.PINGREQ;
/**
 * @author Robin Atherton
 * @since 4.2.0
 */
public class PingRequestPacketImpl implements PingRequestPacket {

    private final @NotNull PINGREQ pingreq;

    public PingRequestPacketImpl(@NotNull final PINGREQ pingreq) {
        this.pingreq = pingreq;
    }

    public PINGREQ getPingreq() {
        return pingreq;
    }
}
