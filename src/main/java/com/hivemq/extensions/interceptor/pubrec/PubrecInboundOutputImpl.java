package com.hivemq.extensions.interceptor.pubrec;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubrec.ModifiablePubrecPacketImpl;
import com.hivemq.mqtt.message.pubrec.PUBREC;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 */
public class PubrecInboundOutputImpl extends AbstractSimpleAsyncOutput<PubrecInboundOutput>
        implements PubrecInboundOutput, Supplier<PubrecInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiablePubrecPacketImpl modifiablePubrecPacket;

    public PubrecInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBREC pubrec) {
        super(asyncer);
        this.configurationService = configurationService;
        this.modifiablePubrecPacket = new ModifiablePubrecPacketImpl(this.configurationService, pubrec);
    }

    @Override
    public @NotNull ModifiablePubrecPacketImpl getPubrecPacket() {
        return modifiablePubrecPacket;
    }

    @Override
    public PubrecInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PUBREC unmodifiedPubrec) {
        this.modifiablePubrecPacket = new ModifiablePubrecPacketImpl(configurationService, unmodifiedPubrec);
    }
}
