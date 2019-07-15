package com.hivemq.extension.sdk.api.interceptor.connack;


import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundOutput;

import java.time.Duration;

/**
 * Interface for the outbound CONNACK interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 *  When the method {@link #onOutboundConnack(ConnackOutboundInput, ConnackOutboundOutput)} throws an exception or a call to {@link ConnackOutboundOutput#async(Duration)} times out with {@link TimeoutFallback#FAILURE},
 *  then the connection will be closed by the broker without another packet being sent to the client.
 * @author Florian Limpöck
 * @since 4.2.0
 */
@FunctionalInterface
public interface ConnackOutboundInterceptor extends Interceptor {

    /**
     * When a {@link ConnackOutboundInterceptor} is set through any extension,
     * this method gets called for every outbound CONNACK packet from any MQTT client.
     *
     * @param connackOutboundInput  The {@link ConnackOutboundInput} parameter.
     * @param connackOutboundOutput The {@link ConnackOutboundOutput} parameter.
     * @since 4.2.0
     */
    void onOutboundConnack(@NotNull ConnackOutboundInput connackOutboundInput, @NotNull ConnackOutboundOutput connackOutboundOutput);
}
