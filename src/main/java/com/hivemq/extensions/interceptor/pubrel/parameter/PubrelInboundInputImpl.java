package com.hivemq.extensions.interceptor.pubrel.parameter;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundInput;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 */
public class PubrelInboundInputImpl implements PubrelInboundInput, Supplier<PubrelInboundInputImpl>, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull PubrelPacket pubrelPacket;

    public PubrelInboundInputImpl(
            final @NotNull PubrelPacket pubrelPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {

        this.pubrelPacket = pubrelPacket;
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
    }

    @Override
    public @NotNull
    @Immutable
    PubrelPacket getPubrelPacket() {
        return pubrelPacket;
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
    public PubrelInboundInputImpl get() {
        return this;
    }

    public void updatePubrel(final @NotNull PubrelPacket pubrelPacket) {
        this.pubrelPacket = new PubrelPacketImpl(pubrelPacket);
    }
}
