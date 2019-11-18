package com.hivemq.extension.sdk.api.interceptor.pubrel;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundOutput;

import java.time.Duration;

/**
 * Interface for the inbound PUBREL interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 * When the method {@link #onInboundPubrel(PubrelInboundInput, PubrelInboundOutput)} throws an exception or a call to
 * {@link PubrelInboundOutput#async(Duration)} times out with {@link TimeoutFallback#FAILURE}, the exception will be
 * logged and the PUBREL will be sent to the server without any changes.
 *
 * @author Yannick Weber
 */
@FunctionalInterface
public interface PubrelInboundInterceptor extends Interceptor {

    /**
     * When a {@link PubrelInboundInterceptor} is set through any extension, this method gets called for every inbound
     * PUBREL packet from any MQTT client.
     *
     * @param pubrelInboundInput  The {@link PubrelInboundInput} parameter.
     * @param pubrelInboundOutput The {@link PubrelInboundOutput} parameter.
     */
    void onInboundPubrel(
            @NotNull PubrelInboundInput pubrelInboundInput,
            @NotNull PubrelInboundOutput pubrelInboundOutput);
}
