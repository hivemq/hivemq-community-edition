package com.hivemq.extensions.interceptor.disconnect;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundInput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class DisconnectOutboundInputImpl implements Supplier<DisconnectOutboundInputImpl>, DisconnectOutboundInput,
        PluginTaskInput {

    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;
    private @NotNull DisconnectPacket disconnectPacket;

    public DisconnectOutboundInputImpl(
            final @NotNull DisconnectPacket disconnectPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {
        this.disconnectPacket = disconnectPacket;
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
    }

    @Override
    public @Immutable @NotNull DisconnectPacket getDisconnectPacket() {
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
    public DisconnectOutboundInputImpl get() {
        return this;
    }

    public void updateDisconnect(final @NotNull DisconnectPacket disconnectPacket) {
        this.disconnectPacket = disconnectPacket;
    }
}
