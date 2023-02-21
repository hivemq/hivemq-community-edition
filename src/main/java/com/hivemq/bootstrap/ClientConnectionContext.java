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
package com.hivemq.bootstrap;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.client.ClientAuthenticators;
import com.hivemq.extensions.client.ClientAuthorizers;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.client.parameter.ConnectionAttributes;
import com.hivemq.extensions.events.client.parameters.ClientEventListeners;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.security.auth.SslClientCertificate;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * The ClientContext contains information about the client that are required to provide them with the functionality of
 * our feature sets.
 * <p>
 * We differentiate between two {@link ClientConnectionContext} implementations to have the most guarantees and
 * immutability of the context information. For example: The client id of a client should not be {@code null} anymore
 * after establishing successfully an MQTT connection.
 * <p>
 * Initially every client starts early in their connection with {@link UndefinedClientConnection} which has loose guarantees
 * like the client id being nullable as they may have not yet been initialized.
 * <p>
 * At some point during the transition of the client lifecycle between connecting and connected which currently happens
 * in the {@link com.hivemq.mqtt.handler.connect.ConnectHandler} we can change to the {@link ClientConnection}.
 * <p>
 * DISCLAIMER: Even when we have this differentiation, we can truly benefit from the guarantees of
 * {@link ClientConnection} when we are using the {@link com.hivemq.persistence.connection.ConnectionPersistence} to
 * obtain it, as we can only then be sure which {@link ClientConnectionContext} implementation we are using.
 */
public interface ClientConnectionContext {

    static @NotNull ClientConnectionContext get(final @NotNull Channel channel) {
        ClientConnectionContext context = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        if (context != null) {
            return context;
        }
        context = channel.attr(UndefinedClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        if (context != null) {
            return context;
        }
        // This should never happen.
        throw new IllegalStateException("Channel has no ClientConnectionContext.");
    }

    @NotNull Channel getChannel();

    @NotNull ChannelHandler getPublishFlushHandler();

    @NotNull ClientState getClientState();

    void proposeClientState(@NotNull ClientState authenticating);

    @Nullable String getClientId();

    void setClientId(@NotNull String clientId);

    @Nullable ProtocolVersion getProtocolVersion();

    void setProtocolVersion(@NotNull ProtocolVersion protocolVersion);

    @Nullable Long getConnectReceivedTimestamp();

    void setConnectReceivedTimestamp(@NotNull Long currentTimeMillis);

    void setCleanStart(boolean cleanStart);

    boolean isClientIdAssigned();

    void setClientIdAssigned(boolean clientIdAssigned);

    void setAuthUsername(@NotNull String username);

    void setAuthPassword(byte @NotNull [] password);

    @Nullable Long getClientSessionExpiryInterval();

    void setClientSessionExpiryInterval(@NotNull Long sessionExpiryInterval);

    void setConnectKeepAlive(@NotNull Integer keepAlive);

    @Nullable Long getMaxPacketSizeSend();

    void setMaxPacketSizeSend(@NotNull Long maximumPacketSize);

    @Nullable Listener getConnectedListener();

    void setConnectedListener(@NotNull Listener listener);

    @Nullable Integer getClientReceiveMaximum();

    void setClientReceiveMaximum(@NotNull Integer clientReceiveMaximum);

    @Nullable Long getQueueSizeMaximum();

    void setQueueSizeMaximum(@NotNull Long queueSizeMaximum);

    @Nullable ScheduledFuture<?> getAuthFuture();

    void setAuthFuture(@NotNull ScheduledFuture<?> authFuture);

    void setDisconnectFuture(@NotNull SettableFuture<Void> disconnectFuture);

    boolean isRequestResponseInformation();

    void setRequestResponseInformation(boolean responseInformationRequested);

    @Nullable Boolean getRequestProblemInformation();

    void setRequestProblemInformation(Boolean problemInformationRequested);

    void setConnectMessage(@NotNull CONNECT msg);

    @NotNull String @Nullable [] getTopicAliasMapping();

    void setTopicAliasMapping(@NotNull String @NotNull [] strings);

    @Nullable String getAuthMethod();

    void setAuthMethod(@NotNull String authMethod);

    @Nullable Mqtt5UserProperties getAuthUserProperties();

    @Nullable ByteBuffer getAuthData();

    void setSendWill(boolean sendWill);

    void setPreventLwt(boolean preventLwt);

    @Nullable SettableFuture<Void> getDisconnectFuture();

    @Nullable CONNECT getAuthConnect();

    void setAuthConnect(@NotNull CONNECT connect);

    void setAuthData(@Nullable ByteBuffer authenticationData);

    void setAuthUserProperties(@NotNull Mqtt5UserProperties mqtt5UserProperties);

    @Nullable String getAuthCipherSuite();

    void setAuthCipherSuite(@NotNull String cipherSuite);

    @Nullable String getAuthProtocol();

    void setAuthProtocol(@NotNull String protocol);

    @Nullable SslClientCertificate getAuthCertificate();

    void setAuthCertificate(@NotNull SslClientCertificate sslClientCertificate);

    @Nullable String getAuthSniHostname();

    void setAuthSniHostname(@NotNull String hostname);

    @Nullable ClientContextImpl getExtensionClientContext();

    void setExtensionClientContext(@NotNull ClientContextImpl clientContext);

    @Nullable ClientAuthenticators getExtensionClientAuthenticators();

    void setExtensionClientAuthenticators(@NotNull ClientAuthenticators clientAuthenticators);

    @Nullable ModifiableDefaultPermissions getAuthPermissions();

    void setAuthPermissions(@NotNull ModifiableDefaultPermissions defaultPermissions);

    @Nullable ClientInformation getExtensionClientInformation();

    void setExtensionClientInformation(@NotNull ClientInformation clientInformation);

    @Nullable ConnectionInformation getExtensionConnectionInformation();

    void setExtensionConnectionInformation(@NotNull ConnectionInformation connectionInformation);

    @Nullable ClientAuthorizers getExtensionClientAuthorizers();

    void setExtensionClientAuthorizers(@NotNull ClientAuthorizers clientAuthorizers);

    @Nullable ClientEventListeners getExtensionClientEventListeners();

    void setExtensionClientEventListeners(@NotNull ClientEventListeners clientEventListeners);

    boolean isIncomingPublishesSkipRest();

    void setIncomingPublishesSkipRest(boolean incomingPublishesSkipRest);

    @Nullable ConnectionAttributes getConnectionAttributes();

    @NotNull ConnectionAttributes setConnectionAttributesIfAbsent(@NotNull ConnectionAttributes connectionAttributes);

    @NotNull Optional<String> getChannelIP();

    @NotNull Optional<InetAddress> getChannelAddress();
}
