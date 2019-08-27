package com.hivemq.extension.sdk.api.interceptor.unsubscribe;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;

/**
 * Interface for the inbound UNSUBSCRIBE interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 *
 * @author Robin Atherton
 */
public interface UnsubscribeInboundInterceptor extends Interceptor {

    /**
     * When a {@link UnsubscribeInboundInterceptor} is set through any extension, this method gets called for every
     * inbound UNSUBSCRIBE packet from any client.
     *
     * @param unsubscribeInboundInput  The {@link UnsubscribeInboundInput} parameter.
     * @param unsubscribeInboundOutput The {@link UnsubscribeInboundOutput} parameter.
     */
    void onInboundUnsubscribe(
            @NotNull UnsubscribeInboundInput unsubscribeInboundInput,
            @NotNull UnsubscribeInboundOutput unsubscribeInboundOutput);
}
