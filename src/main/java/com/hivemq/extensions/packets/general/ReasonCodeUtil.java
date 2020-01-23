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

package com.hivemq.extensions.packets.general;

import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;

/**
 * @author Georg Held
 */
public class ReasonCodeUtil {
    @Nullable
    public static Mqtt5ConnAckReasonCode toMqtt5(@Nullable final ConnackReasonCode connackReasonCode) {
        if (connackReasonCode == null) {
            return null;
        }
        switch (connackReasonCode) {
            case NOT_AUTHORIZED:
                return Mqtt5ConnAckReasonCode.NOT_AUTHORIZED;
            case SUCCESS:
                return Mqtt5ConnAckReasonCode.SUCCESS;
            case UNSPECIFIED_ERROR:
                return Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR;
            case MALFORMED_PACKET:
                return Mqtt5ConnAckReasonCode.MALFORMED_PACKET;
            case PROTOCOL_ERROR:
                return Mqtt5ConnAckReasonCode.PROTOCOL_ERROR;
            case IMPLEMENTATION_SPECIFIC_ERROR:
                return Mqtt5ConnAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR;
            case UNSUPPORTED_PROTOCOL_VERSION:
                return Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION;
            case CLIENT_IDENTIFIER_NOT_VALID:
                return Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID;
            case BAD_USER_NAME_OR_PASSWORD:
                return Mqtt5ConnAckReasonCode.BAD_USER_NAME_OR_PASSWORD;
            case SERVER_UNAVAILABLE:
                return Mqtt5ConnAckReasonCode.SERVER_UNAVAILABLE;
            case SERVER_BUSY:
                return Mqtt5ConnAckReasonCode.SERVER_BUSY;
            case BANNED:
                return Mqtt5ConnAckReasonCode.BANNED;
            case BAD_AUTHENTICATION_METHOD:
                return Mqtt5ConnAckReasonCode.BAD_AUTHENTICATION_METHOD;
            case TOPIC_NAME_INVALID:
                return Mqtt5ConnAckReasonCode.TOPIC_NAME_INVALID;
            case PACKET_TOO_LARGE:
                return Mqtt5ConnAckReasonCode.PACKET_TOO_LARGE;
            case QUOTA_EXCEEDED:
                return Mqtt5ConnAckReasonCode.QUOTA_EXCEEDED;
            case PAYLOAD_FORMAT_INVALID:
                return Mqtt5ConnAckReasonCode.PAYLOAD_FORMAT_INVALID;
            case RETAIN_NOT_SUPPORTED:
                return Mqtt5ConnAckReasonCode.RETAIN_NOT_SUPPORTED;
            case QOS_NOT_SUPPORTED:
                return Mqtt5ConnAckReasonCode.QOS_NOT_SUPPORTED;
            case USE_ANOTHER_SERVER:
                return Mqtt5ConnAckReasonCode.USE_ANOTHER_SERVER;
            case SERVER_MOVED:
                return Mqtt5ConnAckReasonCode.SERVER_MOVED;
            case CONNECTION_RATE_EXCEEDED:
                return Mqtt5ConnAckReasonCode.CONNECTION_RATE_EXCEEDED;
            default:
                return Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR;

        }
    }

    @Nullable
    public static Mqtt3ConnAckReturnCode toMqtt3(@Nullable final ConnackReasonCode connackReasonCode) {
        if (connackReasonCode == null) {
            return null;
        }
        switch (connackReasonCode) {

            case SUCCESS:
                return Mqtt3ConnAckReturnCode.ACCEPTED;
            case UNSUPPORTED_PROTOCOL_VERSION:
                return Mqtt3ConnAckReturnCode.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
            case BAD_AUTHENTICATION_METHOD:
            case BAD_USER_NAME_OR_PASSWORD:
                return Mqtt3ConnAckReturnCode.REFUSED_BAD_USERNAME_OR_PASSWORD;
            case CLIENT_IDENTIFIER_NOT_VALID:
                return Mqtt3ConnAckReturnCode.REFUSED_IDENTIFIER_REJECTED;
            case UNSPECIFIED_ERROR:
            case MALFORMED_PACKET:
            case PROTOCOL_ERROR:
            case IMPLEMENTATION_SPECIFIC_ERROR:
            case BANNED:
            case TOPIC_NAME_INVALID:
            case PACKET_TOO_LARGE:
            case QUOTA_EXCEEDED:
            case PAYLOAD_FORMAT_INVALID:
            case RETAIN_NOT_SUPPORTED:
            case QOS_NOT_SUPPORTED:
            case CONNECTION_RATE_EXCEEDED:
            case NOT_AUTHORIZED:
                return Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED;
            case SERVER_UNAVAILABLE:
            case SERVER_BUSY:
            case USE_ANOTHER_SERVER:
            case SERVER_MOVED:
                return Mqtt3ConnAckReturnCode.REFUSED_SERVER_UNAVAILABLE;
            default:
                return Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED;
        }
    }
}
