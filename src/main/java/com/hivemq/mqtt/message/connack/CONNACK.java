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
package com.hivemq.mqtt.message.connack;

import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extensions.packets.connack.ConnackPacketImpl;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties.MqttMessageWithReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.Bytes;

import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.KEEP_ALIVE_NOT_SET;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;

/**
 * The MQTT CONNACK message
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 * @since 4.0.0
 */
@Immutable
public class CONNACK extends MqttMessageWithReasonCode<Mqtt5ConnAckReasonCode> implements Mqtt3CONNACK, Mqtt5CONNACK {

    private final boolean sessionPresent;

    //Mqtt 5
    private final long sessionExpiryInterval;
    private final int serverKeepAlive;
    private final @Nullable String assignedClientIdentifier;

    //Auth
    private final @Nullable String authMethod;
    private final @Nullable byte[] authData;

    //Restrictions from Server
    private final int receiveMaximum;
    private final int topicAliasMaximum;
    private final int maximumPacketSize;
    private final @Nullable QoS maximumQoS;
    private final boolean isRetainAvailable;
    private final boolean isWildcardSubscriptionAvailable;
    private final boolean isSubscriptionIdentifierAvailable;
    private final boolean isSharedSubscriptionAvailable;

    private final @Nullable String responseInformation;
    private final @Nullable String serverReference;

    // MQTT 5 CONNACK for FAILED CONNECT
    public CONNACK(@NotNull final Mqtt5ConnAckReasonCode reasonCode, @Nullable final String reasonString) {
        super(reasonCode, reasonString, Mqtt5UserProperties.NO_USER_PROPERTIES);

        this.sessionPresent = false;
        this.sessionExpiryInterval = SESSION_EXPIRY_NOT_SET;
        this.serverKeepAlive = KEEP_ALIVE_NOT_SET;
        this.assignedClientIdentifier = null;
        this.authMethod = null;
        this.authData = null;
        this.receiveMaximum = DEFAULT_RECEIVE_MAXIMUM;
        this.topicAliasMaximum = DEFAULT_TOPIC_ALIAS_MAXIMUM;
        this.maximumPacketSize = DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
        this.maximumQoS = null;
        this.isRetainAvailable = DEFAULT_RETAIN_AVAILABLE;
        this.isWildcardSubscriptionAvailable = DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE;
        this.isSubscriptionIdentifierAvailable = DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE;
        this.isSharedSubscriptionAvailable = DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE;
        this.responseInformation = null;
        this.serverReference = null;
    }

    // MQTT 5
    private CONNACK(@NotNull final Mqtt5ConnAckReasonCode reasonCode,
                    @Nullable final String reasonString,
                    @NotNull final Mqtt5UserProperties userProperties,
                    final boolean sessionPresent,
                    final long sessionExpiryInterval,
                    final int serverKeepAlive,
                    @Nullable final String assignedClientIdentifier,
                    @Nullable final String authMethod,
                    @Nullable final byte[] authData,
                    final int receiveMaximum,
                    final int topicAliasMaximum,
                    final int maximumPacketSize,
                    @Nullable final QoS maximumQoS,
                    final boolean isRetainAvailable,
                    final boolean isWildcardSubscriptionAvailable,
                    final boolean isSubscriptionIdentifierAvailable,
                    final boolean isSharedSubscriptionAvailable,
                    @Nullable final String responseInformation,
                    @Nullable final String serverReference) {

        super(reasonCode, reasonString, userProperties);

        checkPreconditions(sessionExpiryInterval, serverKeepAlive, assignedClientIdentifier,
                authMethod, authData, receiveMaximum, topicAliasMaximum, maximumPacketSize, responseInformation, serverReference);

        this.sessionPresent = sessionPresent;
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.serverKeepAlive = serverKeepAlive;
        this.assignedClientIdentifier = assignedClientIdentifier;
        this.authMethod = authMethod;
        this.authData = authData;
        this.receiveMaximum = receiveMaximum;
        this.topicAliasMaximum = topicAliasMaximum;
        this.maximumPacketSize = maximumPacketSize;
        this.maximumQoS = maximumQoS;
        this.isRetainAvailable = isRetainAvailable;
        this.isWildcardSubscriptionAvailable = isWildcardSubscriptionAvailable;
        this.isSubscriptionIdentifierAvailable = isSubscriptionIdentifierAvailable;
        this.isSharedSubscriptionAvailable = isSharedSubscriptionAvailable;
        this.responseInformation = responseInformation;
        this.serverReference = serverReference;
    }

