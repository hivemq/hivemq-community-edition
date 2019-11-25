package com.hivemq.extensions.interceptor.pubrec;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundOutput;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.pubrec.ModifiablePubrecPacketImpl;
import com.hivemq.mqtt.message.pubrec.PUBREC;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubrecOutboundOutputImpl extends AbstractSimpleAsyncOutput<PubrecOutboundOutput>
        implements PubrecOutboundOutput, Supplier<PubrecOutboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiablePubrecPacketImpl pubrecPacket;

    public PubrecOutboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBREC pubrec) {

        super(asyncer);
        this.configurationService = configurationService;
        pubrecPacket = new ModifiablePubrecPacketImpl(configurationService, pubrec);
    }

    @Override
    public @NotNull ModifiablePubrecPacketImpl getPubrecPacket() {
        return pubrecPacket;
    }

    @Override
    public @NotNull PubrecOutboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PubrecPacket pubrecPacket) {
        this.pubrecPacket = new ModifiablePubrecPacketImpl(configurationService, pubrecPacket);
    }
}
