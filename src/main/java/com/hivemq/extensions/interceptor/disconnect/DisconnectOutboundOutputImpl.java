package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.disconnect.ModifiableDisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class DisconnectOutboundOutputImpl extends AbstractSimpleAsyncOutput<DisconnectOutboundOutput>
        implements DisconnectOutboundOutput, PluginTaskOutput, Supplier<DisconnectOutboundOutputImpl> {

    private @NotNull ModifiableDisconnectPacketImpl disconnectPacket;
    private final @NotNull FullConfigurationService configurationService;

    public DisconnectOutboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull DISCONNECT disconnect) {
        super(asyncer);
        this.configurationService = configurationService;
        this.disconnectPacket = new ModifiableDisconnectPacketImpl(this.configurationService, disconnect);
    }

    @Override
    public @NotNull ModifiableDisconnectPacketImpl getDisconnectPacket() {
        return disconnectPacket;
    }

    @Override
    public DisconnectOutboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull ModifiableDisconnectPacketImpl modifiedDisconnectPacket) {
        this.disconnectPacket = modifiedDisconnectPacket;
    }

    public void update(final @NotNull DISCONNECT disconnect) {
        this.disconnectPacket = new ModifiableDisconnectPacketImpl(configurationService, disconnect);
    }
}
