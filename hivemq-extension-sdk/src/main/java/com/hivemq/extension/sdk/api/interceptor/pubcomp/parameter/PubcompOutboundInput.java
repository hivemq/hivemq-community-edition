package com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter for any {@link PubcompOutboundInterceptor} providing PUBCOMP information.
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubcompOutboundInput extends ClientBasedInput {

    /**
     * The unmodifiable PUBCOMP packet that was intercepted.
     *
     * @return An unmodifiable {@link PubcompPacket}.
     */
    @NotNull @Immutable PubcompPacket getPubcompPacket();

}
