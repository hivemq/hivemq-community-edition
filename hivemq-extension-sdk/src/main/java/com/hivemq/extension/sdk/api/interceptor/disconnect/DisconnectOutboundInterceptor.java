package com.hivemq.extension.sdk.api.interceptor.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;

/**
 * Interface for the outbound DISCONNECT interception.
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
public interface DisconnectOutboundInterceptor extends Interceptor {

    /**
     * When a {@link DisconnectOutboundInterceptor} is set through any extension, this method gets called for every
     * outbound DISCONNECT packet from any MQTT client.
     *
     * @param disconnectOutboundInput  The {@link DisconnectOutboundInput} parameter.
     * @param disconnectOutboundOutput The {@link DisconnectOutboundOutput} parameter.
     */
    void onOutboundDisconnect(
            @NotNull DisconnectOutboundInput disconnectOutboundInput,
            @NotNull DisconnectOutboundOutput disconnectOutboundOutput);
}
