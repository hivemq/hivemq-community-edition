package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;

import java.time.Duration;

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
public interface DisconnectInboundOutput extends AsyncOutput<DisconnectInboundOutput> {

    /**
     * Use this object to make any changes to the DISCONNECT message.
     *
     * @return a {@link ModifiableDisconnectPacket} disconnect packet.
     */
    @NotNull ModifiableDisconnectPacket getDisconnectPacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled either as failed or
     * successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout  Timeout that HiveMQ waits for the result of the async operation.
     * @param fallback Fallback behaviour if a timeout occurs. The outcome of the output for the fallback {@link
     *                 TimeoutFallback#SUCCESS} or {@link TimeoutFallback#FAILURE} is specified in the implementation.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<DisconnectInboundOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback fallback);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<DisconnectInboundOutput> async(@NotNull Duration timeout);

}
