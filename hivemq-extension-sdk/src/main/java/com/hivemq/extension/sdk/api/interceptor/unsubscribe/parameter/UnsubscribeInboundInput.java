package com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.unsubscribe.UnsubscribePacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link UnsubscribeInboundInterceptor} providing UNSUBSCRIBE, connection and client
 * based information.
 *
 * @author Robin Atherton
 */
public interface UnsubscribeInboundInput extends ClientBasedInput {

    /**
     * The unmodifiable UNSUBSCRIBE packet that was intercepted.
     *
     * @return An unmodifiable {@link UnsubscribePacket}.
     */
    @NotNull
    @Immutable UnsubscribePacket getUnsubscribePacket();

}
