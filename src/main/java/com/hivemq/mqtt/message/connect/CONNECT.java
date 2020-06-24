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
package com.hivemq.mqtt.message.connect;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.packets.connect.ConnectPacketImpl;
import com.hivemq.extensions.packets.general.MqttVersionUtil;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.util.Bytes;

import java.nio.charset.StandardCharsets;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class CONNECT extends MqttMessageWithUserProperties implements Mqtt5CONNECT, Mqtt3CONNECT {

    private final @NotNull ProtocolVersion protocolVersion;
    private final @NotNull String clientIdentifier;
    private final int keepAlive;
    private final boolean cleanStart;
    private long sessionExpiryInterval;

    // restrictions
    private int receiveMaximum;
    private int topicAliasMaximum;
    private long maximumPacketSize;
    private final boolean responseInformationRequested;
    private final boolean problemInformationRequested;

    // simple auth
    private final @Nullable String username;
    private final byte @Nullable [] password;

    // enhanced auth
    private final @Nullable String authMethod;
    private final byte @Nullable [] authData;

    private final @Nullable MqttWillPublish willPublish;

    private CONNECT(
            final @NotNull ProtocolVersion protocolVersion,
            final @NotNull String clientIdentifier,
            final int keepAlive,
            final boolean cleanStart,
            final long sessionExpiryInterval,
            final int receiveMaximum,
            final int topicAliasMaximum,
            final long maximumPacketSize,
            final boolean responseInformationRequested,
            final boolean problemInformationRequested,
            final @Nullable String username,
            final byte @Nullable [] password,
            final @Nullable String authMethod,
            final byte @Nullable [] authData,
            final @Nullable MqttWillPublish willPublish,
            final @NotNull Mqtt5UserProperties userProperties) {

        super(userProperties);
        this.protocolVersion = protocolVersion;
        this.clientIdentifier = clientIdentifier;
        this.keepAlive = keepAlive;
        this.cleanStart = cleanStart;
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.receiveMaximum = receiveMaximum;
        this.topicAliasMaximum = topicAliasMaximum;
        this.maximumPacketSize = maximumPacketSize;
        this.responseInformationRequested = responseInformationRequested;
        this.problemInformationRequested = problemInformationRequested;
        this.username = username;
        this.password = password;
        this.authMethod = authMethod;
        this.authData = authData;
        this.willPublish = willPublish;
    }

    @Override
    public @NotNull ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public @NotNull String getClientIdentifier() {
        return clientIdentifier;
    }

    @Override
    public int getKeepAlive() {
        return keepAlive;
    }

    @Override
    public boolean isCleanStart() {
        return cleanStart;
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(final long sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    @Override
    public int getReceiveMaximum() {
        return receiveMaximum;
    }

    public void setReceiveMaximum(final int receiveMaximum) {
        this.receiveMaximum = receiveMaximum;
    }

    @Override
    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    public void setTopicAliasMaximum(final int topicAliasMaximum) {
        this.topicAliasMaximum = topicAliasMaximum;
    }

    @Override
    public long getMaximumPacketSize() {
        return maximumPacketSize;
    }

    public void setMaximumPacketSize(final long maximumPacketSize) {
        this.maximumPacketSize = maximumPacketSize;
    }

    @Override
    public boolean isResponseInformationRequested() {
        return responseInformationRequested;
    }

    @Override
    public boolean isProblemInformationRequested() {
        return problemInformationRequested;
    }

    @Override
    public @Nullable String getUsername() {
        return username;
    }

    @Override
    public byte @Nullable [] getPassword() {
        return password;
    }

    @Override
    public @Nullable String getPasswordAsUTF8String() {
        return password != null ? new String(password, StandardCharsets.UTF_8) : null;
    }

    @Override
    public @Nullable String getAuthMethod() {
        return authMethod;
    }

    @Override
    public byte @Nullable [] getAuthData() {
        return authData;
    }

    @Override
    public @Nullable MqttWillPublish getWillPublish() {
        return willPublish;
    }

    @Override
    public @NotNull MessageType getType() {
        return MessageType.CONNECT;
    }

    public static class Mqtt3Builder {

        private @NotNull ProtocolVersion protocolVersion = ProtocolVersion.MQTTv3_1_1;
        private @Nullable String clientIdentifier;
        private int keepAlive;
        private boolean cleanStart;
        private long sessionExpiryInterval;

        private @Nullable String username;
        private byte @Nullable [] password;

        private @Nullable MqttWillPublish willPublish;

        public @NotNull CONNECT build() {
            Preconditions.checkNotNull(clientIdentifier, "client identifier must never be null");
            return new CONNECT(
                    protocolVersion,
                    clientIdentifier,
                    keepAlive,
                    cleanStart,
                    sessionExpiryInterval,
                    DEFAULT_RECEIVE_MAXIMUM,
                    DEFAULT_TOPIC_ALIAS_MAXIMUM,
                    DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT,
                    DEFAULT_RESPONSE_INFORMATION_REQUESTED,
                    DEFAULT_PROBLEM_INFORMATION_REQUESTED,
                    username,
                    password,
                    null,
                    null,
                    willPublish,
                    Mqtt5UserProperties.NO_USER_PROPERTIES);
        }

        public @NotNull Mqtt3Builder withProtocolVersion(final @NotNull ProtocolVersion protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public @NotNull Mqtt3Builder withClientIdentifier(final @NotNull String clientIdentifier) {
            this.clientIdentifier = clientIdentifier;
            return this;
        }

        public @NotNull Mqtt3Builder withKeepAlive(final int keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public @NotNull Mqtt3Builder withCleanStart(final boolean cleanStart) {
            this.cleanStart = cleanStart;
            return this;
        }

        public @NotNull Mqtt3Builder withSessionExpiryInterval(final long sessionExpiryInterval) {
            this.sessionExpiryInterval = sessionExpiryInterval;
            return this;
        }

        public @NotNull Mqtt3Builder withUsername(final @Nullable String username) {
            this.username = username;
            return this;
        }

        public @NotNull Mqtt3Builder withPassword(final byte @Nullable [] password) {
            this.password = password;
            return this;
        }

        public @NotNull Mqtt3Builder withWillPublish(final @Nullable MqttWillPublish willPublish) {
            this.willPublish = willPublish;
            return this;
        }
    }

    public static class Mqtt5Builder {

        private @Nullable String clientIdentifier;
        private int keepAlive;
        private boolean cleanStart;
        private long sessionExpiryInterval;

        // restrictions
        private int receiveMaximum;
        private int topicAliasMaximum;
        private long maximumPacketSize;
        private boolean responseInformationRequested = DEFAULT_RESPONSE_INFORMATION_REQUESTED;
        private boolean problemInformationRequested = DEFAULT_PROBLEM_INFORMATION_REQUESTED;

        // simple auth
        private @Nullable String username;
        private byte @Nullable [] password;

        // enhanced auth
        private @Nullable String authMethod;
        private byte @Nullable [] authData;

        private @Nullable MqttWillPublish willPublish;

        private @NotNull Mqtt5UserProperties userProperties = Mqtt5UserProperties.NO_USER_PROPERTIES;

        public @NotNull CONNECT build() {
            Preconditions.checkNotNull(clientIdentifier, "client identifier must never be null");
            return new CONNECT(
                    ProtocolVersion.MQTTv5,
                    clientIdentifier,
                    keepAlive,
                    cleanStart,
                    sessionExpiryInterval,
                    receiveMaximum,
                    topicAliasMaximum,
                    maximumPacketSize,
                    responseInformationRequested,
                    problemInformationRequested,
                    username,
                    password,
                    authMethod,
                    authData,
                    willPublish,
                    userProperties);
        }

        public @NotNull Mqtt5Builder withClientIdentifier(final @NotNull String clientIdentifier) {
            this.clientIdentifier = clientIdentifier;
            return this;
        }

        public @NotNull Mqtt5Builder withKeepAlive(final int keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public @NotNull Mqtt5Builder withCleanStart(final boolean cleanStart) {
            this.cleanStart = cleanStart;
            return this;
        }

        public @NotNull Mqtt5Builder withSessionExpiryInterval(final long sessionExpiryInterval) {
            this.sessionExpiryInterval = sessionExpiryInterval;
            return this;
        }

        public @NotNull Mqtt5Builder withReceiveMaximum(final int receiveMaximum) {
            this.receiveMaximum = receiveMaximum;
            return this;
        }

        public @NotNull Mqtt5Builder withTopicAliasMaximum(final int topicAliasMaximum) {
            this.topicAliasMaximum = topicAliasMaximum;
            return this;
        }

        public @NotNull Mqtt5Builder withMaximumPacketSize(final long maximumPacketSize) {
            this.maximumPacketSize = maximumPacketSize;
            return this;
        }

        public @NotNull Mqtt5Builder withResponseInformationRequested(final boolean responseInformationRequested) {
            this.responseInformationRequested = responseInformationRequested;
            return this;
        }

        public @NotNull Mqtt5Builder withProblemInformationRequested(final boolean problemInformationRequested) {
            this.problemInformationRequested = problemInformationRequested;
            return this;
        }

        public @NotNull Mqtt5Builder withUsername(final @Nullable String username) {
            this.username = username;
            return this;
        }

        public @NotNull Mqtt5Builder withPassword(final byte @Nullable [] password) {
            this.password = password;
            return this;
        }

        public @NotNull Mqtt5Builder withAuthMethod(final @Nullable String authMethod) {
            this.authMethod = authMethod;
            return this;
        }

        public @NotNull Mqtt5Builder withAuthData(final byte @Nullable [] authData) {
            this.authData = authData;
            return this;
        }

        public @NotNull Mqtt5Builder withWillPublish(final @Nullable MqttWillPublish willPublish) {
            this.willPublish = willPublish;
            return this;
        }

        public @NotNull Mqtt5Builder withUserProperties(final @NotNull Mqtt5UserProperties userProperties) {
            this.userProperties = userProperties;
            return this;
        }
    }

    public static @NotNull CONNECT from(final @NotNull ConnectPacketImpl packet, final @NotNull String clusterId) {
        return new CONNECT(
                MqttVersionUtil.toProtocolVersion(packet.getMqttVersion()),
                packet.getClientId(),
                packet.getKeepAlive(),
                packet.getCleanStart(),
                packet.getSessionExpiryInterval(),
                packet.getReceiveMaximum(),
                packet.getTopicAliasMaximum(),
                packet.getMaximumPacketSize(),
                packet.getRequestResponseInformation(),
                packet.getRequestProblemInformation(),
                packet.getUserName().orElse(null),
                Bytes.getBytesFromReadOnlyBuffer(packet.getPassword()),
                packet.getAuthenticationMethod().orElse(null),
                Bytes.getBytesFromReadOnlyBuffer(packet.getAuthenticationData()),
                MqttWillPublish.fromWillPacket(clusterId, packet.getWillPublish().orElse(null)),
                Mqtt5UserProperties.of(packet.getUserProperties().asInternalList()));
    }
}
