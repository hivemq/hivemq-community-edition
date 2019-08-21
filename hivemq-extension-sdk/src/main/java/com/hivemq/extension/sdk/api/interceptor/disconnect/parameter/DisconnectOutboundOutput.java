package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link DisconnectOutboundInterceptor} providing methods to define the outcome of
 * DISCONNECT interception. It can be used to modify an outbound DISCONNECT packet.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface DisconnectOutboundOutput {

    /**
     * Use this object to make changes to the outbound DISCONNECT.
     *
     * @return A modifiable DISCONNECT packet.
     */
    @NotNull ModifiableDisconnectPacket getDisconnectPacket();

    /**
     * //TODO Write appropriate DOC
     *
     * @param timeout
     * @param timeoutFallback
     * @return
     */
    @NotNull Async<DisconnectOutboundOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback);

}
