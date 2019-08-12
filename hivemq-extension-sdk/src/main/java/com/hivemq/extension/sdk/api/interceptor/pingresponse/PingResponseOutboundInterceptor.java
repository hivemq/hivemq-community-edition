package com.hivemq.extension.sdk.api.interceptor.pingresponse;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingResponseOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingResponseOutboundOutput;

/**
 * @author Robin Atherton
 * @since 4.2.0
 */
public interface PingResponseOutboundInterceptor extends Interceptor {

    /**
     * When a {@link PingResponseOutboundInterceptor} is set through any extension,
     * this method gets called for every outbound PINGRESP packet from any MQTT client.
     *
     * @param pingResponseOutboundInput  The {@link PingResponseOutboundInput} parameter.
     * @param pingResponseOutboundOutput The {@link PingResponseOutboundOutput} parameter.
     * @since 4.2.0
     */
    void onPingResp(@NotNull PingResponseOutboundInput pingResponseOutboundInput, @NotNull PingResponseOutboundOutput pingResponseOutboundOutput);

}