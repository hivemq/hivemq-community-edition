package com.hivemq.extension.sdk.api.interceptor.pingrequestresponse;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pingrequestresponse.parameter.PingRequestResponseInput;
import com.hivemq.extension.sdk.api.interceptor.pingrequestresponse.parameter.PingRequestResponseOutput;

public interface PingRequestResponseInterceptor extends Interceptor {

    /**
     * When a {@link PingRequestResponseInterceptor} is set through any extension, this method gets called for every
     * outbound PINGRESP packet as well as every  inbound PINGREQ from any MQTT client.
     *
     * @param pingRequestResponseInput  The {@link PingRequestResponseInput} parameter.
     * @param pingRequestResponseOutput The {@link PingRequestResponseOutput} parameter.
     */
    void onPing(
            @NotNull PingRequestResponseInput pingRequestResponseInput,
            @NotNull PingRequestResponseOutput pingRequestResponseOutput);
}
