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
package com.hivemq.extension.sdk.api.packets.disconnect;

/**
 * MQTT 5 disconnect reason codes are listed here.
 * <p>
 * MQTT 3 does not support sending a DISCONNECT packet from server to client.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public enum DisconnectReasonCode {

    /**
     * @since 4.0.0
     */
    NORMAL_DISCONNECTION,
    /**
     * Must not be used for outbound disconnect packets from the server to clients.
     *
     * @since 4.0.0
     */
    DISCONNECT_WITH_WILL_MESSAGE,
    /**
     * @since 4.0.0
     */
    UNSPECIFIED_ERROR,
    /**
     * @since 4.0.0
     */
    MALFORMED_PACKET,
    /**
     * @since 4.0.0
     */
    PROTOCOL_ERROR,
    /**
     * @since 4.0.0
     */
    IMPLEMENTATION_SPECIFIC_ERROR,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    NOT_AUTHORIZED,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    SERVER_BUSY,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    SERVER_SHUTTING_DOWN,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    BAD_AUTHENTICATION_METHOD,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    KEEP_ALIVE_TIMEOUT,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    SESSION_TAKEN_OVER,
    /**
     * @since 4.0.0
     * @deprecated Must not be used for disconnect packets.
     */
    @Deprecated
    CLIENT_IDENTIFIER_NOT_VALID,
    /**
     * @since 4.0.0
     */
    TOPIC_FILTER_INVALID,
    /**
     * @since 4.0.0
     */
    TOPIC_NAME_INVALID,
    /**
     * @since 4.0.0
     */
    RECEIVE_MAXIMUM_EXCEEDED,
    /**
     * @since 4.0.0
     */
    TOPIC_ALIAS_INVALID,
    /**
     * @since 4.0.0
     */
    PACKET_TOO_LARGE,
    /**
     * @since 4.0.0
     */
    MESSAGE_RATE_TOO_HIGH,
    /**
     * @since 4.0.0
     */
    QUOTA_EXCEEDED,
    /**
     * @since 4.0.0
     */
    ADMINISTRATIVE_ACTION,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    PAYLOAD_FORMAT_INVALID,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    RETAIN_NOT_SUPPORTED,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    QOS_NOT_SUPPORTED,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    USE_ANOTHER_SERVER,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    SERVER_MOVED,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    SHARED_SUBSCRIPTION_NOT_SUPPORTED,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    CONNECTION_RATE_EXCEEDED,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    MAXIMUM_CONNECT_TIME,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
    /**
     * Must not be used for inbound disconnect packets from a client to the server.
     *
     * @since 4.0.0
     */
    WILDCARD_SUBSCRIPTION_NOT_SUPPORTED
}
