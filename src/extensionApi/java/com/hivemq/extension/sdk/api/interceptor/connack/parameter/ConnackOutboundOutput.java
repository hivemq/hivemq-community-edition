package com.hivemq.extension.sdk.api.interceptor.connack.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.connack.ModifiableConnackPacket;

/**
 * This is the output parameter of any {@link ConnackOutboundInterceptor}
 * providing methods to define the outcome of CONNACK interception.
 * <p>
 * It can be used to Modify an outbound CONNACK packet.
 *
 * @author Florian Limpöck
 * @since 4.2.0
 */
@DoNotImplement
public interface ConnackOutboundOutput extends AsyncOutput<ConnackOutboundOutput> {

    /**
     * Use this object to make any changes to the CONNACK message.
     *
     * @return A modifiable CONNACK packet.
     * @since 4.2.0
     */
    @NotNull
    ModifiableConnackPacket getConnackPacket();

}
