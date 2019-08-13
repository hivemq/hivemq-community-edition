package com.hivemq.extensions.interceptor.puback;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.packets.puback.ModifiablePubackPacketImpl;
import com.hivemq.mqtt.message.puback.PUBACK;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @since 4.2.0
 */
public class PubackOutboundOutputImpl extends AbstractAsyncOutput<PubackOutboundOutput>
        implements PubackOutboundOutput, Supplier<PubackOutboundOutputImpl> {

    private final @NotNull ModifiablePubackPacketImpl modifiablePubackPacket;
    private final @NotNull AtomicBoolean pubackPrevented = new AtomicBoolean(false);

    public PubackOutboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBACK puback) {
        super(asyncer);
        modifiablePubackPacket = new ModifiablePubackPacketImpl(configurationService, puback);
    }

    @Override
    public @Immutable @NotNull ModifiablePubackPacketImpl getPubackPacket() {
        return modifiablePubackPacket;
    }

    @Override
    public PubackOutboundOutputImpl get() {
        return this;
    }

    public void forciblyDisconnect()  {this.pubackPrevented.set(true); }

    public boolean pubackPrevented() {
        return pubackPrevented.get();
    }

}
