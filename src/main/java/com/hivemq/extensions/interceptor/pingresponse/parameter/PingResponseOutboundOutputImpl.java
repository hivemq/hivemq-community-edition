package com.hivemq.extensions.interceptor.pingresponse.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingResponseOutboundOutput;
import com.hivemq.extension.sdk.api.packets.pingresponse.PingResponsePacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.pingresponse.PingResponsePacketImpl;
import com.hivemq.mqtt.message.PINGRESP;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Robin Atherton
 * @since 4.2.0
 */
public class PingResponseOutboundOutputImpl extends AbstractAsyncOutput<PingResponseOutboundOutput>
        implements PingResponseOutboundOutput, PluginTaskOutput, Supplier<PingResponseOutboundOutputImpl> {

    private final @NotNull AtomicBoolean preventDelivery;

    private final @NotNull PingResponsePacket pingResponsePacket;

    public PingResponseOutboundOutputImpl(final @NotNull PluginOutPutAsyncer asyncer, final @NotNull PINGRESP pingresp) {
        super(asyncer);
        this.pingResponsePacket = new PingResponsePacketImpl(pingresp);
        this.preventDelivery = new AtomicBoolean(false);
    }

    public PingResponsePacket getPingResponsePacket() {
        return pingResponsePacket;
    }

    @Override
    public PingResponseOutboundOutputImpl get() {
        return null;
    }

    public void forciblyPreventPingResponseDelivery() {
        this.preventDelivery.set(true);
    }

    public boolean deliveryPrevented() {
        return preventDelivery.get();
    }
}
