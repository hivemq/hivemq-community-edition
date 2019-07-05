package com.hivemq.extensions.interceptor.connect;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundInput;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Lukas Brandl
 * @since 4.2.0
 */
public class ConnectInboundInputImpl implements Supplier<ConnectInboundInputImpl>, ConnectInboundInput, PluginTaskInput {

    private @NotNull ConnectPacket connectPacket;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;

    public ConnectInboundInputImpl(
            final @NotNull ConnectPacket connectPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {
        this.connectPacket = connectPacket;
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
    }

    @Override
    public @Immutable @NotNull ConnectPacket getConnectPacket() {
        return connectPacket;
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
    public @NotNull ConnectInboundInputImpl get() {
        return this;
    }

    public void updateConnect(final @NotNull ConnectPacket connectPacket) {
        this.connectPacket = connectPacket;
    }
}
