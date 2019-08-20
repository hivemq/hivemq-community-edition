package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
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
public class DisconnectInboundOutputImpl extends AbstractAsyncOutput<DisconnectInboundOutput>
        implements DisconnectInboundOutput,
        PluginTaskOutput, Supplier<DisconnectInboundOutputImpl> {

    private final @NotNull ModifiableDisconnectPacket disconnectPacket;

    public DisconnectInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull DISCONNECT disconnect) {
        super(asyncer);

        this.disconnectPacket = new ModifiableDisconnectPacketImpl(configurationService, disconnect);
    }

    @Override
    public @NotNull DisconnectPacket getDisconnectPacket() {
        return this.disconnectPacket;
    }

    @Override
    public DisconnectInboundOutputImpl get() {
        return this;
    }
}
