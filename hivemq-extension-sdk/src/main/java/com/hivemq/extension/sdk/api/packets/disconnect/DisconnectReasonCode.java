/*
 * Copyright 2018 dc-square GmbH
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
     * @since 4.0.0
     */
    @Deprecated DISCONNECT_WITH_WILL_MESSAGE,
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
     * @since 4.0.0
     */
    NOT_AUTHORIZED,
    /**
     * @since 4.0.0
     */
    SERVER_BUSY,
    /**
     * @since 4.0.0
     */
    SERVER_SHUTTING_DOWN,
    /**
     * @since 4.0.0
     */
    @Deprecated BAD_AUTHENTICATION_METHOD,
    /**
     * @since 4.0.0
     */
    KEEP_ALIVE_TIMEOUT,
    /**
     * @since 4.0.0
     */
    SESSION_TAKEN_OVER,
    /**
     * @since 4.0.0
     */
    @Deprecated CLIENT_IDENTIFIER_NOT_VALID,
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
     * @since 4.0.0
     */
    PAYLOAD_FORMAT_INVALID,
    /**
     * @since 4.0.0
     */
    RETAIN_NOT_SUPPORTED,
    /**
     * @since 4.0.0
     */
    QOS_NOT_SUPPORTED,
    /**
     * @since 4.0.0
     */
    USE_ANOTHER_SERVER,
    /**
     * @since 4.0.0
     */
    SERVER_MOVED,
    /**
     * @since 4.0.0
     */
    SHARED_SUBSCRIPTION_NOT_SUPPORTED,
    /**
     * @since 4.0.0
     */
    CONNECTION_RATE_EXCEEDED,
    /**
     * @since 4.0.0
     */
    MAXIMUM_CONNECT_TIME,
    /**
     * @since 4.0.0
     */
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
    /**
     * @since 4.0.0
     */
    WILDCARD_SUBSCRIPTION_NOT_SUPPORTED
}
