package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;

/**
 * This is the output parameter of any {@link DisconnectOutboundInterceptor} providing methods to define the outcome of
 * DISCONNECT interception. It can be used to modify an outbound DISCONNECT packet.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface DisconnectOutboundOutput extends SimpleAsyncOutput<DisconnectOutboundOutput> {

    /**
     * Use this object to make changes to the outbound DISCONNECT.
     *
     * @return a {@link ModifiableDisconnectPacket}
     */
    @NotNull ModifiableDisconnectPacket getDisconnectPacket();


}
