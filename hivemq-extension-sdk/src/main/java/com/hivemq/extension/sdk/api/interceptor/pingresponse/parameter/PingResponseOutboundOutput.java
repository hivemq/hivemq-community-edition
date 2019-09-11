package com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter;

import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingResponseOutboundInterceptor;

/**
 * This is the output parameter of any {@link PingResponseOutboundInterceptor}.
 *
 * @author Robin Atherton
 */
public interface PingResponseOutboundOutput extends SimpleAsyncOutput<PingResponseOutboundOutput> {
}
