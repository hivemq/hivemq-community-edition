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
package com.hivemq.extensions.packets.connack;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connack.ConnackPacket;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.2.0
 */
@Immutable
public class ConnackPacketImpl implements ConnackPacket {

    final @NotNull ConnackReasonCode reasonCode;
    final boolean sessionPresent;
    final long sessionExpiryInterval;
    final int serverKeepAlive;
    final @Nullable String assignedClientId;

    final @Nullable String authenticationMethod;
    final @Nullable ByteBuffer authenticationData;

    final int receiveMaximum;
    final int maximumPacketSize;
    final int topicAliasMaximum;
    final @Nullable Qos maximumQos;
    final boolean retainAvailable;
    final boolean wildCardSubscriptionAvailable;
    final boolean sharedSubscriptionsAvailable;
    final boolean subscriptionIdentifiersAvailable;

    final @Nullable String responseInformation;
    final @Nullable String serverReference;
    final @Nullable String reasonString;
    final @NotNull UserPropertiesImpl userProperties;

    public ConnackPacketImpl(
            final @NotNull ConnackReasonCode reasonCode,
            final boolean sessionPresent,
            final long sessionExpiryInterval,
            final int serverKeepAlive,
            final @Nullable String assignedClientId,
            final @Nullable String authenticationMethod,
            final @Nullable ByteBuffer authenticationData,
            final int receiveMaximum,
            final int maximumPacketSize,
            final int topicAliasMaximum,
            final @Nullable Qos maximumQos,
            final boolean retainAvailable,
            final boolean wildCardSubscriptionAvailable,
            final boolean sharedSubscriptionsAvailable,
            final boolean subscriptionIdentifiersAvailable,
            final @Nullable String responseInformation,
            final @Nullable String serverReference,
            final @Nullable String reasonString,
            final @NotNull UserPropertiesImpl userProperties) {

        this.reasonCode = reasonCode;
        this.sessionPresent = sessionPresent;
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.serverKeepAlive = serverKeepAlive;
        this.assignedClientId = assignedClientId;
        this.authenticationMethod = authenticationMethod;
        this.authenticationData = authenticationData;
        this.receiveMaximum = receiveMaximum;
        this.maximumPacketSize = maximumPacketSize;
        this.topicAliasMaximum = topicAliasMaximum;
        this.maximumQos = maximumQos;
        this.retainAvailable = retainAvailable;
        this.wildCardSubscriptionAvailable = wildCardSubscriptionAvailable;
        this.sharedSubscriptionsAvailable = sharedSubscriptionsAvailable;
        this.subscriptionIdentifiersAvailable = subscriptionIdentifiersAvailable;
        this.responseInformation = responseInformation;
        this.serverReference = serverReference;
        this.reasonString = reasonString;
        this.userProperties = userProperties;
    }

    public ConnackPacketImpl(final @NotNull CONNACK connack) {
        this(
                connack.getReasonCode().toConnackReasonCode(),
                connack.isSessionPresent(),
                connack.getSessionExpiryInterval(),
                connack.getServerKeepAlive(),
                connack.getAssignedClientIdentifier(),
                connack.getAuthMethod(),
                (connack.getAuthData() == null) ? null : ByteBuffer.wrap(connack.getAuthData()),
                connack.getReceiveMaximum(),
                connack.getMaximumPacketSize(),
                connack.getTopicAliasMaximum(),
                (connack.getMaximumQoS() == null) ? null : connack.getMaximumQoS().toQos(),
                connack.isRetainAvailable(),
                connack.isWildcardSubscriptionAvailable(),
                connack.isSharedSubscriptionAvailable(),
                connack.isSubscriptionIdentifierAvailable(),
                connack.getResponseInformation(),
                connack.getServerReference(),
                connack.getReasonString(),
                UserPropertiesImpl.of(connack.getUserProperties().asList()));
    }

