/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.interceptor.connect;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
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
