package com.hivemq.extension.sdk.api.interceptor.pingrequest;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingReqInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingReqInboundOutput;

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
@FunctionalInterface
public interface PingReqInboundInterceptor extends Interceptor {

    /**
     * When a {@link PingReqInboundInterceptor} is set through any extension, this method gets called for every
     * inbound PINGREQ packet from any MQTT client.
     *
     * @param pingReqInboundInput  The {@link PingReqInboundInput} parameter.
     * @param pingReqInboundOutput The {@link PingReqInboundOutput} parameter.
     */
    void onInboundPingReq(
            @NotNull PingReqInboundInput pingReqInboundInput,
            @NotNull PingReqInboundOutput pingReqInboundOutput);

}
