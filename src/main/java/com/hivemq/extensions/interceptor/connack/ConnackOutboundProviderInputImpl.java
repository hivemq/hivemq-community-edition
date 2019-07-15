package com.hivemq.extensions.interceptor.connack;

import com.google.common.base.Preconditions;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundProviderInput;
import com.hivemq.extensions.PluginInformationUtil;
import io.netty.channel.Channel;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnackOutboundProviderInputImpl implements ConnackOutboundProviderInput {

    @NotNull
    private final ConnectionInformation connectionInformation;

    @NotNull
    private final ClientInformation clientInformation;

    @NotNull
    private final ServerInformation serverInformation;

    public ConnackOutboundProviderInputImpl(final @NotNull ServerInformation serverInformation,
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
