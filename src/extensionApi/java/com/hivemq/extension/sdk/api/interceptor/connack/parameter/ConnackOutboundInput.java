package com.hivemq.extension.sdk.api.interceptor.connack.parameter;


import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.connack.ConnackPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link ConnackOutboundInterceptor}
 * providing CONNACK, connection and client based information.
 *
 * @author Florian Limpöck
 * @since 4.2.0
 */
@DoNotImplement
public interface ConnackOutboundInput extends ClientBasedInput {

    /**
     * The unmodifiable CONNACK packet that was intercepted.
     *
     * @return An unmodifiable {@link ConnackPacket}.
     * @since 4.2.0
     */
    @NotNull
    @Immutable
    ConnackPacket getConnackPacket();

}
