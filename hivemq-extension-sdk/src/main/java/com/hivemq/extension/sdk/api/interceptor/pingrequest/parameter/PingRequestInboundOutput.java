package com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter;

import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.PingRequestInboundInterceptor;

/**
 * This is the output parameter of any {@link PingRequestInboundInterceptor}.
 *
 * @author Robin Atherton
 */
public interface PingRequestInboundOutput extends SimpleAsyncOutput<PingRequestInboundOutput> {

}
