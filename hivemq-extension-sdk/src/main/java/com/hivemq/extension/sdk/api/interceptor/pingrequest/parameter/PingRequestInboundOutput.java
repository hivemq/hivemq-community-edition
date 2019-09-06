package com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter;

import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.PingRequestInboundInterceptor;

/**
 * This is the output parameter of any {@link PingRequestInboundInterceptor}.
 *
 * @author Robin Atherton
 */
public interface PingRequestInboundOutput extends AsyncOutput<PingRequestInboundOutput> {

}
