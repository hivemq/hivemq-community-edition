package com.hivemq.extension.sdk.api.interceptor.pingresponse;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingResponseOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingResponseOutboundOutput;

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
public interface PingResponseOutboundInterceptor extends Interceptor {

    /**
     * When a {@link PingResponseOutboundInterceptor} is set through any extension, this method gets called for every
     * outbound PINGRESP packet from any MQTT client.
     *
     * @param pingResponseOutboundInput  The {@link PingResponseOutboundInput} parameter.
     * @param pingResponseOutboundOutput The {@link PingResponseOutboundOutput} parameter.
     */
    void onPingResp(
            @NotNull PingResponseOutboundInput pingResponseOutboundInput,
            @NotNull PingResponseOutboundOutput pingResponseOutboundOutput);

}