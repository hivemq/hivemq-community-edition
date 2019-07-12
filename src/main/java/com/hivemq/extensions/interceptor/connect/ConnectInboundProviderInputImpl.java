package com.hivemq.extensions.interceptor.connect;

import com.google.common.base.Preconditions;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundProviderInput;
import com.hivemq.extensions.PluginInformationUtil;
import io.netty.channel.Channel;

/**
 * @author Lukas Brandl
 */
public class ConnectInboundProviderInputImpl implements ConnectInboundProviderInput {

    @NotNull
    private final ConnectionInformation connectionInformation;

    @NotNull
    private final ClientInformation clientInformation;

    @NotNull
    private final ServerInformation serverInformation;

    public ConnectInboundProviderInputImpl(final @NotNull ServerInformation serverInformation,
                                           final @NotNull Channel channel,
                                           final @NotNull String clientId) {

        Preconditions.checkNotNull(channel, "channel must never be null");
        Preconditions.checkNotNull(clientId, "client id must never be null");
        Preconditions.checkNotNull(serverInformation, "server information must never be null");

        this.clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        this.connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
        this.serverInformation = serverInformation;
    }

    @NotNull
    @Override
    public ServerInformation getServerInformation() {
        return serverInformation;
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
}
