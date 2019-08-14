package com.hivemq.extensions.interceptor.puback;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.packets.puback.ModifiablePubackPacketImpl;
import com.hivemq.mqtt.message.puback.PUBACK;

import java.util.function.Supplier;

public class PubackInboundOutputImpl extends AbstractAsyncOutput<PubackInboundOutput>
        implements PubackInboundOutput, Supplier<PubackInboundOutputImpl> {

    private final ModifiablePubackPacketImpl modifiablePubackPacket;

    public PubackInboundOutputImpl(final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBACK puback) {
        super(asyncer);
        modifiablePubackPacket = new ModifiablePubackPacketImpl(configurationService, puback);
    }

    @Override
    public @Immutable
    @NotNull ModifiablePubackPacketImpl getPubackPacket() {
        return modifiablePubackPacket;
    }

    @Override
    public PubackInboundOutputImpl get() {
        return this;
    }
}
