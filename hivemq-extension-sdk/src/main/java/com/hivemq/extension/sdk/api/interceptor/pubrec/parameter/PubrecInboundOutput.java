package com.hivemq.extension.sdk.api.interceptor.pubrec.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.pubrec.ModifiablePubrecPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link PubrecInboundInterceptor}
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubrecInboundOutput extends SimpleAsyncOutput<PubrecInboundOutput> {

    /**
     * Use this object to make any changes to the PUBREC message.
     *
     * @return A modifiable {@link ModifiablePubrecPacket}
     */
    @NotNull ModifiablePubrecPacket getPubrecPacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed.
     * In that case an unmodified PUBREC is forwarded to the server, all changes made by this interceptor are not passed on.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @Override
    @NotNull Async<PubrecInboundOutput> async(@NotNull Duration timeout);
}