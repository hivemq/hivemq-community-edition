package com.hivemq.extension.sdk.api.interceptor.pubrel.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter for any {@link PubrelInboundInterceptor} providing PUBREL information.
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubrelInboundInput extends ClientBasedInput {

    /**
     * The unmodifiable PUBREL packet that was intercepted.
     *
     * @return An unmodifiable {@link PubrelPacket}.
     */
    @Immutable @NotNull PubrelPacket getPubrelPacket();
}