    public static @NotNull CONNACK from(final @NotNull ConnackPacketImpl packet) {

        final Qos extensionMaxQos = packet.getMaximumQoS().orElse(null);
        final QoS qoS = (extensionMaxQos != null) ? QoS.valueOf(extensionMaxQos.getQosNumber()) : null;

        return new CONNACK.Mqtt5Builder()
                .withReasonCode(Mqtt5ConnAckReasonCode.from(packet.getReasonCode()))
                .withSessionPresent(packet.getSessionPresent())
                .withSessionExpiryInterval(packet.getSessionExpiryInterval().orElse(SESSION_EXPIRY_NOT_SET))
                .withServerKeepAlive(packet.getServerKeepAlive().orElse(KEEP_ALIVE_NOT_SET))
                .withAssignedClientIdentifier(packet.getAssignedClientIdentifier().orElse(null))
                .withAuthMethod(packet.getAuthenticationMethod().orElse(null))
                .withAuthData(Bytes.getBytesFromReadOnlyBuffer(packet.getAuthenticationData()))
                .withReceiveMaximum(packet.getReceiveMaximum())
                .withMaximumPacketSize(packet.getMaximumPacketSize())
                .withTopicAliasMaximum(packet.getTopicAliasMaximum())
                .withMaximumQoS(qoS)
                .withRetainAvailable(packet.getRetainAvailable())
                .withWildcardSubscriptionAvailable(packet.getWildCardSubscriptionAvailable())
                .withSharedSubscriptionAvailable(packet.getSharedSubscriptionsAvailable())
                .withSubscriptionIdentifierAvailable(packet.getSubscriptionIdentifiersAvailable())
                .withResponseInformation(packet.getResponseInformation().orElse(null))
                .withServerReference(packet.getServerReference().orElse(null))
                .withReasonString(packet.getReasonString().orElse(null))
                .withUserProperties(Mqtt5UserProperties.of(packet.getUserProperties().asInternalList()))
                .build();
    }

    //MQTT 3.1
    public CONNACK(@NotNull final Mqtt3ConnAckReturnCode returnCode) {
        this(returnCode, false);
    }

    //MQTT 3.1.1
    public CONNACK(@NotNull final Mqtt3ConnAckReturnCode returnCode, final boolean sessionPresent) {

        super(Mqtt5ConnAckReasonCode.fromReturnCode(returnCode), null, Mqtt5UserProperties.NO_USER_PROPERTIES);

        if (returnCode != Mqtt3ConnAckReturnCode.ACCEPTED && sessionPresent) {
            throw new IllegalArgumentException("The sessionPresent flag is only allowed for return code " + Mqtt3ConnAckReturnCode.ACCEPTED);
        }

        this.sessionPresent = sessionPresent;


        //MQTT 5 only
        this.sessionExpiryInterval = SESSION_EXPIRY_NOT_SET;
        this.serverKeepAlive = KEEP_ALIVE_NOT_SET;
        this.assignedClientIdentifier = null;
        this.authMethod = null;
        this.authData = null;
        this.receiveMaximum = DEFAULT_RECEIVE_MAXIMUM;
        this.topicAliasMaximum = DEFAULT_TOPIC_ALIAS_MAXIMUM;
        this.maximumPacketSize = DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
        this.maximumQoS = null;
        this.isRetainAvailable = DEFAULT_RETAIN_AVAILABLE;
        this.isWildcardSubscriptionAvailable = DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE;
        this.isSubscriptionIdentifierAvailable = DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE;
        this.isSharedSubscriptionAvailable = DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE;
        this.responseInformation = null;
        this.serverReference = null;
    }

