package com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.pubcomp.ModifiablePubcompPacket;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link PubcompOutboundInterceptor}
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubcompOutboundOutput extends SimpleAsyncOutput<PubcompOutboundOutput> {

    /**
     * Use this object to make any changes to the PUBCOMP message.
     *
     * @return An modifiable {@link PubcompPacket}
     */
    @Immutable
    @NotNull ModifiablePubcompPacket getPubcompPacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed. This
     * means that the outcome results in an unmodified PUBCOMP sent to the client.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @Override
    @NotNull Async<PubcompOutboundOutput> async(@NotNull Duration timeout);

}
