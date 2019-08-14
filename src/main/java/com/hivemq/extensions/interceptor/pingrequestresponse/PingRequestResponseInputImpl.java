package com.hivemq.extensions.interceptor.pingrequestresponse;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pingrequestresponse.parameter.PingRequestResponseInput;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import io.netty.channel.Channel;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class PingRequestResponseInputImpl
        implements Supplier<PingRequestResponseInputImpl>, PingRequestResponseInput, PluginTaskInput {

    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ClientInformation clientInformation;

    private final PINGREQ pingReq;
    private final PINGRESP pingResp;

    public PingRequestResponseInputImpl(
            final @NotNull String clientID,
            final @NotNull Channel channel) {
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientID);
        this.pingReq = new PINGREQ();
        this.pingResp = new PINGRESP();
    }

    public PINGREQ getPingReq() {
        return pingReq;
    }

    public PINGRESP getPingResp() {
        return pingResp;
    }

    @Override
    public PingRequestResponseInputImpl get() {
        return this;
    }

    @Override
    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @Override
    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }
}
