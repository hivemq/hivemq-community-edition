package com.hivemq.extensions.interceptor.pubrel.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelOutboundInput;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubrelOutboundInputImpl
        implements Supplier<PubrelOutboundInputImpl>, PubrelOutboundInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull PubrelPacketImpl pubrelPacket;

    public PubrelOutboundInputImpl(
            final @NotNull String clientId,
            final @NotNull Channel channel,
            final @NotNull PUBREL pubrel) {

        clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        pubrelPacket = new PubrelPacketImpl(pubrel);
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
    public @Immutable @NotNull PubrelPacket getPubrelPacket() {
        return pubrelPacket;
    }

    @Override
    public @NotNull PubrelOutboundInputImpl get() {
        return this;
    }

    public void update(final @NotNull PubrelPacket pubrelPacket) {
        this.pubrelPacket = new PubrelPacketImpl(pubrelPacket);
    }
}
