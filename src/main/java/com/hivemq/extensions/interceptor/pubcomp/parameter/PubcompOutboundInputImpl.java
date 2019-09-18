package com.hivemq.extensions.interceptor.pubcomp.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompOutboundInput;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubcomp.PubcompPacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 */
public class PubcompOutboundInputImpl implements Supplier<PubcompOutboundInputImpl>, PubcompOutboundInput,
        PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull PubcompPacket pubcompPacket;

    public PubcompOutboundInputImpl(
            final @NotNull PubcompPacket pubcompPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {

        this.pubcompPacket = pubcompPacket;
        clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
    }

    @NotNull
    @Override
    public PubcompPacket getPubcompPacket() {
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
    public PubcompOutboundInputImpl get() {
        return this;
    }

    public void updatePubcomp(final @NotNull PubcompPacket pubcompPacket) {
        this.pubcompPacket = new PubcompPacketImpl(pubcompPacket);
    }
}
