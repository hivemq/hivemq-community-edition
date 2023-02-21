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
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.security.auth.SslClientCertificate;
import io.netty.channel.Channel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

public class UndefinedClientConnection implements ClientConnectionContext {

    final @NotNull Channel channel;
    final @NotNull PublishFlushHandler publishFlushHandler;

    volatile @NotNull ClientState clientState = ClientState.CONNECTING;
    @Nullable ProtocolVersion protocolVersion;
    @Nullable String clientId;
    boolean cleanStart;
    @Nullable ModifiableDefaultPermissions authPermissions;
    @Nullable Listener connectedListener;
    @Nullable CONNECT connectMessage;
    @Nullable Integer clientReceiveMaximum;
    @Nullable Integer connectKeepAlive;
    @Nullable Long queueSizeMaximum;
    @Nullable Long clientSessionExpiryInterval;
    @Nullable Long connectReceivedTimestamp;
    @NotNull String @Nullable [] topicAliasMapping;
    boolean clientIdAssigned;
    boolean incomingPublishesSkipRest;
    boolean requestResponseInformation;
    @Nullable Boolean requestProblemInformation;
    @Nullable SettableFuture<Void> disconnectFuture;

    @Nullable ConnectionAttributes connectionAttributes;

    boolean sendWill = true;
    boolean preventLwt;

    @Nullable SslClientCertificate authCertificate;
    @Nullable String authSniHostname;
    @Nullable String authCipherSuite;
    @Nullable String authProtocol;
    @Nullable String authUsername;
    byte @Nullable [] authPassword;
    @Nullable CONNECT authConnect;
    @Nullable String authMethod;
    @Nullable ByteBuffer authData;
    @Nullable Mqtt5UserProperties authUserProperties;
    @Nullable ScheduledFuture<?> authFuture;

    @Nullable Long maxPacketSizeSend;

    @Nullable ClientContextImpl extensionClientContext;
    @Nullable ClientEventListeners extensionClientEventListeners;
    @Nullable ClientAuthenticators extensionClientAuthenticators;
    @Nullable ClientAuthorizers extensionClientAuthorizers;
    @Nullable ClientInformation extensionClientInformation;
    @Nullable ConnectionInformation extensionConnectionInformation;

    public UndefinedClientConnection(
            final @NotNull Channel channel,
            final @NotNull PublishFlushHandler publishFlushHandler) {
        this.channel = channel;
        this.publishFlushHandler = publishFlushHandler;
    }

    @Override
    public @NotNull Channel getChannel() {
        return channel;
    }

    @Override
    public @NotNull PublishFlushHandler getPublishFlushHandler() {
        return publishFlushHandler;
    }

    @Override
    public @NotNull ClientState getClientState() {
        return clientState;
    }

    @Override
    public void proposeClientState(final @NotNull ClientState clientState) {
        if (!this.clientState.disconnected()) {
            this.clientState = clientState;
        }
    }

