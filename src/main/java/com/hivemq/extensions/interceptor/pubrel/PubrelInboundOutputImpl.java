package com.hivemq.extensions.interceptor.pubrel;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundOutput;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubrel.ModifiablePubrelPacketImpl;
import com.hivemq.mqtt.message.pubrel.PUBREL;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubrelInboundOutputImpl extends AbstractSimpleAsyncOutput<PubrelInboundOutput>
        implements PubrelInboundOutput, Supplier<PubrelInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiablePubrelPacketImpl pubrelPacket;

    public PubrelInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBREL pubrel) {

        super(asyncer);
        this.configurationService = configurationService;
        pubrelPacket = new ModifiablePubrelPacketImpl(configurationService, pubrel);
    }

    @Override
    public @Immutable @NotNull ModifiablePubrelPacketImpl getPubrelPacket() {
        return pubrelPacket;
    }

    @Override
    public @NotNull PubrelInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PubrelPacket pubrelPacket) {
        this.pubrelPacket = new ModifiablePubrelPacketImpl(configurationService, pubrelPacket);
    }
}
