package com.hivemq.extension.sdk.api.interceptor.puback;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundOutput;

import java.time.Duration;

/**
 * Interface for the outbound PUBACK interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 * When the method {@link #onOutboundPuback(PubackOutboundInput, PubackOutboundOutput)} throws an exception or a call
 * to {@link PubackOutboundOutput#async(Duration)} times out with {@link TimeoutFallback#FAILURE},
 * the exception will be logged and the PUBACK will be sent to the client without any changes.
 *
 * @author Yannick Weber
 */
public interface PubackOutboundInterceptor extends Interceptor {
    
    /**
     * When a {@link PubackOutboundInterceptor} is set through any extension,
     * this method gets called for every outgoing PUBACK packet from any MQTT client.
     *
     * @param pubackOutboundInput  The {@link PubackOutboundInput} parameter.
     * @param pubackOutboundOutput The {@link PubackOutboundOutput} parameter.
     */
    void onOutboundPuback(
            @NotNull PubackOutboundInput pubackOutboundInput, @NotNull PubackOutboundOutput pubackOutboundOutput);

}
