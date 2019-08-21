package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableDisconnectPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.disconnect.ModifiableDisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class DisconnectOutboundOutputImpl extends AbstractAsyncOutput<DisconnectOutboundOutput>
        implements DisconnectOutboundOutput,
        PluginTaskOutput, Supplier<DisconnectOutboundOutputImpl> {

    private final @NotNull ModifiableDisconnectPacketImpl disconnectPacket;

    public DisconnectOutboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull FullConfigurationService configurationService,
            final @NotNull DISCONNECT disconnect) {
        super(asyncer);
        this.disconnectPacket = new ModifiableDisconnectPacketImpl(configurationService, disconnect);
    }

    @Override
    public @NotNull ModifiableDisconnectPacket getDisconnectPacket() {
        return disconnectPacket;
    }

    @Override
    public DisconnectOutboundOutputImpl get() {
        return this;
    }
}
