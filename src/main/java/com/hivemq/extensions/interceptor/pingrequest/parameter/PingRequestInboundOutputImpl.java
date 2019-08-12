package com.hivemq.extensions.interceptor.pingrequest.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingRequestInboundOutput;
import com.hivemq.extension.sdk.api.packets.pingrequest.PingRequestPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.pingrequest.PingRequestPacketImpl;
import com.hivemq.mqtt.message.PINGREQ;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Robin Atherton
 * @since 4.2.0
 */
public class PingRequestInboundOutputImpl extends AbstractAsyncOutput<PingRequestInboundOutput>
        implements PingRequestInboundOutput, PluginTaskOutput, Supplier<PingRequestInboundOutputImpl> {

    private final @NotNull AtomicBoolean preventDelivery;

    private final @NotNull PingRequestPacket pingRequestPacket;

    public PingRequestInboundOutputImpl(final @NotNull PluginOutPutAsyncer asyncer, final @NotNull PINGREQ pingreq) {
        super(asyncer);
        this.pingRequestPacket = new PingRequestPacketImpl(pingreq);
        this.preventDelivery = new AtomicBoolean(false);
    }

    public PingRequestPacket getPingRequestPacket() {
        return pingRequestPacket;
    }

    @Override
    public PingRequestInboundOutputImpl get() {
        return this;
    }

    /**
     * happens for async timeout and fallback behaviour {@link TimeoutFallback#FAILURE}.
     */
    public void forciblyPreventPingRequestDelivery() {
        this.preventDelivery.set(true);
    }

    public boolean deliveryPrevented() {
        return preventDelivery.get();
    }
}