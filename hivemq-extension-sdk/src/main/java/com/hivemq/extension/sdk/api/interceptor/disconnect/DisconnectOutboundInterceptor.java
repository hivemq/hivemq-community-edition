package com.hivemq.extension.sdk.api.interceptor.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;

public interface DisconnectOutboundInterceptor extends Interceptor {

    void onOutboundDisconnect(
            @NotNull DisconnectOutboundInput disconnectOutboundInput,
            @NotNull DisconnectOutboundOutput disconnectOutboundOutput);
}
