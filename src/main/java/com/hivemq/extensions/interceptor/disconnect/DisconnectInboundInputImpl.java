package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundInput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class DisconnectInboundInputImpl
        implements Supplier<DisconnectInboundInputImpl>, DisconnectInboundInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull DisconnectPacketImpl disconnectPacket;

    public DisconnectInboundInputImpl(
            final @NotNull String clientId,
            final @NotNull Channel channel,
            final @NotNull DISCONNECT disconnect) {

        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.disconnectPacket = new DisconnectPacketImpl(disconnect);
    }

    @Override
    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }

    @Override
    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @Override
    public @Immutable @NotNull DisconnectPacket getDisconnectPacket() {
        return disconnectPacket;
    }

    @Override
    public @NotNull DisconnectInboundInputImpl get() {
        return this;
    }

    public void update(final @NotNull DisconnectPacket disconnectPacket) {
        this.disconnectPacket = new DisconnectPacketImpl(disconnectPacket);
    }
}
