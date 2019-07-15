package com.hivemq.extensions.interceptor.connack;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundInput;
import com.hivemq.extension.sdk.api.packets.connack.ConnackPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.connack.ConnackPacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnackOutboundInputImpl implements Supplier<ConnackOutboundInputImpl>, ConnackOutboundInput, PluginTaskInput {

    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;
    private @NotNull ConnackPacket connackPacket;

    public ConnackOutboundInputImpl(
            final @NotNull ConnackPacket connackPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {
        this.connackPacket = connackPacket;
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
    }

    @Override
    public @Immutable @NotNull ConnackPacket getConnackPacket() {
        return connackPacket;
    }

    @NotNull
    @Override
    public ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @NotNull
    @Override
    public ClientInformation getClientInformation() {
        return clientInformation;
    }

    @Override
    public @NotNull ConnackOutboundInputImpl get() {
        return this;
    }

    public void updateConnack(final @NotNull ConnackPacket connackPacket) {
        this.connackPacket = new ConnackPacketImpl(connackPacket);
    }
}
