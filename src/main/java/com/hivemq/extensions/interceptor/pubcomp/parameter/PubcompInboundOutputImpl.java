package com.hivemq.extensions.interceptor.pubcomp.parameter;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubcomp.ModifiablePubcompPacketImpl;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;

import java.util.function.Supplier;

public class PubcompInboundOutputImpl extends AbstractSimpleAsyncOutput<PubcompInboundOutput>
        implements PubcompInboundOutput, Supplier<PubcompInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private ModifiablePubcompPacketImpl modifiablePubcompPacket;

    public PubcompInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBCOMP pubcomp) {
        super(asyncer);
        this.configurationService = configurationService;
        modifiablePubcompPacket = new ModifiablePubcompPacketImpl(this.configurationService, pubcomp);
    }

    @Override
    public @Immutable
    @NotNull ModifiablePubcompPacketImpl getPubcompPacket() {
        return modifiablePubcompPacket;
    }

    @Override
    public PubcompInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PUBCOMP pubcomp) {
        modifiablePubcompPacket = new ModifiablePubcompPacketImpl(configurationService, pubcomp);
    }
}
