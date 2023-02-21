/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.client.parameter;

import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.ClientTlsInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.client.parameter.ProxyInformation;
import com.hivemq.extension.sdk.api.client.parameter.TlsInformation;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extensions.ExtensionInformationUtil;
import io.netty.channel.Channel;

import java.net.InetAddress;
import java.util.Optional;

/**
 * @since 4.0.0
 */
public class ConnectionInformationImpl implements ConnectionInformation {

    private final @NotNull MqttVersion mqttVersion;
    private final @NotNull ConnectionAttributeStore connectionAttributeStore;
    private final @Nullable InetAddress inetAddress;
    private final @Nullable Listener listener;
    private final @Nullable ClientTlsInformation tlsInformation;

    public ConnectionInformationImpl(final @NotNull ClientConnectionContext clientConnectionContext) {
        Preconditions.checkNotNull(clientConnectionContext);

        final Channel channel = clientConnectionContext.getChannel();
        Preconditions.checkNotNull(channel);
        mqttVersion = ExtensionInformationUtil.mqttVersionFromChannel(channel);
        inetAddress = clientConnectionContext.getChannelAddress().orElse(null);
        listener = ExtensionInformationUtil.getListenerFromChannel(channel);
        tlsInformation = ExtensionInformationUtil.getTlsInformationFromChannel(channel);
        connectionAttributeStore = new ConnectionAttributeStoreImpl(channel);

    }

    @Override
    public @NotNull MqttVersion getMqttVersion() {
        return mqttVersion;
    }

    @Override
    public @NotNull Optional<InetAddress> getInetAddress() {
        return Optional.ofNullable(inetAddress);
    }

    @Override
    public @NotNull Optional<Listener> getListener() {
        return Optional.ofNullable(listener);
    }

    @Override
    public @NotNull Optional<ProxyInformation> getProxyInformation() {
        //Noop since community edition has no proxy protocol...
        return Optional.empty();
    }

    @Override
    public @NotNull ConnectionAttributeStore getConnectionAttributeStore() {
        return connectionAttributeStore;
    }


    @Override
    public @NotNull Optional<TlsInformation> getTlsInformation() {
        if (tlsInformation != null &&
                tlsInformation.getClientCertificate().isPresent() &&
                tlsInformation.getClientCertificateChain().isPresent()) {
            return Optional.of((TlsInformation) tlsInformation);
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<ClientTlsInformation> getClientTlsInformation() {
        return Optional.ofNullable(tlsInformation);
    }
}
