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

package com.hivemq.mqtt.message.connect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extensions.packets.connect.ModifiableConnectPacketImpl;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.util.Bytes;

import java.nio.charset.StandardCharsets;

/**
 * @author Florian Limpöck
 */
public class CONNECT extends MqttMessageWithUserProperties implements Mqtt5CONNECT, Mqtt3CONNECT {

    private final String clientIdentifier;
    private final int keepAlive;
    private final boolean isCleanStart;
    private long sessionExpiryInterval;
    private boolean isResponseInformationRequested;
    private boolean isProblemInformationRequested;

    //Restrictions
    private int receiveMaximum;
    private int topicAliasMaximum;
    private long maximumPacketSize;

    //Simple Auth
    private final String username;
    private final byte[] password;

    //Enhanced Auth
    private final String authMethod;
    private final byte[] data;

    //Mqtt5 Will
    private final boolean will;
    private final MqttWillPublish willPublish;


    private final boolean passwordRequired;
    private final boolean usernameRequired;

    private final ProtocolVersion protocolVersion;

    //MQTT 5 CONNECT
    private CONNECT(
            final int keepAlive, final boolean isCleanStart, final long sessionExpiryInterval,
            final boolean isResponseInformationRequested, final boolean isProblemInformationRequested,
            final int receiveMaximum, final int topicAliasMaximum, final long maximumPacketSize,
            final boolean usernameRequired, final boolean passwordRequired,
            @Nullable final String username, @Nullable final byte[] password,
            @Nullable final String authMethod, final byte[] data,
            final boolean will, @Nullable final MqttWillPublish willPublish,
            @NotNull final Mqtt5UserProperties userProperties, @NotNull final String clientIdentifier) {

        super(userProperties);

        Preconditions.checkNotNull(clientIdentifier, "A client identifier may never be null");

        this.protocolVersion = ProtocolVersion.MQTTv5;
        this.keepAlive = keepAlive;
        this.isCleanStart = isCleanStart;
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.isResponseInformationRequested = isResponseInformationRequested;
        this.isProblemInformationRequested = isProblemInformationRequested;
        this.receiveMaximum = receiveMaximum;
        this.topicAliasMaximum = topicAliasMaximum;
        this.maximumPacketSize = maximumPacketSize;
        this.passwordRequired = passwordRequired;
        this.usernameRequired = usernameRequired;
        this.username = username;
        this.password = password;
        this.authMethod = authMethod;
        this.data = data;
        this.will = will;
        this.willPublish = willPublish;
        this.clientIdentifier = clientIdentifier;

    }

    //MQTT 3 CONNECT
    private CONNECT(@NotNull final ProtocolVersion protocolVersion,
                    @NotNull final String clientIdentifier,
                    final String username, final byte[] password,
                    final int keepAlive,
                    final boolean passwordRequired, final boolean usernameRequired,
                    final boolean will, final MqttWillPublish willPublish,
                    final boolean isCleanStart, final long sessionExpiryInterval) {

        super(Mqtt5UserProperties.NO_USER_PROPERTIES);

        this.protocolVersion = protocolVersion;
        this.clientIdentifier = clientIdentifier;
        this.username = username;
        this.password = password;
        this.keepAlive = keepAlive;
        this.passwordRequired = passwordRequired;
        this.usernameRequired = usernameRequired;
        this.will = will;
        this.willPublish = willPublish;
        this.isCleanStart = isCleanStart;
        this.sessionExpiryInterval = sessionExpiryInterval;

        //MQTT5
        this.isResponseInformationRequested = DEFAULT_RESPONSE_INFORMATION_REQUESTED;
        this.isProblemInformationRequested = DEFAULT_PROBLEM_INFORMATION_REQUESTED;
        this.receiveMaximum = DEFAULT_RECEIVE_MAXIMUM;
        this.topicAliasMaximum = DEFAULT_TOPIC_ALIAS_MAXIMUM;
        this.maximumPacketSize = DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
        this.authMethod = null;
        this.data = null;

    }

    @Override
    @Nullable
    public String getAuthMethod() {
        return authMethod;
    }

    @Override
    @Nullable
    public byte[] getAuthData() {
        return data;
    }

    @Override
    public int getReceiveMaximum() {
        return receiveMaximum;
    }

    public void setResponseInformationRequested(final Boolean responseInformationRequested) {
        isResponseInformationRequested = responseInformationRequested;
    }

    public void setProblemInformationRequested(final Boolean problemInformationRequested) {
        isProblemInformationRequested = problemInformationRequested;
    }

    public void setReceiveMaximum(final int receiveMaximum) {
        this.receiveMaximum = receiveMaximum;
    }

    public void setTopicAliasMaximum(final int topicAliasMaximum) {
        this.topicAliasMaximum = topicAliasMaximum;
    }

