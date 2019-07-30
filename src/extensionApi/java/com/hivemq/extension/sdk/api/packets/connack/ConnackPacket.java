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

package com.hivemq.extension.sdk.api.packets.connack;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.services.publish.Publish;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Represents a CONNACK packet.
 * <p>
 * Contains all values of an MQTT 5 CONNACK, but will also used to represent MQTT 3 connack packets.
 *
 * @author Florian Limpöck
 * @since 4.2.0
 */
public interface ConnackPacket {

    /**
     * Duration in seconds how long session for the client is stored.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} that contains the session expiry interval.
     * @since 4.2.0
     */
    @NotNull
    Optional<Long> getSessionExpiryInterval();

    /**
     * An interval in seconds in which the client has to send any MQTT control packet, so that HiveMQ doesn't end the
     * connection.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} that contains the server keep alive.
     * @since 4.2.0
     */
    @NotNull
    Optional<Integer> getServerKeepAlive();

    /**
     * The limit of QoS 1 and QoS 2 {@link Publish}es that the server is willing
     * to process concurrently.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be 65,535.
     *
     * @return The receive maximum.
     * @since 4.2.0
     */
    int getReceiveMaximum();

    /**
     * The maximum packet size in bytes for an MQTT Control Packet, the server is willing to accept.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be the configured {@code <max-packet-size>} in the {@code
     * <mqtt>} config in the config.xml Default: 268435460 bytes.
     *
     * @return The maximum packet size.
     * @since 4.2.0
     */
    int getMaximumPacketSize();

    /**
     * The maximum amount of topic aliases the server allows for this connection.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be 0.
     *
     * @return The topic alias maximum.
     * @since 4.2.0
     */
    int getTopicAliasMaximum();

    /**
     * The maximum quality of service level (QoS) the server allows for this connection.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} that contains the QoS maximum.
     * @since 4.2.0
     */
    @NotNull
    Optional<Qos> getMaximumQoS();

    /**
     * The user properties from the CONNACK packet.
     * <p>
     * The properties will always be empty for an MQTT 3 client.
     *
     * @return The user properties.
     * @since 4.2.0
     */
    @NotNull
    UserProperties getUserProperties();

    /**
     * The reason code from the CONNACK packet.
     * <p>
     *
     * @return The connack reason code.
     * @see ConnackReasonCode How reason codes are translated from MQTT 5 to MQTT 3.
     * @since 4.2.0
     */
    @NotNull
    ConnackReasonCode getReasonCode();

    /**
     * The session present flag from the CONNACK packet.
     *
     * @return True if a session was already present for this client, else false.
     * @since 4.2.0
     */
    boolean getSessionPresent();

    /**
     * The retain available flag from the CONNACK packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property is always true.
     *
     * @return True if retained messages are allowed by HiveMQ, else false.
     * @since 4.2.0
     */
    boolean getRetainAvailable();

    /**
     * The assigned client identifier of the CONNACK packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     * <p>
     * For an MQTT 5 client this property is present when HiveMQ assigned the client identifier.
     *
     * @return An {@link Optional} that contains the assigned client identifier if present.
     * @since 4.2.0
     */
    @NotNull
    Optional<String> getAssignedClientIdentifier();

    /**
     * The reason string of the CONNACK packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} containing the connack reason string if present.
     * @since 4.2.0
     */
    @NotNull
    Optional<String> getReasonString();

    /**
     * The wildcard subscription available flag from the CONNACK packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property is always true.
     *
     * @return True if wildcard subscriptions are allowed by HiveMQ, else false.
     * @since 4.2.0
     */
    boolean getWildCardSubscriptionAvailable();

    /**
     * The subscription identifier available flag from the CONNACK packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property is always true.
     *
     * @return True if subscription identifiers are allowed by HiveMQ, else false.
     * @since 4.2.0
     */
    boolean getSubscriptionIdentifiersAvailable();

    /**
     * The shared subscriptions available flag from the CONNACK packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property is always true.
     *
     * @return True if shared subscriptions are allowed by HiveMQ, else false.
     * @since 4.2.0
     */
    boolean getSharedSubscriptionsAvailable();

    /**
     * The response information of the CONNACK packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} containing the response information if present.
     * @since 4.2.0
     */
    @NotNull
    Optional<String> getResponseInformation();

    /**
     * The server reference of the CONNACK packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} containing the server reference if present.
     * @since 4.2.0
     */
    @NotNull
    Optional<String> getServerReference();

    /**
     * If this property is present, the string contains the authentication method that should be used for the extended
     * authentication.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} that contains the authentication method if present.
     * @since 4.2.0
     */
    @NotNull
    Optional<String> getAuthenticationMethod();

    /**
     * If this property is present, the {@link ByteBuffer} contains the data used for the extended authentication.
     * The contents of this data are defined by the authentication method.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} that contains the authentication data if present.
     * @since 4.2.0
     */
    @NotNull
    Optional<ByteBuffer> getAuthenticationData();

}
