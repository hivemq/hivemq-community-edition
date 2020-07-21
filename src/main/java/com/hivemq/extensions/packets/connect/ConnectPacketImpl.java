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
package com.hivemq.extensions.packets.connect;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extensions.packets.general.MqttVersionUtil;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.packets.publish.WillPublishPacketImpl;
import com.hivemq.mqtt.message.connect.CONNECT;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Georg Held
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@Immutable
public class ConnectPacketImpl implements ConnectPacket {

    final @NotNull MqttVersion mqttVersion;
    final @NotNull String clientId;
    final boolean cleanStart;
    final long sessionExpiryInterval;
    final int keepAlive;

    final int receiveMaximum;
    final long maximumPacketSize;
    final int topicAliasMaximum;
    final boolean requestProblemInformation;
    final boolean requestResponseInformation;

    final @Nullable String userName;
    final @Nullable ByteBuffer password;
    final @Nullable String authenticationMethod;
    final @Nullable ByteBuffer authenticationData;

    final @Nullable WillPublishPacketImpl willPublish;
    final @NotNull UserPropertiesImpl userProperties;

    public ConnectPacketImpl(
            final @NotNull MqttVersion mqttVersion,
            final @NotNull String clientId,
            final boolean cleanStart,
            final long sessionExpiryInterval,
            final int keepAlive,
            final int receiveMaximum,
            final long maximumPacketSize,
            final int topicAliasMaximum,
            final boolean requestProblemInformation,
            final boolean requestResponseInformation,
            final @Nullable String userName,
            final @Nullable ByteBuffer password,
            final @Nullable String authenticationMethod,
            final @Nullable ByteBuffer authenticationData,
            final @Nullable WillPublishPacketImpl willPublish,
            final @NotNull UserPropertiesImpl userProperties) {

        this.mqttVersion = mqttVersion;
        this.clientId = clientId;
        this.cleanStart = cleanStart;
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.keepAlive = keepAlive;
        this.receiveMaximum = receiveMaximum;
        this.maximumPacketSize = maximumPacketSize;
        this.topicAliasMaximum = topicAliasMaximum;
        this.requestProblemInformation = requestProblemInformation;
        this.requestResponseInformation = requestResponseInformation;
        this.userName = userName;
        this.password = password;
        this.authenticationMethod = authenticationMethod;
        this.authenticationData = authenticationData;
        this.willPublish = willPublish;
        this.userProperties = userProperties;
    }

    public ConnectPacketImpl(final @NotNull CONNECT connect, final long timestamp) {
        this(
                MqttVersionUtil.toMqttVersion(connect.getProtocolVersion()),
                connect.getClientIdentifier(),
                connect.isCleanStart(),
                connect.getSessionExpiryInterval(),
                connect.getKeepAlive(),
                connect.getReceiveMaximum(),
                connect.getMaximumPacketSize(),
                connect.getTopicAliasMaximum(),
                connect.isProblemInformationRequested(),
                connect.isResponseInformationRequested(),
                connect.getUsername(),
                (connect.getPassword() == null) ? null : ByteBuffer.wrap(connect.getPassword()),
                connect.getAuthMethod(),
                (connect.getAuthData() == null) ? null : ByteBuffer.wrap(connect.getAuthData()),
                (connect.getWillPublish() == null) ? null : new WillPublishPacketImpl(connect.getWillPublish(), timestamp),
                UserPropertiesImpl.of(connect.getUserProperties().asList()));
    }

    @Override
    public @NotNull MqttVersion getMqttVersion() {
        return mqttVersion;
    }

    @Override
    public @NotNull String getClientId() {
        return clientId;
    }

    @Override
    public boolean getCleanStart() {
        return cleanStart;
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    @Override
    public int getKeepAlive() {
        return keepAlive;
    }

    @Override
    public int getReceiveMaximum() {
        return receiveMaximum;
    }

    @Override
    public long getMaximumPacketSize() {
        return maximumPacketSize;
    }

    @Override
    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    @Override
    public boolean getRequestProblemInformation() {
        return requestProblemInformation;
    }

    @Override
    public boolean getRequestResponseInformation() {
        return requestResponseInformation;
    }

    @Override
    public @NotNull Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    @Override
    public @NotNull Optional<ByteBuffer> getPassword() {
        return (password == null) ? Optional.empty() : Optional.of(password.asReadOnlyBuffer());
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
    public @NotNull Optional<WillPublishPacket> getWillPublish() {
        return Optional.ofNullable(willPublish);
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
        if (!(o instanceof ConnectPacketImpl)) {
            return false;
        }
        final ConnectPacketImpl that = (ConnectPacketImpl) o;
        return (mqttVersion == that.mqttVersion) &&
                clientId.equals(that.clientId) &&
                (cleanStart == that.cleanStart) &&
                (sessionExpiryInterval == that.sessionExpiryInterval) &&
                (keepAlive == that.keepAlive) &&
                (receiveMaximum == that.receiveMaximum) &&
                (maximumPacketSize == that.maximumPacketSize) &&
                (topicAliasMaximum == that.topicAliasMaximum) &&
                (requestProblemInformation == that.requestProblemInformation) &&
                (requestResponseInformation == that.requestResponseInformation) &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(password, that.password) &&
                Objects.equals(authenticationMethod, that.authenticationMethod) &&
                Objects.equals(authenticationData, that.authenticationData) &&
                Objects.equals(willPublish, that.willPublish) &&
                userProperties.equals(that.userProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mqttVersion, clientId, cleanStart, sessionExpiryInterval, keepAlive, receiveMaximum,
                maximumPacketSize, topicAliasMaximum, requestProblemInformation, requestResponseInformation, userName,
                password, authenticationMethod, authenticationData, willPublish, userProperties);
    }
}
