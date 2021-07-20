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

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Daniel Krüger
 */
public class ClientConnection {

    private final @NotNull PublishFlushHandler publishFlushHandler;
    private @Nullable ProtocolVersion protocolVersion;
    private @Nullable ModifiableDefaultPermissions authPermissions;
    private @Nullable Listener connectedListener;
    private @Nullable CONNECT connectMessage;
    private @Nullable AtomicInteger inFlightMessages;
    private @Nullable Integer clientReceiveMaximum;
    private @Nullable Long queueSizeMaximum;
    private @Nullable Long clientSessionExpiryInterval;
    private @Nullable Long connectReceivedTimestamp;
    private @Nullable Long maxPacketSizeSend;
    private @Nullable String[] topicAliasMapping;
    private boolean noSharedSubscription;
    private boolean clientIdAssigned;
    private boolean disconnectEventLogged;
    private boolean incomingPublishesSkipRest;
    private boolean incomingPublishesDefaultFailedSkipRest;
    private boolean requestResponseInformation;
    private @Nullable Boolean requestProblemInformation;
    private @Nullable SettableFuture<Void> disconnectFuture;

    private final Object connectionAttributesMutex = new Object();
    private @Nullable ConnectionAttributes connectionAttributes;

    private boolean gracefulDisconnect;
    private boolean sendWill = true;
    private boolean takenOver;
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
    private boolean authOngoing;
    private boolean reAuthOngoing;
    private boolean authAuthenticated;
    private boolean authenticatedOrAuthenticationBypassed;
    private @Nullable ScheduledFuture<?> authFuture;

    private boolean extensionConnectEventSent;
    private boolean extensionDisconnectEventSent;
    private @Nullable ClientContextImpl extensionClientContext;
    private @Nullable ClientEventListeners extensionClientEventListeners;
    private @Nullable ClientAuthenticators extensionClientAuthenticators;
    private @Nullable ClientAuthorizers extensionClientAuthorizers;
    private @Nullable ClientInformation extensionClientInformation;
    private @Nullable ConnectionInformation extensionConnectionInformation;

    public ClientConnection(final @NotNull PublishFlushHandler publishFlushHandler) {
        this.publishFlushHandler = publishFlushHandler;
    }

    public @NotNull PublishFlushHandler getPublishFlushHandler() {
        return publishFlushHandler;
    }

