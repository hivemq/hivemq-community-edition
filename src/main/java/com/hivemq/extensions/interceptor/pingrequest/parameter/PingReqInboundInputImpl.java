package com.hivemq.extensions.interceptor.pingrequest.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingReqInboundInput;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class PingReqInboundInputImpl implements Supplier<PingReqInboundInputImpl>, PingReqInboundInput,
        PluginTaskInput {

    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;

    public PingReqInboundInputImpl(
            final @NotNull String clientId,
            final @NotNull Channel channel) {
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
    public @NotNull PingReqInboundInputImpl get() {
        return this;
    }

}