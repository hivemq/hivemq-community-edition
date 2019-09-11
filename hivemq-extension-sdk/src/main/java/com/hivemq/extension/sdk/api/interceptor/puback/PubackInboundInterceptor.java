package com.hivemq.extension.sdk.api.interceptor.puback;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundOutput;

import java.time.Duration;

/**
 * Interface for the inbound PUBACK interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 * When the method {@link #onInboundPuback(PubackInboundInput, PubackInboundOutput)} throws an exception or a call
 * to {@link PubackInboundOutput#async(Duration)} times out with {@link TimeoutFallback#FAILURE},
 * the exception will be logged and the PUBACK will be sent to the server without any changes.
 *
 * @author Yannick Weber
 */
@FunctionalInterface
public interface PubackInboundInterceptor extends Interceptor {

    /**
     * When a {@link PubackInboundInterceptor} is set through any extension,
     * this method gets called for every inbound PUBACK packet from any MQTT client.
     *
     * @param pubackInboundInput  The {@link PubackInboundInput} parameter.
     * @param pubackInboundOutput The {@link PubackInboundOutput} parameter.
     */
    void onInboundPuback(
            @NotNull PubackInboundInput pubackInboundInput, @NotNull PubackInboundOutput pubackInboundOutput);

}
