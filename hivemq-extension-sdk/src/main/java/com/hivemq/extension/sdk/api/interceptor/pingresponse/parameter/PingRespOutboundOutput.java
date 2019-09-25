package com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter;

import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingRespOutboundInterceptor;

/**
 * This is the output parameter of any {@link PingRespOutboundInterceptor}.
 *
 * @author Robin Atherton
 */
public interface PingRespOutboundOutput extends SimpleAsyncOutput<PingRespOutboundOutput> {
}
