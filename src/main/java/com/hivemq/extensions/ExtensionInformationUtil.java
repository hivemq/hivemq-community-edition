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
package com.hivemq.extensions;

import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.configuration.service.entity.TlsTcpListener;
import com.hivemq.configuration.service.entity.TlsWebsocketListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.*;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extensions.client.parameter.ClientInformationImpl;
import com.hivemq.extensions.client.parameter.ClientTlsInformationImpl;
import com.hivemq.extensions.client.parameter.ConnectionInformationImpl;
import com.hivemq.extensions.client.parameter.ListenerImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.security.auth.SslClientCertificate;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ExtensionInformationUtil {

    private static final Logger log = LoggerFactory.getLogger(ExtensionInformationUtil.class);

    public static @NotNull ClientInformation getAndSetClientInformation(@NotNull final Channel channel, @NotNull final String clientId) {
        final ClientConnectionContext clientConnectionContext = ClientConnectionContext.of(channel);
        if (clientConnectionContext.getExtensionClientInformation() == null) {
            clientConnectionContext.setExtensionClientInformation(new ClientInformationImpl(clientId));
        }
        return clientConnectionContext.getExtensionClientInformation();
    }

    public static @NotNull ConnectionInformation getAndSetConnectionInformation(@NotNull final Channel channel) {
        final ClientConnectionContext clientConnectionContext = ClientConnectionContext.of(channel);
        if (clientConnectionContext.getExtensionConnectionInformation() == null) {
            clientConnectionContext.setExtensionConnectionInformation(new ConnectionInformationImpl(clientConnectionContext));
        }
        return clientConnectionContext.getExtensionConnectionInformation();
    }

    public static @NotNull MqttVersion mqttVersionFromChannel(final @NotNull Channel channel) {

        Preconditions.checkNotNull(channel, "channel must never be null");
        final ProtocolVersion protocolVersion = ClientConnectionContext.of(channel).getProtocolVersion();
        Preconditions.checkNotNull(protocolVersion, "protocol version must never be null");

        return mqttVersionFromProtocolVersion(protocolVersion);
    }

    public static @NotNull MqttVersion mqttVersionFromProtocolVersion(final @NotNull ProtocolVersion protocolVersion) {
        switch (protocolVersion) {
            case MQTTv3_1:
                return MqttVersion.V_3_1;
            case MQTTv3_1_1:
                return MqttVersion.V_3_1_1;
            case MQTTv5:
            default:
                return MqttVersion.V_5;
        }
    }

    public static @Nullable Listener getListenerFromChannel(final @NotNull Channel channel) {

        Preconditions.checkNotNull(channel, "channel must never be null");
        final com.hivemq.configuration.service.entity.Listener hiveMQListener = ClientConnectionContext.of(channel).getConnectedListener();
        if (hiveMQListener == null) {
            return null;
        }

        return new ListenerImpl(hiveMQListener);

    }

    public static @NotNull ListenerType listenerTypeFromInstance(final @NotNull com.hivemq.configuration.service.entity.Listener hiveMQListener) {

        if (hiveMQListener instanceof TlsTcpListener) {
            return ListenerType.TLS_TCP_LISTENER;
        } else if (hiveMQListener instanceof TcpListener) {
            return ListenerType.TCP_LISTENER;
        } else if (hiveMQListener instanceof TlsWebsocketListener) {
            return ListenerType.TLS_WEBSOCKET_LISTENER;
        } else {
            return ListenerType.WEBSOCKET_LISTENER;
        }
    }

    public static @Nullable ClientTlsInformation getTlsInformationFromChannel(final @NotNull Channel channel) {

        Preconditions.checkNotNull(channel, "channel must never be null");

        final ClientConnectionContext clientConnectionContext = ClientConnectionContext.of(channel);
        try {
            final String cipher = clientConnectionContext.getAuthCipherSuite();
            final String protocol = clientConnectionContext.getAuthProtocol();
            final String sniHostname = clientConnectionContext.getAuthSniHostname();

            final SslClientCertificate sslClientCertificate = clientConnectionContext.getAuthCertificate();

            if (cipher == null || protocol == null) {
                return null;
            }

            if (sslClientCertificate == null) {
                return new ClientTlsInformationImpl(null, null, cipher, protocol, sniHostname);

            } else {
                final X509Certificate certificate = (X509Certificate) sslClientCertificate.certificate();
                final X509Certificate[] certificateChain = (X509Certificate[]) sslClientCertificate.certificateChain();

                return new ClientTlsInformationImpl(certificate, certificateChain, cipher, protocol, sniHostname);
            }

        } catch (final Exception e) {
            log.debug("Tls information creation failed: ", e);
        }

        return null;
    }
}
