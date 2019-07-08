package com.hivemq.extensions.interceptor.subscribe.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubscribePacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.subscribe.SubscribePacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class SubscribeInboundInputImpl implements Supplier<SubscribeInboundInputImpl>, SubscribeInboundInput, PluginTaskInput {

    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;
    private @NotNull SubscribePacket subscribePacket;

    public SubscribeInboundInputImpl(
            final @NotNull SubscribePacket subscribePacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {
        this.subscribePacket = subscribePacket;
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
    }

    @NotNull
    @Override
    public SubscribePacket getSubscribePacket() {
        return subscribePacket;
    }

    @NotNull
    @Override
    public ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @NotNull
    @Override
    public ClientInformation getClientInformation() {
        return clientInformation;
    }

    @Override
    public @NotNull SubscribeInboundInputImpl get() {
        return this;
    }

    public void updateSubscribe(final @NotNull ModifiableSubscribePacket subscribePacket) {
        this.subscribePacket = new SubscribePacketImpl(subscribePacket);
    }
}
