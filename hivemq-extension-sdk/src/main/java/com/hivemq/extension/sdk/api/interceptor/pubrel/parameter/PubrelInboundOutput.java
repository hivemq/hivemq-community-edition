package com.hivemq.extension.sdk.api.interceptor.pubrel.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.pubrel.ModifiablePubrelPacket;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link PubrelInboundInterceptor}
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubrelInboundOutput extends SimpleAsyncOutput<PubrelInboundOutput> {

    /**
     * Use this object to make any changes to the PUBREL message.
     *
     * @return An modifiable {@link PubrelPacket}
     */
    @Immutable
    @NotNull ModifiablePubrelPacket getPubrelPacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed. This
     * means that the outcome results an unmodified PUBREL sent to the server.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @Override
    @NotNull Async<PubrelInboundOutput> async(@NotNull Duration timeout);
}
