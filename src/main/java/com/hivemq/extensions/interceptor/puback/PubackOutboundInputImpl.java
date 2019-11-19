package com.hivemq.extensions.interceptor.puback;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundInput;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.puback.PubackPacketImpl;
import com.hivemq.mqtt.message.puback.PUBACK;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Yannick Weber
 */
public class PubackOutboundInputImpl implements Supplier<PubackOutboundInputImpl>, PubackOutboundInput,
        PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private @NotNull PubackPacket pubackPacket;

    public PubackOutboundInputImpl(
            final @NotNull String clientId,
            final @NotNull Channel channel,
            final @NotNull PUBACK puback) {

        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.pubackPacket = new PubackPacketImpl(puback);
    }


    @Override
    public @NotNull PubackPacket getPubackPacket() {
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
    public @NotNull PubackOutboundInputImpl get() {
        return this;
    }

    public void update(final @NotNull PubackPacket pubackPacket) {
        this.pubackPacket = new PubackPacketImpl(pubackPacket);
    }
}
