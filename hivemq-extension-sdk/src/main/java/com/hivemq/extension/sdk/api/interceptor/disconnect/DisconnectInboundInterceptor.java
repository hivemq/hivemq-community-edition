package com.hivemq.extension.sdk.api.interceptor.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundOutput;

/**
 * Interface for the inbound DISCONNECT interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * </p>
 *
 * @author Robin Atherton
 */
@FunctionalInterface
public interface DisconnectInboundInterceptor extends Interceptor {

    /**
     * When a {@link DisconnectInboundInterceptor} is set through any extension, this method gets called for every
     * inbound DISCONNECT packet from any MQTT client.
     *
     * @param disconnectInboundInput  The {@link DisconnectInboundInput} parameter.
     * @param disconnectInboundOutput The {@link DisconnectInboundOutput} parameter.
     */
    void onInboundDisconnect(
            @NotNull DisconnectInboundInput disconnectInboundInput,
            @NotNull DisconnectInboundOutput disconnectInboundOutput);
}
