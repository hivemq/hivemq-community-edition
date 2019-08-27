package com.hivemq.extension.sdk.api.interceptor.puback.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter for any {@link PubackInboundInterceptor}
 * providing PUBACK information.
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubackInboundInput extends ClientBasedInput {

    /**
     * The unmodifiable PUBACK packet that was intercepted.
     *
     * @return An unmodifiable {@link PubackPacket}.
     */
    @NotNull @Immutable PubackPacket getPubackPacket();

}
