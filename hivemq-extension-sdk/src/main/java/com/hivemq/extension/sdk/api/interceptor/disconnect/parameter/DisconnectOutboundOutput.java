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
     * @return a {@link ModifiableDisconnectPacket}
     */
    @NotNull ModifiableDisconnectPacket getDisconnectPacket();

    /**
     * If the timeout expires before {@link Async#resume()} is called then the outcome is handled either as failed or
     * successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     * <p>
     * {@link TimeoutFallback#FAILURE} results in closed connection without a DISCONNECT sent to the client.
     * <p>
     * {@link TimeoutFallback#SUCCESS} will proceed the DISCONNECT.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @param fallback Fallback behavior if a timeout occurs.
     * @throws UnsupportedOperationException if async is called more than once.
     */
    @NotNull Async<DisconnectOutboundOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback fallback);

}
