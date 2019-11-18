package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.disconnect.ModifiableOutboundDisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class DisconnectOutboundOutputImpl extends AbstractSimpleAsyncOutput<DisconnectOutboundOutput>
        implements DisconnectOutboundOutput, PluginTaskOutput, Supplier<DisconnectOutboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiableOutboundDisconnectPacketImpl disconnectPacket;

    public DisconnectOutboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull DISCONNECT disconnect) {

        super(asyncer);
        this.configurationService = configurationService;
        this.disconnectPacket = new ModifiableOutboundDisconnectPacketImpl(configurationService, disconnect);
    }

    @Override
    public @NotNull ModifiableOutboundDisconnectPacketImpl getDisconnectPacket() {
        return disconnectPacket;
    }

    @Override
    public @NotNull DisconnectOutboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull DisconnectPacket disconnectPacket) {
        this.disconnectPacket = new ModifiableOutboundDisconnectPacketImpl(configurationService, disconnectPacket);
    }
}
