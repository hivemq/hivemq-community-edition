package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link DisconnectOutboundInterceptor} providing DISCONNECT information.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface DisconnectOutboundInput extends ClientBasedInput {

    /**
     * The unmodifiable DISCONNECT packet that was intercepted.
     *
     * @return An unmodifiable {@link DisconnectPacket}.
     */
    @Immutable
    @NotNull DisconnectPacket getDisconnectPacket();

}
