package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.disconnect.ModifiableInboundDisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class DisconnectInboundOutputImpl extends AbstractSimpleAsyncOutput<DisconnectInboundOutput>
        implements DisconnectInboundOutput, PluginTaskOutput, Supplier<DisconnectInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiableInboundDisconnectPacketImpl disconnectPacket;

    public DisconnectInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull DISCONNECT disconnect) {
        super(asyncer);
        this.configurationService = configurationService;
        this.disconnectPacket = new ModifiableInboundDisconnectPacketImpl(this.configurationService, disconnect);
    }

    @Override
    public @NotNull ModifiableInboundDisconnectPacketImpl getDisconnectPacket() {
        return this.disconnectPacket;
    }

    @Override
    public DisconnectInboundOutputImpl get() {
        return this;
    }


    public void update(final @NotNull DISCONNECT disconnect) {
        this.disconnectPacket = new ModifiableInboundDisconnectPacketImpl(configurationService, disconnect);
    }
}
