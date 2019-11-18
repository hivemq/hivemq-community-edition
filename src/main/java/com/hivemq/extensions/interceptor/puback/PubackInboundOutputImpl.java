package com.hivemq.extensions.interceptor.puback;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundOutput;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.packets.puback.ModifiablePubackPacketImpl;
import com.hivemq.mqtt.message.puback.PUBACK;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 */
public class PubackInboundOutputImpl extends AbstractSimpleAsyncOutput<PubackInboundOutput>
        implements PubackInboundOutput, Supplier<PubackInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private ModifiablePubackPacketImpl pubackPacket;

    public PubackInboundOutputImpl(final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBACK puback) {
        super(asyncer);
        this.configurationService = configurationService;
        this.pubackPacket = new ModifiablePubackPacketImpl(configurationService, puback);
    }

    @Override
    public @Immutable
    @NotNull ModifiablePubackPacketImpl getPubackPacket() {
        return pubackPacket;
    }

    @Override
    public PubackInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull PubackPacket puback) {
        this.pubackPacket = new ModifiablePubackPacketImpl(configurationService, puback);
    }
}
