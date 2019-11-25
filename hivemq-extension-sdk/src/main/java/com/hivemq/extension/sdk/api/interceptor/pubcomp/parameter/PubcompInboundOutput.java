package com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.pubcomp.ModifiablePubcompPacket;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link PubcompInboundInterceptor}
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubcompInboundOutput extends SimpleAsyncOutput<PubcompInboundOutput> {

    /**
     * Use this object to make any changes to the PUBCOMP message.
     *
     * @return An modifiable {@link PubcompPacket}
     */
    @NotNull ModifiablePubcompPacket getPubcompPacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed.
     * In that case an unmodified PUBCOMP is forwarded to the server, all changes made by this interceptor are not passed on.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @Override
    @NotNull Async<PubcompInboundOutput> async(@NotNull Duration timeout);
}
