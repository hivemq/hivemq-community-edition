package com.hivemq.extensions.interceptor.pingrequest.parameter;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingRequestInboundInput;
import com.hivemq.extension.sdk.api.packets.pingrequest.PingRequestPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 * @since 4.2.0
 */
public class PingRequestInboundInputImpl implements Supplier<PingRequestInboundInputImpl>, PingRequestInboundInput,
        PluginTaskInput {

    private final @NotNull PingRequestPacket pingRequestPacket;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;

    public PingRequestInboundInputImpl(
            final @NotNull PingRequestPacket pingRequestPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {
        this.pingRequestPacket = pingRequestPacket;
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
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
    public @Immutable @NotNull PingRequestPacket getPingRequestPacket() {
        return pingRequestPacket;
    }

    @Override
    public PingRequestInboundInputImpl get() {
        return this;
    }

}