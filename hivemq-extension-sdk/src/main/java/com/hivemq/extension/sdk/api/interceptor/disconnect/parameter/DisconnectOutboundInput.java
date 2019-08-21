package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link DisconnectOutboundInterceptor} providing DISCONNECT. //TODO more
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface DisconnectOutboundInput extends ClientBasedInput {

    /**
     * //TODO Write appropriate DOC
     */
    @Immutable
    @NotNull DisconnectPacket getDisconnectPacket();

}
