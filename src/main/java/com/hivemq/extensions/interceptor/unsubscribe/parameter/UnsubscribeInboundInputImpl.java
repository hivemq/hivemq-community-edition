package com.hivemq.extensions.interceptor.unsubscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundInput;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
import com.hivemq.extension.sdk.api.packets.unsubscribe.UnsubscribePacket;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.unsubscribe.UnsubscribePacketImpl;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class UnsubscribeInboundInputImpl implements Supplier<UnsubscribeInboundInputImpl>, UnsubscribeInboundInput,
        PluginTaskInput {

    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;
    private @NotNull UnsubscribePacket unsubscribePacket;

    public UnsubscribeInboundInputImpl(
            final @NotNull UnsubscribePacket unsubscribePacket,
            final @NotNull String clientId,
            final @NotNull Channel channel) {
        this.unsubscribePacket = unsubscribePacket;
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
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
    public @NotNull UnsubscribePacket getUnsubscribePacket() {
        return unsubscribePacket;
    }

    @Override
    public @NotNull UnsubscribeInboundInputImpl get() {
        return this;
    }

    public void updateUnsubscribe(final @NotNull ModifiableUnsubscribePacket unsubscribePacket) {
        this.unsubscribePacket = new UnsubscribePacketImpl(unsubscribePacket);
    }
}
