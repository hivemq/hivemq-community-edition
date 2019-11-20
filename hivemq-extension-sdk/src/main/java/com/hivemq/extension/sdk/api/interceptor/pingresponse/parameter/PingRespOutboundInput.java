package com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingRespOutboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link PingRespOutboundInterceptor}.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface PingRespOutboundInput extends ClientBasedInput {


}
