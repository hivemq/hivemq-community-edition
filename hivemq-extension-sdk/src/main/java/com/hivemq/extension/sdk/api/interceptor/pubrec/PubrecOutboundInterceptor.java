package com.hivemq.extension.sdk.api.interceptor.pubrec;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundOutput;

import java.time.Duration;

/**
 * Interface for the outbound PUBREC interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 * When the method {@link #onOutboundPubrec(PubrecOutboundInput, PubrecOutboundOutput)} throws an exception or a call to
 * {@link PubrecOutboundOutput#async(Duration)} times out with {@link TimeoutFallback#FAILURE}, then an error will be
 * logged. The connection will not be terminated and the original PUBREC will be sent to the client.
 *
 * @author Yannick Weber
 */
public interface PubrecOutboundInterceptor extends Interceptor {

    /**
     * When a {@link PubrecOutboundInterceptor} is set through any extension, this method gets called for every outbound
     * PUBREC packet for any MQTT client.
     *
     * @param pubrecOutboundInput  The {@link PubrecOutboundInput} parameter.
     * @param pubrecOutboundOutput The {@link PubrecOutboundOutput} parameter.
     */
    void onOutboundPubrec(
            @NotNull PubrecOutboundInput pubrecOutboundInput, @NotNull PubrecOutboundOutput pubrecOutboundOutput);

}