    public @Nullable ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(final @Nullable ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public @Nullable ModifiableDefaultPermissions getAuthPermissions() {
        return authPermissions;
    }

    public void setAuthPermissions(final @NotNull ModifiableDefaultPermissions authPermissions) {
        this.authPermissions = authPermissions;
    }

    /**
     * This key contains the actual listener a client connected to.
     */
    public @Nullable Listener getConnectedListener() {
        return connectedListener;
    }

    public void setConnectedListener(final @Nullable Listener connectedListener) {
        this.connectedListener = connectedListener;
    }

    public @Nullable CONNECT getConnectMessage() {
        return connectMessage;
    }

    public void setConnectMessage(final @Nullable CONNECT connectMessage) {
        this.connectMessage = connectMessage;
    }

    /**
     * The amount of messages that have been polled but not yet delivered.
     */
    public @Nullable AtomicInteger getInFlightMessages() {
        return inFlightMessages;
    }

    public void setInFlightMessages(final @Nullable AtomicInteger inFlightMessages) {
        this.inFlightMessages = inFlightMessages;
    }

    public @Nullable Integer getClientReceiveMaximum() {
        return clientReceiveMaximum;
    }

    public void setClientReceiveMaximum(final @Nullable Integer clientReceiveMaximum) {
        this.clientReceiveMaximum = clientReceiveMaximum;
    }

    public @Nullable Long getQueueSizeMaximum() {
        return queueSizeMaximum;
    }

    public void setQueueSizeMaximum(final @Nullable Long queueSizeMaximum) {
        this.queueSizeMaximum = queueSizeMaximum;
    }

    /**
     * Attribute for storing the client session expiry interval.
     */
    public @Nullable Long getClientSessionExpiryInterval() {
        return clientSessionExpiryInterval;
    }

    public void setClientSessionExpiryInterval(final @Nullable Long clientSessionExpiryInterval) {
        this.clientSessionExpiryInterval = clientSessionExpiryInterval;
    }

    /**
     * The time at which the clients CONNECT message was received by the broker.
     */
    public @Nullable Long getConnectReceivedTimestamp() {
        return connectReceivedTimestamp;
    }

    public void setConnectReceivedTimestamp(final @Nullable Long connectReceivedTimestamp) {
        this.connectReceivedTimestamp = connectReceivedTimestamp;
    }

    public @Nullable Long getMaxPacketSizeSend() {
        return maxPacketSizeSend;
    }

    public void setMaxPacketSizeSend(final @Nullable Long maxPacketSizeSend) {
        this.maxPacketSizeSend = maxPacketSizeSend;
    }

    public @Nullable String[] getTopicAliasMapping() {
        return topicAliasMapping;
    }

    public void setTopicAliasMapping(final @Nullable String[] topicAliasMapping) {
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

    public boolean isClientIdAssigned() {
        return clientIdAssigned;
    }

    public void setClientIdAssigned(final boolean clientIdAssigned) {
        this.clientIdAssigned = clientIdAssigned;
    }

    public boolean isDisconnectEventLogged() {
        return disconnectEventLogged;
    }

    public void setDisconnectEventLogged(final boolean disconnectEventLogged) {
        this.disconnectEventLogged = disconnectEventLogged;
    }

    /**
     * True if this client is not allowed to publish any more messages, if false he is allowed to do so.
     */
    public boolean isIncomingPublishesSkipRest() {
        return incomingPublishesSkipRest;
    }

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

    public boolean isRequestResponseInformation() {
        return requestResponseInformation;
    }

    public void setRequestResponseInformation(final boolean requestResponseInformation) {
        this.requestResponseInformation = requestResponseInformation;
    }

    public @Nullable Boolean getRequestProblemInformation() {
        return requestProblemInformation;
    }

    public void setRequestProblemInformation(final @Nullable Boolean requestProblemInformation) {
        this.requestProblemInformation = requestProblemInformation;
    }

    /**
     * This attribute is added during connection. The future is set, when the client disconnect handling is complete.
     */
    public @Nullable SettableFuture<Void> getDisconnectFuture() {
        return disconnectFuture;
    }

    public void setDisconnectFuture(final @Nullable SettableFuture<Void> disconnectFuture) {
        this.disconnectFuture = disconnectFuture;
    }

    /**
     * Attribute for storing connection attributes. It is added only when connection attributes are set.
     */
    public @Nullable ConnectionAttributes getConnectionAttributes() {
        return connectionAttributes;
    }

    public void setConnectionAttributes(final @Nullable ConnectionAttributes connectionAttributes) {
        this.connectionAttributes = connectionAttributes;
    }

    public @NotNull ConnectionAttributes setConnectionAttributesIfAbsent(
            final @NotNull ConnectionAttributes connectionAttributes) {

        synchronized (connectionAttributesMutex) {
            if (this.connectionAttributes == null) {
                this.connectionAttributes = connectionAttributes;
            }
            return this.connectionAttributes;
        }
    }

    public boolean isGracefulDisconnect() {
        return gracefulDisconnect;
    }

    public void setGracefulDisconnect(final boolean gracefulDisconnect) {
        this.gracefulDisconnect = gracefulDisconnect;
    }

    public boolean isSendWill() {
        return sendWill;
    }

    public void setSendWill(final boolean sendWill) {
        this.sendWill = sendWill;
    }

    public boolean isTakenOver() {
        return takenOver;
    }

    public void setTakenOver(final boolean takenOver) {
        this.takenOver = takenOver;
    }

    public boolean isPreventLwt() {
        return preventLwt;
    }

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

    public @Nullable SslClientCertificate getAuthCertificate() {
        return authCertificate;
    }

    public void setAuthCertificate(final @Nullable SslClientCertificate authCertificate) {
        this.authCertificate = authCertificate;
    }

    /**
     * This contains the SNI hostname sent by the client if TLS SNI is used.
     */
    public @Nullable String getAuthSniHostname() {
        return authSniHostname;
    }

    public void setAuthSniHostname(final @Nullable String authSniHostname) {
        this.authSniHostname = authSniHostname;
    }

    public @Nullable String getAuthCipherSuite() {
        return authCipherSuite;
    }

    public void setAuthCipherSuite(final @Nullable String authCipherSuite) {
        this.authCipherSuite = authCipherSuite;
    }

    public @Nullable String getAuthProtocol() {
        return authProtocol;
    }

    public void setAuthProtocol(final @Nullable String authProtocol) {
        this.authProtocol = authProtocol;
    }

    public @Nullable String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(final @Nullable String authUsername) {
        this.authUsername = authUsername;
    }

    public byte @Nullable [] getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(final byte @Nullable [] authPassword) {
        this.authPassword = authPassword;
    }

    public @Nullable CONNECT getAuthConnect() {
        return authConnect;
    }

    public void setAuthConnect(final @Nullable CONNECT authConnect) {
        this.authConnect = authConnect;
    }

    public @Nullable String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(final @Nullable String authMethod) {
        this.authMethod = authMethod;
    }

    public @Nullable ByteBuffer getAuthData() {
        return authData;
    }

    public void setAuthData(final @Nullable ByteBuffer authData) {
        this.authData = authData;
    }

    public @Nullable Mqtt5UserProperties getAuthUserProperties() {
        return authUserProperties;
    }

    public void setAuthUserProperties(final @Nullable Mqtt5UserProperties authUserProperties) {
        this.authUserProperties = authUserProperties;
    }

    public boolean isAuthOngoing() {
        return authOngoing;
    }

    public void setAuthOngoing(final boolean authOngoing) {
        this.authOngoing = authOngoing;
    }

    public boolean isReAuthOngoing() {
        return reAuthOngoing;
    }

    public void setReAuthOngoing(final boolean reAuthOngoing) {
        this.reAuthOngoing = reAuthOngoing;
    }

    public boolean isAuthAuthenticated() {
        return authAuthenticated;
    }

    public void setAuthAuthenticated(final boolean authAuthenticated) {
        this.authAuthenticated = authAuthenticated;
    }

    public boolean isAuthenticatedOrAuthenticationBypassed() {
        return authenticatedOrAuthenticationBypassed;
    }

    public void setAuthenticatedOrAuthenticationBypassed(final boolean authenticatedOrAuthenticationBypassed) {
        this.authenticatedOrAuthenticationBypassed = authenticatedOrAuthenticationBypassed;
    }

    public @Nullable ScheduledFuture<?> getAuthFuture() {
        return authFuture;
    }

    public void setAuthFuture(final @Nullable ScheduledFuture<?> authFuture) {
        this.authFuture = authFuture;
    }

    public boolean isExtensionConnectEventSent() {
        return extensionConnectEventSent;
    }

    public void setExtensionConnectEventSent(final boolean extensionConnectEventSent) {
        this.extensionConnectEventSent = extensionConnectEventSent;
    }

    public boolean isExtensionDisconnectEventSent() {
        return extensionDisconnectEventSent;
    }

    public void setExtensionDisconnectEventSent(final boolean extensionDisconnectEventSent) {
        this.extensionDisconnectEventSent = extensionDisconnectEventSent;
    }

    public @Nullable ClientContextImpl getExtensionClientContext() {
        return extensionClientContext;
    }

    public void setExtensionClientContext(final @Nullable ClientContextImpl extensionClientContext) {
        this.extensionClientContext = extensionClientContext;
    }

    public @Nullable ClientEventListeners getExtensionClientEventListeners() {
        return extensionClientEventListeners;
    }

    public void setExtensionClientEventListeners(final @Nullable ClientEventListeners extensionClientEventListeners) {
        this.extensionClientEventListeners = extensionClientEventListeners;
    }

    public @Nullable ClientAuthorizers getExtensionClientAuthorizers() {
        return extensionClientAuthorizers;
    }

    public void setExtensionClientAuthorizers(final @Nullable ClientAuthorizers extensionClientAuthorizers) {
        this.extensionClientAuthorizers = extensionClientAuthorizers;
    }

    public @Nullable ClientInformation getExtensionClientInformation() {
        return extensionClientInformation;
    }

    public void setExtensionClientInformation(final @Nullable ClientInformation extensionClientInformation) {
        this.extensionClientInformation = extensionClientInformation;
    }

    public @Nullable ConnectionInformation getExtensionConnectionInformation() {
        return extensionConnectionInformation;
    }

    public void setExtensionConnectionInformation(final @Nullable ConnectionInformation extensionConnectionInformation) {
        this.extensionConnectionInformation = extensionConnectionInformation;
    }

    public @Nullable ClientAuthenticators getExtensionClientAuthenticators() {
        return extensionClientAuthenticators;
    }

    public void setExtensionClientAuthenticators(final @Nullable ClientAuthenticators extensionClientAuthenticators) {
        this.extensionClientAuthenticators = extensionClientAuthenticators;
    }
}
