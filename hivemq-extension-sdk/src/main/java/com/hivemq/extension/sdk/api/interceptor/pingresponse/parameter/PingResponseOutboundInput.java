package com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter;

import com.hivemq.extension.sdk.api.interceptor.pingresponse.PingResponseOutboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link PingResponseOutboundInterceptor}
 * providing PINGRESP
 *
 * @author Robin Atherton
 */
public interface PingResponseOutboundInput extends ClientBasedInput {


}
