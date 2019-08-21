package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;

/**
 *
 *
 *
 * @author Robin Atherton
 */
public interface DisconnectInboundOutput extends AsyncOutput<DisconnectInboundOutput> {

    /**
     *  Use this object to make any changes to the DISCONNECT message.
     *
     * @return a modifiable disconnect packet.
     */
    @NotNull ModifiableDisconnectPacket getDisconnectPacket();

}
