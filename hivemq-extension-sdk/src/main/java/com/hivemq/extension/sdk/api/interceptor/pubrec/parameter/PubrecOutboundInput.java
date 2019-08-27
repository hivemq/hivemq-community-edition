package com.hivemq.extension.sdk.api.interceptor.pubrec.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter for any {@link PubrecOutboundInterceptor} providing PUBREC information.
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubrecOutboundInput extends ClientBasedInput {

    /**
     * The unmodifiable PUBREC packet that was intercepted.
     *
     * @return An unmodifiable {@link PubrecPacket}.
     */
    @NotNull @Immutable PubrecPacket getPubrecPacket();
}
