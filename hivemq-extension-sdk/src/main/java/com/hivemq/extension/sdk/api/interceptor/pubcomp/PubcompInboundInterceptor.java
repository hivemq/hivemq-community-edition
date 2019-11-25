package com.hivemq.extension.sdk.api.interceptor.pubcomp;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundOutput;

import java.time.Duration;

/**
 * Interface for the inbound PUBCOMP interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 * When the method {@link #onInboundPubcomp(PubcompInboundInput, PubcompInboundOutput)} throws an exception or a call to
 * {@link PubcompInboundOutput#async(Duration)} times out with {@link TimeoutFallback#FAILURE}, the exception will be
 * logged and the PUBCOMP will be sent to the server without any changes.
 *
 * @author Yannick Weber
 */
@FunctionalInterface
public interface PubcompInboundInterceptor extends Interceptor {

    /**
     * When a {@link PubcompInboundInterceptor} is set through any extension, this method gets called for every inbound
     * PUBCOMP packet from any MQTT client.
     *
     * @param pubcompInboundInput  The {@link PubcompInboundInput} parameter.
     * @param pubcompInboundOutput The {@link PubcompInboundOutput} parameter.
     */
    void onInboundPubcomp(
            @NotNull PubcompInboundInput pubcompInboundInput,
            @NotNull PubcompInboundOutput pubcompInboundOutput);
}
