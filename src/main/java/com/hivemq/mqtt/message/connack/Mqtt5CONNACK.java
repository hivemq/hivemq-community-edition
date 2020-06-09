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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger;
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;

/**
 * @author Florian Limpöck
 *
 * @since 4.0.0
 */
public interface Mqtt5CONNACK extends Message {

    /**
     * The default maximum amount of not acknowledged publishes with QoS 1 or 2 the server accepts concurrently.
     */
    int DEFAULT_RECEIVE_MAXIMUM = UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE;

    /**
     * The default maximum amount of topic aliases the server accepts from the client.
     */
    int DEFAULT_TOPIC_ALIAS_MAXIMUM = 0;

    /**
     * The default maximum packet size the server accepts from the client which indicates that the packet size is
     * not limited beyond the restrictions of the encoding.
     */
    int DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT = MqttVariableByteInteger.MAXIMUM_PACKET_SIZE_LIMIT;

    /**
     * The default maximum QoS the server accepts from the client.
     */
    QoS DEFAULT_MAXIMUM_QOS = QoS.EXACTLY_ONCE;
    /**
     * The default for whether the server accepts retained messages.
     */
    boolean DEFAULT_RETAIN_AVAILABLE = true;
    /**
     * The default for whether the server accepts wildcard subscriptions.
     */
    boolean DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE = true;
    /**
     * The default for whether the server accepts subscription identifiers.
     */
    boolean DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE = true;
    /**
     * The default for whether the server accepts shared subscriptions.
     */
    boolean DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE = true;

    /**
     * @return the maximum amount of not acknowledged publishes with QoS 1 or 2 the server accepts concurrently. The
     * default is {@link #DEFAULT_RECEIVE_MAXIMUM}.
     */
    int getReceiveMaximum();

    /**
     * @return the maximum amount of topic aliases the server accepts from the client. The default is {@link
     * #DEFAULT_TOPIC_ALIAS_MAXIMUM}.
     */
    int getTopicAliasMaximum();

    /**
     * @return the maximum packet size the server accepts from the client. The default is {@link
     * #DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT}.
     */
    int getMaximumPacketSize();

    /**
     * @return the maximum QoS the server accepts from the client. The default is {@link #DEFAULT_MAXIMUM_QOS}.
     */
    QoS getMaximumQoS();

    /**
     * @return whether the server accepts retained messages. The default is {@link #DEFAULT_RETAIN_AVAILABLE}.
     */
    boolean isRetainAvailable();

    /**
     * @return whether the server accepts wildcard subscriptions. The default is {@link
     * #DEFAULT_WILDCARD_SUBSCRIPTION_AVAILABLE}.
     */
    boolean isWildcardSubscriptionAvailable();

    /**
     * @return whether the server accepts subscription identifiers. The default is {@link
     * #DEFAULT_SUBSCRIPTION_IDENTIFIER_AVAILABLE}.
     */
    boolean isSubscriptionIdentifierAvailable();

    /**
     * @return whether the server accepts shared subscriptions. The default is {@link
     * #DEFAULT_SHARED_SUBSCRIPTION_AVAILABLE}.
     */
    boolean isSharedSubscriptionAvailable();

    /**
     * @return the reason code of this CONNACK packet.
     */
    @NotNull
    Mqtt5ConnAckReasonCode getReasonCode();

    /**
     * Returns <code>true</code> if there is already a session present on the
     * MQTT Broker for a client. Returns <code>false</code> if the client
     * has a clean start
     *
     * @return if there is a session present on the MQTT broker
     * @since 4.0.0
     */
    boolean isSessionPresent();

    /**
     * @return the optional session expiry interval set from the server. If absent, the session expiry interval from the
     * CONNECT packet is used.
     */
    long getSessionExpiryInterval();

    /**
     * @return the optional keep alive set from the server. If absent, the keep alive from the CONNECT packet is used.
     */
    int getServerKeepAlive();

    /**
     * @return the optional client identifier assigned by the server. If absent, the client identifier from the CONNECT
     * packet is used.
     */
    @Nullable
    String getAssignedClientIdentifier();

    /**
     * @return the authentication/authorization method.
     */
    @Nullable
    String getAuthMethod();

    /**
     * @return the optional authentication/authorization data.
     */
    @Nullable
    byte[] getAuthData();

    /**
     * @return the optional response information of this CONNACK packet to retrieve a response topic from.
     */
    @Nullable
    String getResponseInformation();

    /**
     * @return the optional server reference.
     */
    @Nullable
    String getServerReference();

    /**
     * @return the optional reason string of this CONNACK packet.
     */
    @Nullable
    String getReasonString();

    /**
     * @return the optional user properties of this CONNACK packet.
     */
    @NotNull
    Mqtt5UserProperties getUserProperties();

}
