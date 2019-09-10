package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link DisconnectInboundInterceptor} providing DISCONNECT information.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface DisconnectInboundInput extends ClientBasedInput {

    /**
     * The unmodifiable DISCONNECT packet that was intercepted.
     *
     * @return an unmodifiable {@link DisconnectPacket}.
     */
    @Immutable
    @NotNull DisconnectPacket getDisconnectPacket();

}
