package com.hivemq.extensions.interceptor.subscribe.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.subscribe.ModifiableSubscribePacketImpl;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class SubscribeInboundOutputImpl extends AbstractAsyncOutput<SubscribeInboundOutput> implements SubscribeInboundOutput, PluginTaskOutput, Supplier<SubscribeInboundOutputImpl> {

    private final @NotNull AtomicBoolean preventDelivery;

    private final @NotNull ModifiableSubscribePacketImpl subscribePacket;

    public SubscribeInboundOutputImpl(final @NotNull FullConfigurationService configurationService, final @NotNull PluginOutPutAsyncer asyncer, final @NotNull SUBSCRIBE subscribe) {
        super(asyncer);
        this.subscribePacket = new ModifiableSubscribePacketImpl(configurationService, subscribe);
        this.preventDelivery = new AtomicBoolean(false);
    }

    @Override
    public @NotNull ModifiableSubscribePacketImpl getSubscribePacket() {
        return subscribePacket;
    }

    /**
     * happens for async timeout and fallback behaviour {@link TimeoutFallback#FAILURE}.
     */
    public void forciblyPreventSubscribeDelivery() {
        this.preventDelivery.set(true);
    }

    @Override
    public @NotNull SubscribeInboundOutputImpl get() {
        return this;
    }

    public boolean deliveryPrevented() {
        return preventDelivery.get();
    }
}
