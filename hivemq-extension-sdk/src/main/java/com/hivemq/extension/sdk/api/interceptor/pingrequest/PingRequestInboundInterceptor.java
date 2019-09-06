package com.hivemq.extension.sdk.api.interceptor.pingrequest;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingRequestInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingRequestInboundOutput;

/**
 * Interface for the ping request inbound interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 *
 * @author Robin Atherton
 */
public interface PingRequestInboundInterceptor extends Interceptor {

    /**
     * When a {@link PingRequestInboundInterceptor} is set through any extension, this method gets called for every
     * inbound PINGREQ packet from any MQTT client.
     *
     * @param pingRequestInboundInput  The {@link PingRequestInboundInput} parameter.
     * @param pingRequestInboundOutput The {@link PingRequestInboundOutput} parameter.
     */
    void onPingReq(
            @NotNull PingRequestInboundInput pingRequestInboundInput,
            @NotNull PingRequestInboundOutput pingRequestInboundOutput);

}