    @Override
    public @Nullable ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public void setProtocolVersion(final @NotNull ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @Override
    public @Nullable String getClientId() {
        return clientId;
    }

    @Override
    public void setClientId(final @NotNull String clientId) {
        this.clientId = clientId;
    }

    @Override
    public void setCleanStart(final boolean cleanStart) {
        this.cleanStart = cleanStart;
    }

    @Override
    public @Nullable ModifiableDefaultPermissions getAuthPermissions() {
        return authPermissions;
    }

    @Override
    public void setAuthPermissions(final @NotNull ModifiableDefaultPermissions authPermissions) {
        this.authPermissions = authPermissions;
    }

    @Override
    public @Nullable Listener getConnectedListener() {
        return connectedListener;
    }

    @Override
    public void setConnectedListener(final @NotNull Listener connectedListener) {
        this.connectedListener = connectedListener;
    }

    @Override
    public void setConnectMessage(final @NotNull CONNECT connectMessage) {
        this.connectMessage = connectMessage;
    }

    @Override
    public @Nullable Integer getClientReceiveMaximum() {
        return clientReceiveMaximum;
    }

    @Override
    public void setClientReceiveMaximum(final @NotNull Integer clientReceiveMaximum) {
        this.clientReceiveMaximum = clientReceiveMaximum;
    }

    public @Nullable Integer getConnectKeepAlive() {
        return connectKeepAlive;
    }

    @Override
    public void setConnectKeepAlive(final @NotNull Integer connectKeepAlive) {
        this.connectKeepAlive = connectKeepAlive;
    }

    @Override
    public @Nullable Long getQueueSizeMaximum() {
        return queueSizeMaximum;
    }

    @Override
    public void setQueueSizeMaximum(final @NotNull Long queueSizeMaximum) {
        this.queueSizeMaximum = queueSizeMaximum;
    }

    @Override
    public @Nullable Long getClientSessionExpiryInterval() {
        return clientSessionExpiryInterval;
    }

    @Override
    public void setClientSessionExpiryInterval(final @NotNull Long clientSessionExpiryInterval) {
        this.clientSessionExpiryInterval = clientSessionExpiryInterval;
    }

    /**
     * The time at which the clients CONNECT message was received by the broker.
     */
    @Override
    public @Nullable Long getConnectReceivedTimestamp() {
        return connectReceivedTimestamp;
    }

    @Override
    public void setConnectReceivedTimestamp(final @NotNull Long connectReceivedTimestamp) {
        this.connectReceivedTimestamp = connectReceivedTimestamp;
    }

    @Override
    public @NotNull String @Nullable [] getTopicAliasMapping() {
        return topicAliasMapping;
    }

    public void setTopicAliasMapping(final @NotNull String @NotNull [] topicAliasMapping) {
        this.topicAliasMapping = topicAliasMapping;
    }

    @Override
    public boolean isClientIdAssigned() {
        return clientIdAssigned;
    }

    @Override
    public void setClientIdAssigned(final boolean clientIdAssigned) {
        this.clientIdAssigned = clientIdAssigned;
    }

    /**
     * True if this client is not allowed to publish any more messages, if false he is allowed to do so.
     */
    @Override
    public boolean isIncomingPublishesSkipRest() {
        return incomingPublishesSkipRest;
    }

    @Override
    public void setIncomingPublishesSkipRest(final boolean incomingPublishesSkipRest) {
        this.incomingPublishesSkipRest = incomingPublishesSkipRest;
    }

    @Override
    public boolean isRequestResponseInformation() {
        return requestResponseInformation;
    }

    @Override
    public void setRequestResponseInformation(final boolean requestResponseInformation) {
        this.requestResponseInformation = requestResponseInformation;
    }

    @Override
    public @Nullable Boolean getRequestProblemInformation() {
        return requestProblemInformation;
    }

    @Override
    public void setRequestProblemInformation(final @NotNull Boolean requestProblemInformation) {
        this.requestProblemInformation = requestProblemInformation;
    }

    /**
     * This future is added during connection and is set when the client disconnect handling is complete.
     */
    @Override
    public @Nullable SettableFuture<Void> getDisconnectFuture() {
        return disconnectFuture;
    }

    @Override
    public void setDisconnectFuture(final @NotNull SettableFuture<Void> disconnectFuture) {
        this.disconnectFuture = disconnectFuture;
    }

    /**
     * Attribute for storing connection attributes. It is added only when connection attributes are set.
     */
    @Override
    public @Nullable ConnectionAttributes getConnectionAttributes() {
        return connectionAttributes;
    }

    @Override
    public synchronized @NotNull ConnectionAttributes setConnectionAttributesIfAbsent(
            final @NotNull ConnectionAttributes connectionAttributes) {

        if (this.connectionAttributes == null) {
            this.connectionAttributes = connectionAttributes;
        }
        return this.connectionAttributes;
    }

    public boolean isSendWill() {
        return sendWill;
    }

    @Override
    public void setSendWill(final boolean sendWill) {
        this.sendWill = sendWill;
    }

    public boolean isPreventLwt() {
        return preventLwt;
    }

    @Override
    public void setPreventLwt(final boolean preventLwt) {
        this.preventLwt = preventLwt;
    }

    @Override
    public @Nullable SslClientCertificate getAuthCertificate() {
        return authCertificate;
    }

    @Override
    public void setAuthCertificate(final @NotNull SslClientCertificate authCertificate) {
        this.authCertificate = authCertificate;
    }

    @Override
    public @Nullable String getAuthSniHostname() {
        return authSniHostname;
    }

    @Override
    public void setAuthSniHostname(final @NotNull String authSniHostname) {
        this.authSniHostname = authSniHostname;
    }

    @Override
    public @Nullable String getAuthCipherSuite() {
        return authCipherSuite;
    }

    @Override
    public void setAuthCipherSuite(final @NotNull String authCipherSuite) {
        this.authCipherSuite = authCipherSuite;
    }

    @Override
    public @Nullable String getAuthProtocol() {
        return authProtocol;
    }

    @Override
    public void setAuthProtocol(final @NotNull String authProtocol) {
        this.authProtocol = authProtocol;
    }

    public @Nullable String getAuthUsername() {
        return authUsername;
    }

    @Override
    public void setAuthUsername(final @NotNull String authUsername) {
        this.authUsername = authUsername;
    }

    public byte @Nullable [] getAuthPassword() {
        return authPassword;
    }

    @Override
    public void setAuthPassword(final byte @Nullable [] authPassword) {
        this.authPassword = authPassword;
    }

    @Override
    public @Nullable CONNECT getAuthConnect() {
        return authConnect;
    }

    @Override
    public void setAuthConnect(final @NotNull CONNECT authConnect) {
        this.authConnect = authConnect;
    }

    @Override
    public @Nullable String getAuthMethod() {
        return authMethod;
    }

    @Override
    public void setAuthMethod(final @NotNull String authMethod) {
        this.authMethod = authMethod;
    }

    @Override
    public @Nullable ByteBuffer getAuthData() {
        return authData;
    }

    @Override
    public void setAuthData(final @Nullable ByteBuffer authData) {
        this.authData = authData;
    }

    @Override
    public @Nullable Mqtt5UserProperties getAuthUserProperties() {
        return authUserProperties;
    }

    @Override
    public void setAuthUserProperties(final @NotNull Mqtt5UserProperties authUserProperties) {
        this.authUserProperties = authUserProperties;
    }

    @Override
    public @Nullable ScheduledFuture<?> getAuthFuture() {
        return authFuture;
    }

    @Override
    public void setAuthFuture(final @NotNull ScheduledFuture<?> authFuture) {
        this.authFuture = authFuture;
    }

    @Override
    public @Nullable Long getMaxPacketSizeSend() {
        return maxPacketSizeSend;
    }

    @Override
    public void setMaxPacketSizeSend(final @NotNull Long maxPacketSizeSend) {
        this.maxPacketSizeSend = maxPacketSizeSend;
    }

    @Override
    public @Nullable ClientContextImpl getExtensionClientContext() {
        return extensionClientContext;
    }

    @Override
    public void setExtensionClientContext(final @NotNull ClientContextImpl extensionClientContext) {
        this.extensionClientContext = extensionClientContext;
    }

    @Override
    public @Nullable ClientEventListeners getExtensionClientEventListeners() {
        return extensionClientEventListeners;
    }

    @Override
    public void setExtensionClientEventListeners(final @NotNull ClientEventListeners extensionClientEventListeners) {
        this.extensionClientEventListeners = extensionClientEventListeners;
    }

    @Override
    public @Nullable ClientAuthenticators getExtensionClientAuthenticators() {
        return extensionClientAuthenticators;
    }

    @Override
    public void setExtensionClientAuthenticators(final @NotNull ClientAuthenticators extensionClientAuthenticators) {
        this.extensionClientAuthenticators = extensionClientAuthenticators;
    }

    @Override
    public @Nullable ClientAuthorizers getExtensionClientAuthorizers() {
        return extensionClientAuthorizers;
    }

    @Override
    public void setExtensionClientAuthorizers(final @NotNull ClientAuthorizers extensionClientAuthorizers) {
        this.extensionClientAuthorizers = extensionClientAuthorizers;
    }

    @Override
    public @Nullable ClientInformation getExtensionClientInformation() {
        return extensionClientInformation;
    }

    @Override
    public void setExtensionClientInformation(final @NotNull ClientInformation extensionClientInformation) {
        this.extensionClientInformation = extensionClientInformation;
    }

    @Override
    public @Nullable ConnectionInformation getExtensionConnectionInformation() {
        return extensionConnectionInformation;
    }

    @Override
    public void setExtensionConnectionInformation(final @NotNull ConnectionInformation extensionConnectionInformation) {
        this.extensionConnectionInformation = extensionConnectionInformation;
    }

    @Override
    public @NotNull Optional<String> getChannelIP() {
        final Optional<InetAddress> inetAddress = getChannelAddress();

        return inetAddress.map(InetAddress::getHostAddress);
    }

    @Override
    public @NotNull Optional<InetAddress> getChannelAddress() {
        final Optional<SocketAddress> socketAddress = Optional.ofNullable(channel.remoteAddress());
        if (socketAddress.isPresent()) {
            final SocketAddress sockAddress = socketAddress.get();
            //If this is not an InetAddress, we're treating this as if there's no address
            if (sockAddress instanceof InetSocketAddress) {
                return Optional.ofNullable(((InetSocketAddress) sockAddress).getAddress());
            }
        }

        return Optional.empty();
    }
}
