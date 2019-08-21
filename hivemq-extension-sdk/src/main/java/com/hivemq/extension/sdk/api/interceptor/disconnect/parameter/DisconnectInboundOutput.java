package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;

/**
 * This is the output parameter of any {@link ConnectInboundInterceptor} providing methods to define the outcome of a
 * DISCONNECT interception.
 * <p>
 * It can be used to
 * <ul>
 * <li>Modify a DISCONNECT packet</li>
 * </ul>
 *
 * @author Robin Atherton
 */
public interface DisconnectInboundOutput extends AsyncOutput<DisconnectInboundOutput> {

    /**
     * Use this object to make any changes to the DISCONNECT message.
     *
     * @return a modifiable disconnect packet.
     */
    @NotNull ModifiableDisconnectPacket getDisconnectPacket();

}
