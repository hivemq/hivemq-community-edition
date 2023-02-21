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

import com.google.common.annotations.VisibleForTesting;
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
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.pool.SequentialMessageIDPoolImpl;
import com.hivemq.security.auth.SslClientCertificate;
import io.netty.channel.Channel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ClientConnection implements ClientConnectionContext {

    private final @NotNull Channel channel;
    private final @NotNull PublishFlushHandler publishFlushHandler;
    private final @NotNull MessageIDPool messageIDPool = new SequentialMessageIDPoolImpl();
    private volatile @NotNull ClientState clientState;

    private @NotNull ProtocolVersion protocolVersion;
    private @NotNull String clientId;
    private boolean cleanStart;
    private @Nullable ModifiableDefaultPermissions authPermissions;
    private @Nullable Listener connectedListener;
    private @Nullable CONNECT connectMessage;
    private @Nullable AtomicInteger inFlightMessageCount;
    private @Nullable Integer clientReceiveMaximum;
    private @Nullable Integer connectKeepAlive;
    private @Nullable Long queueSizeMaximum;
    private @Nullable Long clientSessionExpiryInterval;
    private @Nullable Long connectReceivedTimestamp;
    private @Nullable Long maxPacketSizeSend;
    private @NotNull String @Nullable [] topicAliasMapping;
    private boolean noSharedSubscription;
    private boolean clientIdAssigned;
    private boolean incomingPublishesSkipRest;
    private boolean incomingPublishesDefaultFailedSkipRest;
    private boolean requestResponseInformation;
    private @Nullable Boolean requestProblemInformation;
    private @Nullable SettableFuture<Void> disconnectFuture;

    private @Nullable ConnectionAttributes connectionAttributes;

    private boolean sendWill;
    private boolean preventLwt;
    private boolean inFlightMessagesSent;

    private @Nullable SslClientCertificate authCertificate;
    private @Nullable String authSniHostname;
    private @Nullable String authCipherSuite;
    private @Nullable String authProtocol;
    private @Nullable String authUsername;
    private byte @Nullable [] authPassword;
    private @Nullable CONNECT authConnect;
    private @Nullable String authMethod;
    private @Nullable ByteBuffer authData;
    private @Nullable Mqtt5UserProperties authUserProperties;
    private @Nullable ScheduledFuture<?> authFuture;

    private @Nullable ClientContextImpl extensionClientContext;
    private @Nullable ClientEventListeners extensionClientEventListeners;
    private @Nullable ClientAuthenticators extensionClientAuthenticators;
    private @Nullable ClientAuthorizers extensionClientAuthorizers;
    private @Nullable ClientInformation extensionClientInformation;
    private @Nullable ConnectionInformation extensionConnectionInformation;

    public static @NotNull ClientConnection of(final @NotNull Channel channel) {

        final ClientConnectionContext clientConnectionContext =
                channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).get();

        checkArgument(clientConnectionContext instanceof ClientConnection);

        return (ClientConnection) clientConnectionContext;
    }

    public static @NotNull ClientConnection from(final @NotNull ClientConnectionContext clientConnectionContext) {
        checkArgument(clientConnectionContext instanceof UndefinedClientConnection);

        final UndefinedClientConnection context = (UndefinedClientConnection) clientConnectionContext;

        checkNotNull(context.clientId, "Client id must not be null.");
        checkNotNull(context.clientState, "Client state must not be null.");
        checkNotNull(context.protocolVersion, "Protocol version must not be null.");

        final ClientConnection clientConnection = new ClientConnection(
                context.channel,
                context.publishFlushHandler,
                context.clientState,
                context.protocolVersion,
                context.clientId,
                context.cleanStart,
                context.authPermissions,
                context.connectedListener,
                context.connectMessage,
                context.clientReceiveMaximum,
                context.connectKeepAlive,
                context.queueSizeMaximum,
                context.clientSessionExpiryInterval,
                context.connectReceivedTimestamp,
                context.topicAliasMapping,
                context.clientIdAssigned,
                context.incomingPublishesSkipRest,
                context.requestResponseInformation,
                context.requestProblemInformation,
                context.disconnectFuture,
                context.connectionAttributes,
                context.sendWill,
                context.preventLwt,
                context.authCertificate,
                context.authSniHostname,
                context.authCipherSuite,
                context.authProtocol,
                context.authUsername,
                context.authPassword,
                context.authConnect,
                context.authMethod,
                context.authData,
                context.authUserProperties,
                context.authFuture,
                context.maxPacketSizeSend,
                context.extensionClientContext,
                context.extensionClientEventListeners,
                context.extensionClientAuthenticators,
                context.extensionClientAuthorizers,
                context.extensionClientInformation,
                context.extensionConnectionInformation);

        context.getChannel().attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        return clientConnection;
    }

    public ClientConnection(
            final @NotNull Channel channel,
            final @NotNull PublishFlushHandler publishFlushHandler,
            final @NotNull ClientState clientState,
            final @NotNull ProtocolVersion protocolVersion,
            final @NotNull String clientId,
            final boolean cleanStart,
            final @Nullable ModifiableDefaultPermissions authPermissions,
            final @Nullable Listener connectedListener,
            final @Nullable CONNECT connectMessage,
            final @Nullable Integer clientReceiveMaximum,
            final @Nullable Integer connectKeepAlive,
            final @Nullable Long queueSizeMaximum,
            final @Nullable Long clientSessionExpiryInterval,
            final @Nullable Long connectReceivedTimestamp,
            final @NotNull String @Nullable [] topicAliasMapping,
            final boolean clientIdAssigned,
            final boolean incomingPublishesSkipRest,
            final boolean requestResponseInformation,
            final @Nullable Boolean requestProblemInformation,
            final @Nullable SettableFuture<Void> disconnectFuture,
            final @Nullable ConnectionAttributes connectionAttributes,
            final boolean sendWill,
            final boolean preventLwt,
            final @Nullable SslClientCertificate authCertificate,
            final @Nullable String authSniHostname,
            final @Nullable String authCipherSuite,
            final @Nullable String authProtocol,
            final @Nullable String authUsername,
            final byte @Nullable [] authPassword,
            final @Nullable CONNECT authConnect,
            final @Nullable String authMethod,
            final @Nullable ByteBuffer authData,
            final @Nullable Mqtt5UserProperties authUserProperties,
            final @Nullable ScheduledFuture<?> authFuture,
            final @Nullable Long maxPacketSizeSend,
            final @Nullable ClientContextImpl extensionClientContext,
            final @Nullable ClientEventListeners extensionClientEventListeners,
            final @Nullable ClientAuthenticators extensionClientAuthenticators,
            final @Nullable ClientAuthorizers extensionClientAuthorizers,
            final @Nullable ClientInformation extensionClientInformation,
            final @Nullable ConnectionInformation extensionConnectionInformation) {
        this.channel = channel;
        this.publishFlushHandler = publishFlushHandler;
        this.clientState = clientState;
        this.protocolVersion = protocolVersion;
        this.clientId = clientId;
        this.cleanStart = cleanStart;
        this.authPermissions = authPermissions;
        this.connectedListener = connectedListener;
        this.connectMessage = connectMessage;
        this.clientReceiveMaximum = clientReceiveMaximum;
        this.connectKeepAlive = connectKeepAlive;
        this.queueSizeMaximum = queueSizeMaximum;
        this.clientSessionExpiryInterval = clientSessionExpiryInterval;
        this.connectReceivedTimestamp = connectReceivedTimestamp;
        this.topicAliasMapping = topicAliasMapping;
        this.clientIdAssigned = clientIdAssigned;
        this.incomingPublishesSkipRest = incomingPublishesSkipRest;
        this.requestResponseInformation = requestResponseInformation;
        this.requestProblemInformation = requestProblemInformation;
        this.disconnectFuture = disconnectFuture;
        this.connectionAttributes = connectionAttributes;
        this.sendWill = sendWill;
        this.preventLwt = preventLwt;
        this.authCertificate = authCertificate;
        this.authSniHostname = authSniHostname;
        this.authCipherSuite = authCipherSuite;
        this.authProtocol = authProtocol;
        this.authUsername = authUsername;
        this.authPassword = authPassword;
        this.authConnect = authConnect;
        this.authMethod = authMethod;
        this.authData = authData;
        this.authUserProperties = authUserProperties;
        this.authFuture = authFuture;
        this.maxPacketSizeSend = maxPacketSizeSend;
        this.extensionClientContext = extensionClientContext;
        this.extensionClientEventListeners = extensionClientEventListeners;
        this.extensionClientAuthenticators = extensionClientAuthenticators;
        this.extensionClientAuthorizers = extensionClientAuthorizers;
        this.extensionClientInformation = extensionClientInformation;
        this.extensionConnectionInformation = extensionConnectionInformation;
    }

    @Override
    public @NotNull Channel getChannel() {
        return channel;
    }

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

    // ONLY VISIBLE FOR TESTING !!!
    // DO NOT USE IN PROD !!!
    @VisibleForTesting()
    public void setClientStateUnsafe(final @NotNull ClientState clientState) {
        this.clientState = clientState;
    }

    @Override
    public @Nullable ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public void setProtocolVersion(final @Nullable ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @Override
    public @NotNull String getClientId() {
        return clientId;
    }

    public void setClientId(final @NotNull String clientId) {
        this.clientId = clientId;
    }

    public boolean isCleanStart() {
        return cleanStart;
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

    /**
     * This key contains the actual listener a client connected to.
     */
    public @Nullable Listener getConnectedListener() {
        return connectedListener;
    }

    public void setConnectedListener(final @NotNull Listener connectedListener) {
        this.connectedListener = connectedListener;
    }

    public @Nullable CONNECT getConnectMessage() {
        return connectMessage;
    }

    @Override
    public void setConnectMessage(final @NotNull CONNECT connectMessage) {
        this.connectMessage = connectMessage;
    }

    /**
     * The amount of messages that have been polled but not yet delivered.
     */
    public @Nullable AtomicInteger getInFlightMessageCount() {
        return inFlightMessageCount;
    }

    public void setInFlightMessageCount(final @Nullable AtomicInteger inFlightMessageCount) {
        this.inFlightMessageCount = inFlightMessageCount;
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

    public @NotNull MessageIDPool getMessageIDPool() {
        return messageIDPool;
    }

    /**
     * The amount of messages that have been polled but not yet delivered.
     */
    public int inFlightMessageCount() {
        if (inFlightMessageCount == null) {
            return 0;
        }
        return inFlightMessageCount.get();
    }

    public int decrementInFlightCount() {
        if (inFlightMessageCount == null) {
            return 0;
        }
        return inFlightMessageCount.decrementAndGet();
    }

    public int incrementInFlightCount() {
        if (inFlightMessageCount == null) {
            inFlightMessageCount = new AtomicInteger();
        }
        return inFlightMessageCount.incrementAndGet();
    }

    /**
     * Attribute for storing the client session expiry interval.
     */
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
    public @Nullable Long getMaxPacketSizeSend() {
        return maxPacketSizeSend;
    }

    @Override
    public void setMaxPacketSizeSend(final @NotNull Long maxPacketSizeSend) {
        this.maxPacketSizeSend = maxPacketSizeSend;
    }

    @Override
    public @NotNull String @Nullable [] getTopicAliasMapping() {
        return topicAliasMapping;
    }

    public void setTopicAliasMapping(final @NotNull String @NotNull [] topicAliasMapping) {
        this.topicAliasMapping = topicAliasMapping;
    }

    /**
     * True if it is guarantied that this client has no shared subscriptions, if false it is unclear.
     */
    public boolean getNoSharedSubscription() {
        return noSharedSubscription;
    }

    public void setNoSharedSubscription(final boolean noSharedSubscription) {
        this.noSharedSubscription = noSharedSubscription;
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

    /**
     * True if this client is not allowed to publish any more messages by default, if false he is allowed to do so.
     */
    public boolean isIncomingPublishesDefaultFailedSkipRest() {
        return incomingPublishesDefaultFailedSkipRest;
    }

    public void setIncomingPublishesDefaultFailedSkipRest(final boolean incomingPublishesDefaultFailedSkipRest) {
        this.incomingPublishesDefaultFailedSkipRest = incomingPublishesDefaultFailedSkipRest;
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
     * This attribute is added during connection. The future is set, when the client disconnect handling is complete.
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

    /**
     * This reveres to the in-flight messages in the client queue, not the ones in the ordered topic queue.
     */
    public boolean isInFlightMessagesSent() {
        return inFlightMessagesSent;
    }

    public void setInFlightMessagesSent(final boolean inFlightMessagesSent) {
        this.inFlightMessagesSent = inFlightMessagesSent;
    }

    public boolean isMessagesInFlight() {
        return !inFlightMessagesSent || inFlightMessageCount() > 0;
    }

    @Override
    public @Nullable SslClientCertificate getAuthCertificate() {
        return authCertificate;
    }

    @Override
    public void setAuthCertificate(final @NotNull SslClientCertificate authCertificate) {
        this.authCertificate = authCertificate;
    }

    /**
     * This contains the SNI hostname sent by the client if TLS SNI is used.
     */
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
    public @Nullable ClientContextImpl getExtensionClientContext() {
        return extensionClientContext;
    }

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
    public @Nullable ClientAuthenticators getExtensionClientAuthenticators() {
        return extensionClientAuthenticators;
    }

    @Override
    public void setExtensionClientAuthenticators(final @NotNull ClientAuthenticators extensionClientAuthenticators) {
        this.extensionClientAuthenticators = extensionClientAuthenticators;
    }

    public int getMaxInflightWindow(final int defaultMaxInflightWindow) {
        if (clientReceiveMaximum == null) {
            return defaultMaxInflightWindow;
        }
        return Math.min(clientReceiveMaximum, defaultMaxInflightWindow);
    }

    public @NotNull Optional<String> getChannelIP() {
        final Optional<InetAddress> inetAddress = getChannelAddress();

        return inetAddress.map(InetAddress::getHostAddress);
    }

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
