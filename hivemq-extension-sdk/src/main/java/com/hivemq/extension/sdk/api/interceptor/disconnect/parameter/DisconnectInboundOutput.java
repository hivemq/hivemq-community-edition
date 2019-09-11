package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;

/**
 * This is the output parameter of any {@link DisconnectInboundInterceptor} providing methods to define the outcome of a
 * DISCONNECT interception.
 * <p>
 * It can be used to
 * <ul>
 * <li>Modify a DISCONNECT packet</li>
 * </ul>
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface DisconnectInboundOutput extends SimpleAsyncOutput<DisconnectInboundOutput> {

    /**
     * Use this object to make any changes to the DISCONNECT message.
     *
     * @return a {@link ModifiableDisconnectPacket} disconnect packet.
     */
    @NotNull ModifiableDisconnectPacket getDisconnectPacket();


}
