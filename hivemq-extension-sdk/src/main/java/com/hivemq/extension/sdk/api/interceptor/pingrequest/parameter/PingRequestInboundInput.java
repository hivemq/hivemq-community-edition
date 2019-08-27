package com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter;

import com.hivemq.extension.sdk.api.interceptor.pingrequest.PingRequestInboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link PingRequestInboundInterceptor}
 * providing PINGREQ
 *
 * @author Robin Atherton
 */
public interface PingRequestInboundInput extends ClientBasedInput {

}