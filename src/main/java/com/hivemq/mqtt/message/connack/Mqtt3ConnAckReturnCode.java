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
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;

/**
 * The return code of a MQTT 3.1.1 {@link CONNACK} message.
 *
 * @author Dominik Obermaier
 * @author Christian Goetz
 * @author Florian Limpöck
 *
 * @since 1.4
 */
public enum Mqtt3ConnAckReturnCode {
    ACCEPTED(0),
    REFUSED_UNACCEPTABLE_PROTOCOL_VERSION(1),
    REFUSED_IDENTIFIER_REJECTED(2),
    REFUSED_SERVER_UNAVAILABLE(3),
    REFUSED_BAD_USERNAME_OR_PASSWORD(4),
    REFUSED_NOT_AUTHORIZED(5);

    private static final @NotNull Mqtt3ConnAckReturnCode @NotNull [] VALUES = values();

    private final int code;

    Mqtt3ConnAckReturnCode(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static @NotNull Mqtt3ConnAckReturnCode fromCode(final int code) {
        try {
            return VALUES[code];
        } catch (final IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("No Return code with value " + code + " defined", e);
        }
    }

    public static @NotNull Mqtt3ConnAckReturnCode fromReasonCode(final @NotNull Mqtt5ConnAckReasonCode reasonCode) {
        switch (reasonCode) {
            case SUCCESS:
                return ACCEPTED;
            case UNSUPPORTED_PROTOCOL_VERSION:
                return REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
            case CLIENT_IDENTIFIER_NOT_VALID:
                return REFUSED_IDENTIFIER_REJECTED;
            case SERVER_UNAVAILABLE:
            case SERVER_BUSY:
            case USE_ANOTHER_SERVER:
            case SERVER_MOVED:
                return REFUSED_SERVER_UNAVAILABLE;
            case BAD_USER_NAME_OR_PASSWORD:
            case BAD_AUTHENTICATION_METHOD:
                return REFUSED_BAD_USERNAME_OR_PASSWORD;
            case UNSPECIFIED_ERROR:
            case MALFORMED_PACKET:
            case PROTOCOL_ERROR:
            case IMPLEMENTATION_SPECIFIC_ERROR:
            case NOT_AUTHORIZED:
            case BANNED:
            case TOPIC_NAME_INVALID:
            case PACKET_TOO_LARGE:
            case QUOTA_EXCEEDED:
            case PAYLOAD_FORMAT_INVALID:
            case RETAIN_NOT_SUPPORTED:
            case QOS_NOT_SUPPORTED:
            case CONNECTION_RATE_EXCEEDED:
            default:
                return REFUSED_NOT_AUTHORIZED;
        }
    }
}