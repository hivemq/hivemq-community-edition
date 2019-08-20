package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

public interface DisconnectOutboundInput extends ClientBasedInput {

    /**
     * //TODO
     */
    @Immutable
    @NotNull DisconnectPacket getDisconnectPacket();

}
