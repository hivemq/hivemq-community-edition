package com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.PingReqInboundInterceptor;

/**
 * This is the output parameter of any {@link PingReqInboundInterceptor}.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface PingReqInboundOutput extends SimpleAsyncOutput<PingReqInboundOutput> {

}
