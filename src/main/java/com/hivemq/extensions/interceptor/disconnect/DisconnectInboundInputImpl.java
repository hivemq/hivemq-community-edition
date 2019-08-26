package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundInput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class DisconnectInboundInputImpl
        implements Supplier<DisconnectInboundInputImpl>, DisconnectInboundInput, PluginTaskInput {

    private @NotNull DisconnectPacket disconnectPacket;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;

    public DisconnectInboundInputImpl(
            final @NotNull DisconnectPacket disconnectPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {
        this.disconnectPacket = disconnectPacket;
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
    }

    @Override
    @Immutable
    public @NotNull DisconnectPacket getDisconnectPacket() {
        return disconnectPacket;
    }

    @Override
    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @Override
    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }

    @Override
    public DisconnectInboundInputImpl get() {
        return this;
    }

    public void updateDisconnect(final @NotNull DisconnectPacket disconnectPacket) {
        this.disconnectPacket = new DisconnectPacketImpl(disconnectPacket);
    }
}
