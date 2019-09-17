package com.hivemq.extensions.interceptor.pubrel.parameter;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubrel.ModifiablePubrelPacketImpl;
import com.hivemq.mqtt.message.pubrel.PUBREL;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 */
public class PubrelOutboundOutputImpl extends AbstractSimpleAsyncOutput<PubrelOutboundOutput>
        implements PubrelOutboundOutput, Supplier<PubrelOutboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiablePubrelPacketImpl modifiablePubrelPacket;

    public PubrelOutboundOutputImpl(
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
    public PubrelOutboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PUBREL pubrel) {
        modifiablePubrelPacket = new ModifiablePubrelPacketImpl(configurationService, pubrel);
    }
}
