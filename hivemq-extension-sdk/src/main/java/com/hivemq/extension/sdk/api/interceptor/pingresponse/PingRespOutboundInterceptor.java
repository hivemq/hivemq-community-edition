package com.hivemq.extension.sdk.api.interceptor.pingresponse;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingRespOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingRespOutboundOutput;

/**
 * Interface for the ping response interception.
 * <p>
 * Interceptors are always called by the same thread for all messages for the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called by different threads and therefore must be
 * thread-safe.
 * <p>
 *
 * @author Robin Atherton
 */
@FunctionalInterface
public interface PingRespOutboundInterceptor extends Interceptor {

    /**
     * When a {@link PingRespOutboundInterceptor} is set through any extension, this method gets called for every
     * outbound PINGRESP packet from any MQTT client.
     *
     * @param pingRespOutboundInput  The {@link PingRespOutboundInput} parameter.
     * @param pingRespOutboundOutput The {@link PingRespOutboundOutput} parameter.
     */
    void onOutboundPingResp(
            @NotNull PingRespOutboundInput pingRespOutboundInput,
            @NotNull PingRespOutboundOutput pingRespOutboundOutput);

}