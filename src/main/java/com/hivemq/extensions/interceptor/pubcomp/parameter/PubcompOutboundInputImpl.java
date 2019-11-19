package com.hivemq.extensions.interceptor.pubcomp.parameter;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompOutboundInput;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubcomp.PubcompPacketImpl;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubcompOutboundInputImpl
        implements Supplier<PubcompOutboundInputImpl>, PubcompOutboundInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull PubcompPacketImpl pubcompPacket;

    public PubcompOutboundInputImpl(
            final @NotNull String clientId,
            final @NotNull Channel channel,
            final @NotNull PUBCOMP pubcomp) {

        clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        pubcompPacket = new PubcompPacketImpl(pubcomp);
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
    public @Immutable @NotNull PubcompPacket getPubcompPacket() {
        return pubcompPacket;
    }

    @Override
    public @NotNull PubcompOutboundInputImpl get() {
        return this;
    }

    public void update(final @NotNull PubcompPacket pubcompPacket) {
        this.pubcompPacket = new PubcompPacketImpl(pubcompPacket);
    }
}
