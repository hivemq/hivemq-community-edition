package com.hivemq.extension.sdk.api.interceptor.pubrec;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecInboundOutput;

import java.time.Duration;

/**
 * Interface for the outbound PUBREC interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 * When the method {@link #onInboundPubrec(PubrecInboundInput, PubrecInboundOutput)} throws an exception or a call to
 * {@link PubrecInboundOutput#async(Duration)} times out with {@link TimeoutFallback#FAILURE}, then an error will be
 * logged. The connection will not be terminated and the original PUBREC will be sent to the server.
 *
 * @author Yannick Weber
 */
public interface PubrecInboundInterceptor extends Interceptor {

    /**
     * When a {@link PubrecInboundInterceptor} is set through any extension, this method gets called for every inbound
     * PUBREC packet from any MQTT client.
     *
     * @param pubrecInboundInput  The {@link PubrecInboundInput} parameter.
     * @param pubrecInboundOutput The {@link PubrecInboundOutput} parameter.
     */
    void onInboundPubrec(
            @NotNull PubrecInboundInput pubrecInboundInput, @NotNull PubrecInboundOutput pubrecInboundOutput);

}
