package com.hivemq.extension.sdk.api.interceptor.pubrec.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.pubrec.ModifiablePubrecPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link PubrecOutboundInterceptor}
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubrecOutboundOutput extends SimpleAsyncOutput<PubrecOutboundOutput> {

    /**
     * Use this object to make any changes to the PUBREC message.
     *
     * @return A modifiable {@link ModifiablePubrecPacket}
     */
    @Immutable
    @NotNull ModifiablePubrecPacket getPubrecPacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed.
     * In that case an unmodified PUBREC is forwarded to the client, all changes made by this interceptor are not passed on.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @Override
    @NotNull Async<PubrecOutboundOutput> async(@NotNull Duration timeout);

}
