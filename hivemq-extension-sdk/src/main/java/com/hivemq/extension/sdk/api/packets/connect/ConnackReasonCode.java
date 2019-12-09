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

/**
 * Reason code for the MQTT CONNACK packet.
 * <p>
 * These reason codes represent the MQTT 5 reason codes.
 * <p>
 * For MQTT version 3.1 and 3.1.1 these reason codes are automatically translated by HiveMQ.
 * <p>
 * The reason codes are translated as follows:
 *
 * <table border="1">
 * <tr><th>MQTT 3 CONNACK reason code</th><th>ConnackReasonCode</th></tr>
 * <tr><td>ACCEPTED</td><td>SUCCESS</td></tr>
 * <tr><td>REFUSED_UNACCEPTABLE_PROTOCOL_VERSION</td><td>UNSUPPORTED_PROTOCOL_VERSION</td></tr>
 * <tr><td>REFUSED_IDENTIFIER_REJECTED</td><td>CLIENT_IDENTIFIER_NOT_VALID</td></tr>
 * <tr><td>REFUSED_SERVER_UNAVAILABLE</td><td>SERVER_UNAVAILABLE, SERVER_BUSY, USE_ANOTHER_SERVER,
 * SERVER_MOVED</td></tr>
 * <tr><td>REFUSED_BAD_USERNAME_OR_PASSWORD</td><td>BAD_USER_NAME_OR_PASSWORD, BAD_AUTHENTICATION_METHOD</td></tr>
 * <tr><td>REFUSED_NOT_AUTHORIZED</td><td>NOT_AUTHORIZED, UNSPECIFIED_ERROR, MALFORMED_PACKET, PROTOCOL_ERROR,
 * IMPLEMENTATION_SPECIFIC_ERROR, BANNED, TOPIC_NAME_INVALID, PACKET_TOO_LARGE, QUOTA_EXCEEDED,
 * PAYLOAD_FORMAT_INVALID, RETAIN_NOT_SUPPORTED, QOS_NOT_SUPPORTED, CONNECTION_RATE_EXCEEDED</td></tr>
 * </table>
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public enum ConnackReasonCode {
    /**
     * For an MQTT 3 client this will be translated to the return code ACCEPTED.
     * <p>
     * This is a success code.
     *
     * @since 4.0.0
     */
    SUCCESS,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    UNSPECIFIED_ERROR,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    MALFORMED_PACKET,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    PROTOCOL_ERROR,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    IMPLEMENTATION_SPECIFIC_ERROR,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_UNACCEPTABLE_PROTOCOL_VERSION.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    UNSUPPORTED_PROTOCOL_VERSION,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_IDENTIFIER_REJECTED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    CLIENT_IDENTIFIER_NOT_VALID,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_BAD_USERNAME_OR_PASSWORD.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    BAD_USER_NAME_OR_PASSWORD,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    NOT_AUTHORIZED,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_SERVER_UNAVAILABLE.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    SERVER_UNAVAILABLE,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_SERVER_UNAVAILABLE.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    SERVER_BUSY,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    BANNED,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_BAD_USERNAME_OR_PASSWORD.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    BAD_AUTHENTICATION_METHOD,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    TOPIC_NAME_INVALID,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    PACKET_TOO_LARGE,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    QUOTA_EXCEEDED,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    PAYLOAD_FORMAT_INVALID,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    RETAIN_NOT_SUPPORTED,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    QOS_NOT_SUPPORTED,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_SERVER_UNAVAILABLE.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    USE_ANOTHER_SERVER,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_SERVER_UNAVAILABLE.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    SERVER_MOVED,
    /**
     * For an MQTT 3 client this will be translated to the return code REFUSED_NOT_AUTHORIZED.
     * <p>
     * This is an unsuccessful code.
     *
     * @since 4.0.0
     */
    CONNECTION_RATE_EXCEEDED
}
