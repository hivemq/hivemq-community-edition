package com.hivemq.extension.sdk.api.interceptor.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;

/**
 * Interface for the outbound DISCONNECT interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from th4e same client. If the same instance is
 * shared between multiple clients it can be called in different Threads and must therefore be thread-safe.
 * </p>
 *
 * @author Robin Atherton
 */
@FunctionalInterface
public interface DisconnectOutboundInterceptor extends Interceptor {

    void onOutboundDisconnect(
            @NotNull DisconnectOutboundInput disconnectOutboundInput,
            @NotNull DisconnectOutboundOutput disconnectOutboundOutput);
}