    public void setMaximumPacketSize(final long maximumPacketSize) {
        this.maximumPacketSize = maximumPacketSize;
    }

    @Override
    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    @Override
    public long getMaximumPacketSize() {
        return maximumPacketSize;
    }

    @Override
    public int getKeepAlive() {
        return keepAlive;
    }

    @Override
    public boolean isCleanStart() {
        return isCleanStart;
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(final long sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    @Override
    public boolean isResponseInformationRequested() {
        return isResponseInformationRequested;
    }

    @Override
    public boolean isProblemInformationRequested() {
        return isProblemInformationRequested;
    }

    @Override
    public MqttWillPublish getWillPublish() {
        return willPublish;
    }

    @NotNull
    @Override
    public String getClientIdentifier() {
        return clientIdentifier;
    }

    @Override
    public boolean isWill() {
        return will;
    }

    @Override
    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    @Override
    public boolean isUsernameRequired() {
        return usernameRequired;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public byte[] getPassword() {
        return password;
    }

    @Override
    public String getPasswordAsUTF8String() {
        return password != null ? new String(password, StandardCharsets.UTF_8) : null;
    }

    @Override
    @NotNull
    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    @NotNull
    @Override
    public MessageType getType() {
        return MessageType.CONNECT;
    }

    public static class Mqtt3Builder {

        private boolean will;

        private boolean passwordRequired;
        private boolean usernameRequired;
        private int keepAliveTimer;
        private String clientIdentifier;

        private String username;
        private byte[] password;

        //Needed Mqtt 5 Values
        private boolean cleanStart;
        private long sessionExpiryInterval;


        private ProtocolVersion protocolVersion = ProtocolVersion.MQTTv3_1_1;

        private MqttWillPublish willPublish;

        public CONNECT build() {
            return new CONNECT(protocolVersion,
                    clientIdentifier,
                    username,
                    password,
                    keepAliveTimer,
                    passwordRequired,
                    usernameRequired,
                    will,
                    willPublish,
                    cleanStart,
                    sessionExpiryInterval);
        }

        public Mqtt3Builder withWill(final boolean will) {
            this.will = will;
            return this;
        }

        public Mqtt3Builder withPasswordRequired(final boolean passwordRequired) {
            this.passwordRequired = passwordRequired;
            return this;
        }

        public Mqtt3Builder withUsernameRequired(final boolean usernameRequired) {
            this.usernameRequired = usernameRequired;
            return this;
        }

        public Mqtt3Builder withKeepAliveTimer(final int keepAliveTimer) {
            this.keepAliveTimer = keepAliveTimer;
            return this;
        }

        public Mqtt3Builder withClientIdentifier(final String clientIdentifier) {
            this.clientIdentifier = clientIdentifier;
            return this;
        }

        public Mqtt3Builder withUsername(final String username) {
            this.username = username;
            return this;
        }

        public Mqtt3Builder withPassword(final byte[] password) {
            this.password = password;
            return this;
        }

        public Mqtt3Builder withProtocolVersion(final ProtocolVersion protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Mqtt3Builder withWillPublish(final MqttWillPublish willPublish) {
            this.willPublish = willPublish;
            return this;
        }

        public Mqtt3Builder withCleanStart(final boolean cleanStart) {
            this.cleanStart = cleanStart;
            return this;
        }

        public Mqtt3Builder withSessionExpiryInterval(final long sessionExpiryInterval) {
            this.sessionExpiryInterval = sessionExpiryInterval;
            return this;
        }
    }

    public static class Mqtt5Builder {

        private Mqtt5UserProperties mqtt5UserProperties = Mqtt5UserProperties.NO_USER_PROPERTIES;

        private String clientIdentifier;
        private int keepAlive;
        private boolean isCleanStart;
        private long sessionExpiryInterval;
        private boolean isResponseInformationRequested = DEFAULT_RESPONSE_INFORMATION_REQUESTED;
        private boolean isProblemInformationRequested = DEFAULT_PROBLEM_INFORMATION_REQUESTED;

        //Restrictions
        private int receiveMaximum;
        private int topicAliasMaximum;
        private long maximumPacketSize;

        //Simple Auth
        private String username;
        private byte[] password;

        //Enhanced Auth
        private String authMethod;
        private byte[] authData;

        //Mqtt5 Will
        private boolean will;
        private MqttWillPublish willPublish;

        private boolean passwordRequired;
        private boolean usernameRequired;


        public CONNECT build() {
            return new CONNECT(keepAlive,
                    isCleanStart,
                    sessionExpiryInterval,
                    isResponseInformationRequested,
                    isProblemInformationRequested,
                    receiveMaximum,
                    topicAliasMaximum,
                    maximumPacketSize,
                    usernameRequired,
                    passwordRequired,
                    username,
                    password,
                    authMethod,
                    authData,
                    will,
                    willPublish,
                    mqtt5UserProperties,
                    clientIdentifier);
        }

        public Mqtt5Builder withMqtt5UserProperties(final Mqtt5UserProperties mqtt5UserProperties) {
            this.mqtt5UserProperties = mqtt5UserProperties;
            return this;
        }

        public Mqtt5Builder withClientIdentifier(final String clientIdentifier) {
            this.clientIdentifier = clientIdentifier;
            return this;
        }

        public Mqtt5Builder withKeepAlive(final int keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public Mqtt5Builder withCleanStart(final boolean cleanStart) {
            isCleanStart = cleanStart;
            return this;
        }

        public Mqtt5Builder withSessionExpiryInterval(final long sessionExpiryInterval) {
            this.sessionExpiryInterval = sessionExpiryInterval;
            return this;
        }

        public Mqtt5Builder withResponseInformationRequested(final boolean responseInformationRequested) {
            isResponseInformationRequested = responseInformationRequested;
            return this;
        }

        public Mqtt5Builder withProblemInformationRequested(final boolean problemInformationRequested) {
            isProblemInformationRequested = problemInformationRequested;
            return this;
        }

        public Mqtt5Builder withReceiveMaximum(final int receiveMaximum) {
            this.receiveMaximum = receiveMaximum;
            return this;
        }

        public Mqtt5Builder withTopicAliasMaximum(final int topicAliasMaximum) {
            this.topicAliasMaximum = topicAliasMaximum;
            return this;
        }

        public Mqtt5Builder withMaximumPacketSize(final long maximumPacketSize) {
            this.maximumPacketSize = maximumPacketSize;
            return this;
        }

        public Mqtt5Builder withUsername(final String username) {
            this.username = username;
            return this;
        }

        public Mqtt5Builder withPassword(final byte[] password) {
            this.password = password;
            return this;
        }

        public Mqtt5Builder withAuthMethod(final String authMethod) {
            this.authMethod = authMethod;
            return this;
        }

        public Mqtt5Builder withAuthData(final byte[] authData) {
            this.authData = authData;
            return this;
        }

        public Mqtt5Builder withWill(final boolean will) {
            this.will = will;
            return this;
        }

        public Mqtt5Builder withWillPublish(final MqttWillPublish willPublish) {
            this.willPublish = willPublish;
            return this;
        }

        public Mqtt5Builder withPasswordRequired(final boolean passwordRequired) {
            this.passwordRequired = passwordRequired;
            return this;
        }

        public Mqtt5Builder withUsernameRequired(final boolean usernameRequired) {
            this.usernameRequired = usernameRequired;
            return this;
        }
    }

    public static @NotNull CONNECT mergeConnectPacket(final @NotNull ModifiableConnectPacketImpl connectPacket, final @NotNull CONNECT origin,
                                                      @NotNull final String clusterId) {

        if (!connectPacket.isModified()) {
            return origin;
        }

        final CONNECT.Mqtt5Builder builder = new CONNECT.Mqtt5Builder();

        final ImmutableList.Builder<MqttUserProperty> userProperties = new ImmutableList.Builder<>();
        for (final UserProperty userProperty : connectPacket.getUserProperties().asList()) {
            userProperties.add(new MqttUserProperty(userProperty.getName(), userProperty.getValue()));
        }

        return builder.withClientIdentifier(connectPacket.getClientId())
                .withCleanStart(connectPacket.getCleanStart())
                .withWillPublish(MqttWillPublish.fromWillPacket(clusterId, connectPacket.getWillPublish().orElse(null)))
                .withSessionExpiryInterval(connectPacket.getSessionExpiryInterval())
                .withKeepAlive(connectPacket.getKeepAlive())
                .withReceiveMaximum(connectPacket.getReceiveMaximum())
                .withMaximumPacketSize(connectPacket.getMaximumPacketSize())
                .withTopicAliasMaximum(connectPacket.getTopicAliasMaximum())
                .withResponseInformationRequested(connectPacket.getRequestResponseInformation())
                .withProblemInformationRequested(connectPacket.getRequestProblemInformation())
                .withAuthMethod(connectPacket.getAuthenticationMethod().orElse(null))
                .withAuthData(Bytes.getBytesFromReadOnlyBuffer(connectPacket.getAuthenticationData()))
                .withMqtt5UserProperties(Mqtt5UserProperties.of(userProperties.build()))
                .withUsername(connectPacket.getUserName().orElse(null))
                .withPassword(Bytes.getBytesFromReadOnlyBuffer(connectPacket.getPassword()))
                .withWill(connectPacket.getWillPublish().isPresent())
                .withUsernameRequired(connectPacket.getUserName().isPresent())
                .withPasswordRequired(connectPacket.getPassword().isPresent())
                .build();
    }

}
