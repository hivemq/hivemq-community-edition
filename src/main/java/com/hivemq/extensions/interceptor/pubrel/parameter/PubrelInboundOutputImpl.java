package com.hivemq.extensions.interceptor.pubrel.parameter;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubrel.ModifiablePubrelPacketImpl;
import com.hivemq.mqtt.message.pubrel.PUBREL;

import java.util.function.Supplier;

public class PubrelInboundOutputImpl extends AbstractSimpleAsyncOutput<PubrelInboundOutput>
        implements PubrelInboundOutput, Supplier<PubrelInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private ModifiablePubrelPacketImpl modifiablePubrelPacket;

    public PubrelInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBREL pubrel) {
        super(asyncer);
        this.configurationService = configurationService;
        modifiablePubrelPacket = new ModifiablePubrelPacketImpl(this.configurationService, pubrel);
    }

    @Override
    public @Immutable
    @NotNull ModifiablePubrelPacketImpl getPubrelPacket() {
        return modifiablePubrelPacket;
    }

    @Override
    public PubrelInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PUBREL pubrel) {
        modifiablePubrelPacket = new ModifiablePubrelPacketImpl(configurationService, pubrel);
    }
}
