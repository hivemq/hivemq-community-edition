package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;

/**
 * @author Robin Atherton
 */
public interface DisconnectInboundOutput extends AsyncOutput<DisconnectInboundOutput> {

    /**
     *
     * @return the disconnect packet.
     */
    @NotNull DisconnectPacket getDisconnectPacket();

}
