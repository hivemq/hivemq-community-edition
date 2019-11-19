package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableOutboundDisconnectPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link DisconnectOutboundInterceptor} providing methods to define the outcome of
 * DISCONNECT interception. It can be used to modify an outbound DISCONNECT packet.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface DisconnectOutboundOutput extends SimpleAsyncOutput<DisconnectOutboundOutput> {

    /**
     * Use this object to make any changes to the outbound DISCONNECT.
     *
     * @return A {@link ModifiableOutboundDisconnectPacket}.
     */
    @NotNull ModifiableOutboundDisconnectPacket getDisconnectPacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @NotNull Async<DisconnectOutboundOutput> async(@NotNull Duration timeout);
}
