package com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link UnsubscribeInboundInterceptor} providing methods to define the outcome of
 * UNSUBSCRIBE interception.
 * <p>
 * It can be used to modify an inbound UNSUBSCRIBE packet.
 * <p>
 *
 * @author Robin Atherton
 */
public interface UnsubscribeInboundOutput extends AsyncOutput<UnsubscribeInboundOutput> {

    /**
     * Use this Object to make any changes to the inbound UNSUBSCRIBE.
     *
     * @return a {@link ModifiableUnsubscribePacket}.
     */
    @NotNull ModifiableUnsubscribePacket getUnsubscribePacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed. In that
     * case an unmodified UNSUBSCRIBE is forwarded to the server and all changes made by this interceptor are
     * discarded.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.3.0
     */
    @NotNull Async<UnsubscribeInboundOutput> async(@NotNull Duration timeout);

}
