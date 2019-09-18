package com.hivemq.extensions.interceptor.pubcomp.parameter;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundInput;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubcomp.PubcompPacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

public class PubcompInboundInputImpl
        implements PubcompInboundInput, Supplier<PubcompInboundInputImpl>, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull PubcompPacket pubcompPacket;

    public PubcompInboundInputImpl(
            final @NotNull PubcompPacket pubcompPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {

        this.pubcompPacket = pubcompPacket;
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
    }

    @Override
    public @NotNull
    @Immutable
    PubcompPacket getPubcompPacket() {
        return pubcompPacket;
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
    public PubcompInboundInputImpl get() {
        return this;
    }

    public void updatePubcomp(final @NotNull PubcompPacket pubcompPacket) {
        this.pubcompPacket = new PubcompPacketImpl(pubcompPacket);
    }
}
