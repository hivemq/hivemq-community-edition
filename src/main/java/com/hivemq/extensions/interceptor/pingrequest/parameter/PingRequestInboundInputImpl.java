package com.hivemq.extensions.interceptor.pingrequest.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingRequestInboundInput;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.mqtt.message.PINGREQ;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class PingRequestInboundInputImpl implements Supplier<PingRequestInboundInputImpl>, PingRequestInboundInput,
        PluginTaskInput {

    private final @NotNull PINGREQ pingreq;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;

    public PingRequestInboundInputImpl(
            final @NotNull String clientId,
            final @NotNull Channel channel) {
        this.pingreq = new PINGREQ();
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
    }

    public PINGREQ getPingreq() {
        return pingreq;
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
    public PingRequestInboundInputImpl get() {
        return this;
    }

}