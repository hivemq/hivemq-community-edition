package com.hivemq.extensions.interceptor.pubrec;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundInput;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubrec.PubrecPacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

public class PubrecOutboundInputImpl implements Supplier<PubrecOutboundInputImpl>, PubrecOutboundInput,
        PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull PubrecPacket pubrecPacket;

    public PubrecOutboundInputImpl(
            final @NotNull PubrecPacket pubrecPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {

        this.pubrecPacket = pubrecPacket;
        clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
    }

    @Override
    public @NotNull PubrecPacket getPubrecPacket() {
        return pubrecPacket;
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
    public PubrecOutboundInputImpl get() {
        return this;
    }

    public void updatePubrec(final @NotNull PubrecPacket pubrecPacket) {
        this.pubrecPacket = new PubrecPacketImpl(pubrecPacket);
    }
}
