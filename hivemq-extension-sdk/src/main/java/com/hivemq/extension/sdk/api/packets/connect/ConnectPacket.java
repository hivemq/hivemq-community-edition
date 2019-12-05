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
package com.hivemq.extension.sdk.api.packets.connect;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Contains all information from a CONNECT packet.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
@Immutable
public interface ConnectPacket {

    /**
     * The {@link MqttVersion} the clients wants to use for the connection.
     *
     * @return The MQTT version.
     * @since 4.0.0
     */
    @NotNull MqttVersion getMqttVersion();

    /**
     * The client identifier the client wants to use.
     * <p>
     * An MQTT 5 client may send an empty string.
     *
     * @return The client identifier.
     * @since 4.0.0
     */
    @NotNull String getClientId();

    /**
     * Flag that indicates if the existing session for the client should be continued (<b>false</b>) or the existing
     * session should be overwritten (<b>true</b>). Has no effect if no session for the client exists.
     * <p>
     * For an MQTT 3 client, this MQTT 5 property can be interpreted as followed:
     * <ul>
     * <li>cleanSession = true -&gt; cleanStart = true</li>
     * <li>cleanSession = false -&gt; cleanStart = false</li>
     * </ul>
     *
     * @return The clean start flag.
     * @since 4.0.0
     */
    boolean getCleanStart();

    /**
     * Contains the {@link WillPublishPacket} if it was sent in the CONNECT packet.
     *
     * @return An {@link Optional} that contains the {@link WillPublishPacket} if present.
     * @since 4.0.0
     */
    @NotNull Optional<WillPublishPacket> getWillPublish();

    /**
     * Duration in seconds how long session for the client is stored.
     * <p>
     * For an MQTT 3 client, this MQTT 5 property can be interpreted as followed:
     * <ul>
     * <li>cleanSession = true -&gt; sessionExpiryInterval = 0</li>
     * <li>cleanSession = false -&gt; sessionExpiryInterval = The configured {@code <session-expiry>} in the {@code
     * <mqtt>} config in the config.xml. Default: 4294967295</li>
     * </ul>
     *
     * @return The session expiry interval.
     * @since 4.0.0
     */
    long getSessionExpiryInterval();

    /**
     * An interval in seconds in which the client has to send any MQTT control packet, so that HiveMQ doesn't end the
     * connection.
     *
     * @return The keep alive.
     * @since 4.0.0
     */
    int getKeepAlive();

    /**
     * The limit of QoS 1 and QoS 2 {@link Publish}es that the client is willing
     * to
     * process concurrently.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be 65,535.
     *
     * @return The receive maximum.
     * @since 4.0.0
     */
    int getReceiveMaximum();

    /**
     * The maximum packet size in bytes for an MQTT Control Packet, the client is willing to accept.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be 268435460 bytes (protocol limit for the packet size).
     *
     * @return The maximum packet size.
     * @since 4.0.0
     */
    long getMaximumPacketSize();

    /**
     * The maximum amount of topic alias the client allows for this connection.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be 0.
     *
     * @return The topic alias maximum.
     * @since 4.0.0
     */
    int getTopicAliasMaximum();

    /**
     * This flag indicates if the client wants to receive Response Information in the CONNACK packet (<b>true</b>) or
     * not (<b>false</b>).
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be false.
     *
     * @return The request response information flag.
     * @since 4.0.0
     */
    boolean getRequestResponseInformation();

    /**
     * This flag indicates if the server may sent Reason String or User Properties in the case of failures.
     * If <b>true</b> server may sent these in any MQTT packet, for <b>false</b> they may only be sent in a PUBLISH,
     * CONNACK, or DISCONNECT packet.
     * <p>
     * For an MQTT 3 client this MQTT 5 property will always be false.
     * This can be ignored for MQTT 3 clients.
     *
     * @return The request problem information flag.
     * @since 4.0.0
     */
    boolean getRequestProblemInformation();

    /**
     * If this property is present, the string contains the authentication method that should be used for the extended
     * authentication.
     * <p>
     * For an MQTT 3 client this property can be set in the {@link ConnectInboundInterceptor}.
     *
     * @return An {@link Optional} that contains the authentication method if present.
     * @since 4.0.0
     */
    @NotNull Optional<String> getAuthenticationMethod();

    /**
     * If this property is present, the {@link ByteBuffer} contains the data used for the extended authentication.
     * The contents of this data are defined by the authentication method.
     * <p>
     * For an MQTT 3 client this property can be set in the {@link ConnectInboundInterceptor}.
     *
     * @return An {@link Optional} that contains the authentication data if present.
     * @since 4.0.0
     */
    @NotNull Optional<ByteBuffer> getAuthenticationData();

    /**
     * The user properties from the CONNECT packet.
     * <p>
     * For an MQTT 3 client this property can be set in the {@link ConnectInboundInterceptor}.
     *
     * @return The user properties.
     * @since 4.0.0
     */
    @NotNull UserProperties getUserProperties();

    /**
     * If this property is present, this is the username for the client.
     *
     * @return An {@link Optional} that contains the username if present.
     * @since 4.0.0
     */
    @NotNull Optional<String> getUserName();

    /**
     * If this property is present, this is the password for the client.
     *
     * @return An {@link Optional} that contains the password if present.
     * @since 4.0.0
     */
    @NotNull Optional<ByteBuffer> getPassword();
}
