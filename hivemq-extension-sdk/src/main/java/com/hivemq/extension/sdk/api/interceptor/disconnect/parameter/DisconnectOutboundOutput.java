package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;

public interface DisconnectOutboundOutput {

    /**
     * @return
     */
    @NotNull ModifiableDisconnectPacket getDisconnectPacket();

}
