package com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;

public interface UnsubscribeInboundOutput extends AsyncOutput<UnsubscribeInboundOutput> {

    /**
     * Use this Object to make any changes to the inbound UNSUBSCRIBE.
     *
     * @return a modifiable unsubscribe packet.
     */
    @NotNull ModifiableUnsubscribePacket getUnsubscribePacket();

}
