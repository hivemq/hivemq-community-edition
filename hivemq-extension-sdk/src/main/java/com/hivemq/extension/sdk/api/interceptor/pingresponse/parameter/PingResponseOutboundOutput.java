package com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter;

import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingResponseOutboundInterceptor;

/**
 * This is the output parameter of any {@link PingResponseOutboundInterceptor}.
 *
 * @author Robin Atherton
 */
public interface PingResponseOutboundOutput extends AsyncOutput<PingResponseOutboundOutput> {
}
