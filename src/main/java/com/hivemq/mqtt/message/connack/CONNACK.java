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

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extensions.packets.connack.ConnackPacketImpl;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties.MqttMessageWithReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.Bytes;

/**
 * The MQTT CONNACK message
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 * @since 4.0.0
 */
@Immutable
public class CONNACK extends MqttMessageWithReasonCode<Mqtt5ConnAckReasonCode> implements Mqtt3CONNACK, Mqtt5CONNACK {

    public static @NotNull CONNACKBuilder builder() {
        return new CONNACKBuilder();
    }

    public static @NotNull CONNACK from(final @NotNull ConnackPacketImpl packet) {

        final Qos extensionMaxQos = packet.getMaximumQoS().orElse(null);
        final QoS qoS = (extensionMaxQos != null) ? QoS.valueOf(extensionMaxQos.getQosNumber()) : null;

        return new CONNACKBuilder()
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

    public static final long SESSION_EXPIRY_NOT_SET = Long.MAX_VALUE;
    public static final int KEEP_ALIVE_NOT_SET = -1;

    private final boolean sessionPresent;

    //Mqtt 5
    private final long sessionExpiryInterval;
    private final int serverKeepAlive;
    private final @Nullable String assignedClientIdentifier;

    //Auth
    private final @Nullable String authMethod;
    private final byte @Nullable [] authData;

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

    CONNACK(final @NotNull Mqtt5ConnAckReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final boolean sessionPresent,
            final long sessionExpiryInterval,
            final int serverKeepAlive,
            final @Nullable String assignedClientIdentifier,
            final @Nullable String authMethod,
            final byte @Nullable [] authData,
            final int receiveMaximum,
            final int topicAliasMaximum,
            final int maximumPacketSize,
            final @Nullable QoS maximumQoS,
            final boolean isRetainAvailable,
            final boolean isWildcardSubscriptionAvailable,
            final boolean isSubscriptionIdentifierAvailable,
            final boolean isSharedSubscriptionAvailable,
            final @Nullable String responseInformation,
            final @Nullable String serverReference) {

        super(reasonCode, reasonString, userProperties);

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

    @Override
    public @NotNull Mqtt3ConnAckReturnCode getReturnCode() {
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

    @Override
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

    @Override
    public @Nullable QoS getMaximumQoS() {
        return maximumQoS;
    }

    @Override
    public @Nullable String getAssignedClientIdentifier() {
        return assignedClientIdentifier;
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
    public @Nullable String getResponseInformation() {
        return responseInformation;
    }

    @Override
    public @NotNull MessageType getType() {
        return MessageType.CONNACK;
    }

    @Override
    public @Nullable String getServerReference() {
        return serverReference;
    }
}
