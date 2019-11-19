package com.hivemq.extensions.interceptor.pubcomp.parameter;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundOutput;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubcomp.ModifiablePubcompPacketImpl;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubcompInboundOutputImpl extends AbstractSimpleAsyncOutput<PubcompInboundOutput>
        implements PubcompInboundOutput, Supplier<PubcompInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiablePubcompPacketImpl pubcompPacket;

    public PubcompInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBCOMP pubcomp) {

        super(asyncer);
        this.configurationService = configurationService;
        pubcompPacket = new ModifiablePubcompPacketImpl(configurationService, pubcomp);
    }

    @Override
    public @NotNull ModifiablePubcompPacketImpl getPubcompPacket() {
        return pubcompPacket;
    }

    @Override
    public @NotNull PubcompInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PubcompPacket pubcompPacket) {
        this.pubcompPacket = new ModifiablePubcompPacketImpl(configurationService, pubcompPacket);
    }
}