    @Override
    public @NotNull ConnackReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public boolean getSessionPresent() {
        return sessionPresent;
    }

    @Override
    public @NotNull Optional<Long> getSessionExpiryInterval() {
        if (sessionExpiryInterval == Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET) {
            return Optional.empty();
        }
        return Optional.of(sessionExpiryInterval);
    }

    @Override
    public @NotNull Optional<Integer> getServerKeepAlive() {
        if (serverKeepAlive == Mqtt5CONNECT.KEEP_ALIVE_NOT_SET) {
            return Optional.empty();
        }
        return Optional.of(serverKeepAlive);
    }

    @Override
    public @NotNull Optional<String> getAssignedClientIdentifier() {
        return Optional.ofNullable(assignedClientId);
    }

    @Override
    public @NotNull Optional<String> getAuthenticationMethod() {
        return Optional.ofNullable(authenticationMethod);
    }

    @Override
    public @NotNull Optional<ByteBuffer> getAuthenticationData() {
        return (authenticationData == null) ? Optional.empty() : Optional.of(authenticationData.asReadOnlyBuffer());
    }

    @Override
    public int getReceiveMaximum() {
        return receiveMaximum;
    }

    @Override
    public int getMaximumPacketSize() {
        return maximumPacketSize;
    }

    @Override
    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    @Override
    public @NotNull Optional<Qos> getMaximumQoS() {
        return Optional.ofNullable(maximumQos);
    }

    @Override
    public boolean getRetainAvailable() {
        return retainAvailable;
    }

    @Override
    public boolean getWildCardSubscriptionAvailable() {
        return wildCardSubscriptionAvailable;
    }

    @Override
    public boolean getSharedSubscriptionsAvailable() {
        return sharedSubscriptionsAvailable;
    }

    @Override
    public boolean getSubscriptionIdentifiersAvailable() {
        return subscriptionIdentifiersAvailable;
    }

    @Override
    public @NotNull Optional<String> getResponseInformation() {
        return Optional.ofNullable(responseInformation);
    }

    @Override
    public @NotNull Optional<String> getServerReference() {
        return Optional.ofNullable(serverReference);
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public @NotNull UserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConnackPacketImpl)) {
            return false;
        }
        final ConnackPacketImpl that = (ConnackPacketImpl) o;
        return (reasonCode == that.reasonCode) &&
                (sessionPresent == that.sessionPresent) &&
                (sessionExpiryInterval == that.sessionExpiryInterval) &&
                (serverKeepAlive == that.serverKeepAlive) &&
                Objects.equals(assignedClientId, that.assignedClientId) &&
                Objects.equals(authenticationMethod, that.authenticationMethod) &&
                Objects.equals(authenticationData, that.authenticationData) &&
                (receiveMaximum == that.receiveMaximum) &&
                (maximumPacketSize == that.maximumPacketSize) &&
                (topicAliasMaximum == that.topicAliasMaximum) &&
                (maximumQos == that.maximumQos) &&
                (retainAvailable == that.retainAvailable) &&
                (wildCardSubscriptionAvailable == that.wildCardSubscriptionAvailable) &&
                (sharedSubscriptionsAvailable == that.sharedSubscriptionsAvailable) &&
                (subscriptionIdentifiersAvailable == that.subscriptionIdentifiersAvailable) &&
                Objects.equals(responseInformation, that.responseInformation) &&
                Objects.equals(serverReference, that.serverReference) &&
                Objects.equals(reasonString, that.reasonString) &&
                userProperties.equals(that.userProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reasonCode, sessionPresent, sessionExpiryInterval, serverKeepAlive, assignedClientId,
                authenticationMethod, authenticationData, receiveMaximum, maximumPacketSize, topicAliasMaximum,
                maximumQos, retainAvailable, wildCardSubscriptionAvailable, sharedSubscriptionsAvailable,
                subscriptionIdentifiersAvailable, responseInformation, serverReference, reasonString, userProperties);
    }
}
