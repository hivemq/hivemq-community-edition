package com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.pingrequest.PingRequestPacket;
import com.hivemq.extension.sdk.api.packets.pingresponse.PingResponsePacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * @author Robin Atherton
 * @since 4.2.0
 */
public interface PingResponseOutboundInput extends ClientBasedInput {

    /**
     * The unmodifiable PINGREQ packet that was intercepted.
     *
     *
     * @return An unmodifiable {@link PingRequestPacket}.
     * @since 4.2.0
     */
    @Immutable
    @NotNull PingResponsePacket getPingResponsePacket();

}
