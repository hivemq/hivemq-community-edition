package com.hivemq.extensions.interceptor.pubrel.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelOutboundInput;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 */
public class PubrelOutboundInputImpl implements Supplier<PubrelOutboundInputImpl>, PubrelOutboundInput,
        PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull PubrelPacket pubrelPacket;

    public PubrelOutboundInputImpl(
            final @NotNull PubrelPacket pubrelPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {

        this.pubrelPacket = pubrelPacket;
        clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
    }

    @NotNull
    @Override
    public PubrelPacket getPubrelPacket() {
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
    public PubrelOutboundInputImpl get() {
        return this;
    }

    public void updatePubrel(final @NotNull PubrelPacket pubrelPacket) {
        this.pubrelPacket = new PubrelPacketImpl(pubrelPacket);
    }
}