    private void checkPreconditions(final long sessionExpiryInterval,
                                    final int serverKeepAlive,
                                    @Nullable final String assignedClientIdentifier,
                                    @Nullable final String authMethod,
                                    @Nullable final byte[] authData,
                                    final int receiveMaximum,
                                    final int topicAliasMaximum,
                                    final int maximumPacketSize,
                                    @Nullable final String responseInformation,
                                    @Nullable final String serverReference) {

        checkArgument(receiveMaximum != 0, "Receive maximum must never be zero");

        if (assignedClientIdentifier != null) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(assignedClientIdentifier.getBytes(StandardCharsets.UTF_8).length),
                    "A client Id must never exceed 65.535 bytes");
        }
        if (authMethod != null) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(authMethod.getBytes(StandardCharsets.UTF_8).length),
                    "An auth method must never exceed 65.535 bytes");
        }
        if (authData != null) {
            checkNotNull(authMethod, "Auth method must be set if auth data is set");
            checkArgument(UnsignedDataTypes.isUnsignedShort(authData.length),
                    "An auth data must never exceed 65.535 bytes");
        }
        if (responseInformation != null) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(responseInformation.getBytes(StandardCharsets.UTF_8).length),
                    "A response information must never exceed 65.535 bytes");
        }
        if (serverReference != null) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(serverReference.getBytes(StandardCharsets.UTF_8).length),
                    "A server reference must never exceed 65.535 bytes");
        }
        if (sessionExpiryInterval != SESSION_EXPIRY_NOT_SET) {
            checkArgument(UnsignedDataTypes.isUnsignedInt(sessionExpiryInterval),
                    "A session expiry interval must never be larger than 4.294.967.296");
        }

        checkArgument(maximumPacketSize <= CONNECT.DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT,
                "A maximum packet size must never be larger than 268.435.460");

        checkArgument(UnsignedDataTypes.isUnsignedShort(topicAliasMaximum),
                "A topic alias maximum must never be larger than 65.535");

        if (serverKeepAlive != CONNECT.KEEP_ALIVE_NOT_SET) {
            checkArgument(UnsignedDataTypes.isUnsignedShort(serverKeepAlive),
                    "A server keep alive must never be larger than 65.535");
        }
    }

    public static class Mqtt5Builder {

        private Mqtt5ConnAckReasonCode reasonCode;
        private String reasonString;
        private Mqtt5UserProperties userProperties = Mqtt5UserProperties.NO_USER_PROPERTIES;

        private boolean sessionPresent;

        //Mqtt 5
        private long sessionExpiryInterval = SESSION_EXPIRY_NOT_SET;
        private int serverKeepAlive = KEEP_ALIVE_NOT_SET;
        private String assignedClientIdentifier;

        //Auth
        private String authMethod;
        private byte[] authData;

        //Restrictions from Server
        private int receiveMaximum = DEFAULT_RECEIVE_MAXIMUM;
        private int topicAliasMaximum = DEFAULT_TOPIC_ALIAS_MAXIMUM;
        private int maximumPacketSize = DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
        private QoS maximumQoS;
        private boolean isRetainAvailable = DEFAULT_RETAIN_AVAILABLE;
        private boolean isWildcardSubscriptionAvailable = DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE;
        private boolean isSubscriptionIdentifierAvailable = DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE;
        private boolean isSharedSubscriptionAvailable = DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE;

        private String responseInformation;
        private String serverReference;

        public CONNACK build() {
            return new CONNACK(reasonCode,
                    reasonString,
                    userProperties,
                    sessionPresent,
                    sessionExpiryInterval,
                    serverKeepAlive,
                    assignedClientIdentifier,
                    authMethod,
                    authData,
                    receiveMaximum,
                    topicAliasMaximum,
                    maximumPacketSize,
                    maximumQoS,
                    isRetainAvailable,
                    isWildcardSubscriptionAvailable,
                    isSubscriptionIdentifierAvailable,
                    isSharedSubscriptionAvailable,
                    responseInformation,
                    serverReference);
        }

        public Mqtt5Builder withReasonCode(final Mqtt5ConnAckReasonCode reasonCode) {
            this.reasonCode = reasonCode;
            return this;
        }

        public Mqtt5Builder withReasonString(final String reasonString) {
            this.reasonString = reasonString;
            return this;
        }

        public Mqtt5Builder withUserProperties(final Mqtt5UserProperties userProperties) {
            this.userProperties = userProperties;
            return this;
        }

        public Mqtt5Builder withSessionPresent(final boolean sessionPresent) {
            this.sessionPresent = sessionPresent;
            return this;
        }

        public Mqtt5Builder withSessionExpiryInterval(final long sessionExpiryInterval) {
            this.sessionExpiryInterval = sessionExpiryInterval;
            return this;
        }

        public Mqtt5Builder withServerKeepAlive(final int serverKeepAlive) {
            this.serverKeepAlive = serverKeepAlive;
            return this;
        }

        public Mqtt5Builder withAssignedClientIdentifier(final String assignedClientIdentifier) {
            this.assignedClientIdentifier = assignedClientIdentifier;
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

        public Mqtt5Builder withReceiveMaximum(final int receiveMaximum) {
            this.receiveMaximum = receiveMaximum;
            return this;
        }

        public Mqtt5Builder withTopicAliasMaximum(final int topicAliasMaximum) {
            this.topicAliasMaximum = topicAliasMaximum;
            return this;
        }

        public Mqtt5Builder withMaximumPacketSize(final int maximumPacketSize) {
            this.maximumPacketSize = maximumPacketSize;
            return this;
        }

        public Mqtt5Builder withMaximumQoS(final QoS maximumQoS) {
            this.maximumQoS = maximumQoS;
            return this;
        }

        public Mqtt5Builder withRetainAvailable(final boolean retainAvailable) {
            isRetainAvailable = retainAvailable;
            return this;
        }

        public Mqtt5Builder withWildcardSubscriptionAvailable(final boolean wildcardSubscriptionAvailable) {
            isWildcardSubscriptionAvailable = wildcardSubscriptionAvailable;
            return this;
        }

        public Mqtt5Builder withSubscriptionIdentifierAvailable(final boolean subscriptionIdentifierAvailable) {
            isSubscriptionIdentifierAvailable = subscriptionIdentifierAvailable;
            return this;
        }

        public Mqtt5Builder withSharedSubscriptionAvailable(final boolean sharedSubscriptionAvailable) {
            isSharedSubscriptionAvailable = sharedSubscriptionAvailable;
            return this;
        }

        public Mqtt5Builder withResponseInformation(final String responseInformation) {
            this.responseInformation = responseInformation;
            return this;
        }

        public Mqtt5Builder withServerReference(final String serverReference) {
            this.serverReference = serverReference;
            return this;
        }
    }

    @Override
    public int getReceiveMaximum() {
        return receiveMaximum;
    }

    @Override
    public int getTopicAliasMaximum() {
        return topicAliasMaximum;
    }

    @Override
    public int getMaximumPacketSize() {
        return maximumPacketSize;
    }

    @NotNull
    public Mqtt3ConnAckReturnCode getReturnCode() {
        return Mqtt3ConnAckReturnCode.fromReasonCode(getReasonCode());
    }

    @Override
    public boolean isRetainAvailable() {
        return isRetainAvailable;
    }

    @Override
    public boolean isWildcardSubscriptionAvailable() {
        return isWildcardSubscriptionAvailable;
    }

    @Override
    public boolean isSubscriptionIdentifierAvailable() {
        return isSubscriptionIdentifierAvailable;
    }

    @Override
    public boolean isSharedSubscriptionAvailable() {
        return isSharedSubscriptionAvailable;
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    @Override
    public int getServerKeepAlive() {
        return serverKeepAlive;
    }

    @Nullable
    @Override
    public QoS getMaximumQoS() {
        return maximumQoS;
    }

    @Nullable
    @Override
    public String getAssignedClientIdentifier() {
        return assignedClientIdentifier;
    }

    @Nullable
    @Override
    public String getAuthMethod() {
        return authMethod;
    }

    @Nullable
    @Override
    public byte[] getAuthData() {
        return authData;
    }

    @Nullable
    @Override
    public String getResponseInformation() {
        return responseInformation;
    }

    @NotNull
    @Override
    public MessageType getType() {
        return MessageType.CONNACK;
    }

    @Nullable
    @Override
    public String getServerReference() {
        return serverReference;
    }

}
