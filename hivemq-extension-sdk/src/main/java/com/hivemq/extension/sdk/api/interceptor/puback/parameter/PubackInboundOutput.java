package com.hivemq.extension.sdk.api.interceptor.puback.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.puback.ModifiablePubackPacket;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link PubackInboundInterceptor}
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubackInboundOutput extends SimpleAsyncOutput<PubackInboundOutput> {

    /**
     * Use this object to make any changes to the PUBACK message.
     *
     * @return An modifiable {@link PubackPacket}
     */
    @Immutable
    @NotNull ModifiablePubackPacket getPubackPacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed.
     * In that case an unmodified PUBACK is forwarded to the server, all changes made by this interceptor are not passed on.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @Override
    @NotNull Async<PubackInboundOutput> async(@NotNull Duration timeout);
}
