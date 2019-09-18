package com.hivemq.extensions.interceptor.puback;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundInput;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.puback.PubackPacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 */
public class PubackInboundInputImpl implements PubackInboundInput, Supplier<PubackInboundInputImpl>, PluginTaskInput {

    private @NotNull PubackPacket pubackPacket;
    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;

    public PubackInboundInputImpl(
            final @NotNull PubackPacket pubackPacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {

        this.pubackPacket = pubackPacket;
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
    }

    @Override
    public @NotNull @Immutable
    PubackPacket getPubackPacket() {
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
    public PubackInboundInputImpl get() {
        return this;
    }

    public void updatePuback(final @NotNull PubackPacket pubackPacket) {
        this.pubackPacket = new PubackPacketImpl(pubackPacket);
    }
}
