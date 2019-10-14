package com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;

/**
 * This is the output parameter of any {@link UnsubscribeInboundInterceptor} providing methods to define the outcome of
 * UNSUBSCRIBE interception.
 * <p>
 * It can be used to modify an inbound UNSUBSCRIBE packet.
 * <p>
 *
 * @author Robin Atherton
 */
public interface UnsubscribeInboundOutput extends AsyncOutput<UnsubscribeInboundOutput> {

    /**
     * Use this Object to make any changes to the inbound UNSUBSCRIBE.
     *
     * @return a {@link ModifiableUnsubscribePacket}.
     */
    @NotNull ModifiableUnsubscribePacket getUnsubscribePacket();

}
