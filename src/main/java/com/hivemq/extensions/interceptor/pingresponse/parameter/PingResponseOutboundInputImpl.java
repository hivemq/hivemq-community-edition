package com.hivemq.extensions.interceptor.pingresponse.parameter;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingResponseOutboundInput;
import com.hivemq.extension.sdk.api.packets.pingresponse.PingResponsePacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 * @since 4.2.0
 */
public class PingResponseOutboundInputImpl implements Supplier<PingResponseOutboundInputImpl>,
        PingResponseOutboundInput, PluginTaskInput {

    private final @NotNull PingResponsePacket pingResponsePacket;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;

    public PingResponseOutboundInputImpl(
            final @NotNull PingResponsePacket pingResponsePacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {
        this.pingResponsePacket = pingResponsePacket;
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
    }

    @Override
    public PingResponseOutboundInputImpl get() {
        return this;
    }

    @Override
    public @Immutable @NotNull PingResponsePacket getPingResponsePacket() {
        return pingResponsePacket;
    }

    @Override
    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @Override
    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }
}