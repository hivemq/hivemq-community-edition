package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
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
    private final long originalSessionExpiryInterval;
    private @NotNull ModifiableInboundDisconnectPacketImpl disconnectPacket;

    public DisconnectInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull DISCONNECT disconnect,
            final long originalSessionExpiryInterval) {

        super(asyncer);
        this.configurationService = configurationService;
        this.originalSessionExpiryInterval = originalSessionExpiryInterval;
        this.disconnectPacket = new ModifiableInboundDisconnectPacketImpl(configurationService, disconnect,
                originalSessionExpiryInterval);
    }

    @Override
    public @NotNull ModifiableInboundDisconnectPacketImpl getDisconnectPacket() {
        return disconnectPacket;
    }

    @Override
    public @NotNull DisconnectInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull DisconnectPacket disconnectPacket) {
        this.disconnectPacket = new ModifiableInboundDisconnectPacketImpl(configurationService, disconnectPacket,
                originalSessionExpiryInterval);
    }
}
