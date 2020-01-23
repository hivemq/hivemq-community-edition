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
     * This is a success code.
     *
     * @since 4.0.0
     */
    NORMAL_DISCONNECTION,
    /**
     * Can only be used for the {@link ModifiableInboundDisconnectPacket}.
     * <p>
     * This is a success code.
     *
     * @since 4.0.0
     */
    DISCONNECT_WITH_WILL_MESSAGE,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    UNSPECIFIED_ERROR,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    MALFORMED_PACKET,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    PROTOCOL_ERROR,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    IMPLEMENTATION_SPECIFIC_ERROR,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    NOT_AUTHORIZED,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    SERVER_BUSY,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    SERVER_SHUTTING_DOWN,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    BAD_AUTHENTICATION_METHOD,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    KEEP_ALIVE_TIMEOUT,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    SESSION_TAKEN_OVER,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     * @deprecated Must not be used for disconnect packets.
     */
    @Deprecated
    CLIENT_IDENTIFIER_NOT_VALID,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    TOPIC_FILTER_INVALID,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    TOPIC_NAME_INVALID,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    RECEIVE_MAXIMUM_EXCEEDED,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    TOPIC_ALIAS_INVALID,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    PACKET_TOO_LARGE,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    MESSAGE_RATE_TOO_HIGH,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    QUOTA_EXCEEDED,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    ADMINISTRATIVE_ACTION,
    /**
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    PAYLOAD_FORMAT_INVALID,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    RETAIN_NOT_SUPPORTED,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    QOS_NOT_SUPPORTED,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    USE_ANOTHER_SERVER,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    SERVER_MOVED,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    SHARED_SUBSCRIPTION_NOT_SUPPORTED,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    CONNECTION_RATE_EXCEEDED,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    MAXIMUM_CONNECT_TIME,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
    /**
     * Can only be used for the {@link ModifiableOutboundDisconnectPacket}.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    WILDCARD_SUBSCRIPTION_NOT_SUPPORTED
}
