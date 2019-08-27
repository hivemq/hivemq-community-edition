package com.hivemq.extensions.interceptor.pubrec;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecInboundInput;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.pubrec.PubrecPacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

public class PubrecInboundInputImpl implements Supplier<PubrecInboundInputImpl>, PubrecInboundInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull PubrecPacket pubackPacket;

    public PubrecInboundInputImpl(
            final @NotNull PubrecPacket pubackPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {

        this.pubackPacket = pubackPacket;
        clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
    }

    @Override
    public @NotNull PubrecPacket getPubrecPacket() {
        return pubackPacket;
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
    public PubrecInboundInputImpl get() {
        return this;
    }

    public void updatePubrec(final @NotNull PubrecPacket pubackPacket) {
        this.pubackPacket = new PubrecPacketImpl(pubackPacket);
    }
}
