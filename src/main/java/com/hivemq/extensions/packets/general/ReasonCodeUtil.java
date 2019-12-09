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

import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.annotations.Nullable;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Georg Held
 */
public class ReasonCodeUtil {

    private static final Logger log = LoggerFactory.getLogger(ReasonCodeUtil.class);

    @Nullable
    public static Mqtt5ConnAckReasonCode toMqtt5(@Nullable final ConnackReasonCode connackReasonCode) {
        if (connackReasonCode == null) {
            return null;
        }
        try {
            return Mqtt5ConnAckReasonCode.valueOf(connackReasonCode.name());
        } catch (final IllegalArgumentException e){
            //should never happen
            log.debug("No Mqtt5ConnAckReasonCode found for the given value: {}" , connackReasonCode.name());
            return null;
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

            case SERVER_UNAVAILABLE:
            case SERVER_BUSY:
            case USE_ANOTHER_SERVER:
            case SERVER_MOVED:
                return Mqtt3ConnAckReturnCode.REFUSED_SERVER_UNAVAILABLE;

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
            default:
                return Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED;
        }
    }

    @NotNull
    public static ConnackReasonCode toConnackReasonCode(@NotNull final DisconnectedReasonCode disconnectReasonCode) {
        switch (disconnectReasonCode) {
            case SUCCESS:
                return ConnackReasonCode.SUCCESS;
            case NOT_AUTHORIZED:
                return ConnackReasonCode.NOT_AUTHORIZED;
            case UNSPECIFIED_ERROR:
                return ConnackReasonCode.UNSPECIFIED_ERROR;
            case BAD_AUTHENTICATION_METHOD:
                return ConnackReasonCode.BAD_AUTHENTICATION_METHOD;
            case MALFORMED_PACKET:
                return ConnackReasonCode.MALFORMED_PACKET;
            case PROTOCOL_ERROR:
                return ConnackReasonCode.PROTOCOL_ERROR;
            case IMPLEMENTATION_SPECIFIC_ERROR:
                return ConnackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR;
            case SERVER_BUSY:
                return ConnackReasonCode.SERVER_BUSY;
            case CLIENT_IDENTIFIER_NOT_VALID:
                return ConnackReasonCode.CLIENT_IDENTIFIER_NOT_VALID;
            case TOPIC_NAME_INVALID:
                return ConnackReasonCode.TOPIC_NAME_INVALID;
            case PACKET_TOO_LARGE:
                return ConnackReasonCode.PACKET_TOO_LARGE;
            case QUOTA_EXCEEDED:
                return ConnackReasonCode.QUOTA_EXCEEDED;
            case PAYLOAD_FORMAT_INVALID:
                return ConnackReasonCode.PAYLOAD_FORMAT_INVALID;
            case RETAIN_NOT_SUPPORTED:
                return ConnackReasonCode.RETAIN_NOT_SUPPORTED;
            case QOS_NOT_SUPPORTED:
                return ConnackReasonCode.QOS_NOT_SUPPORTED;
            case USE_ANOTHER_SERVER:
                return ConnackReasonCode.USE_ANOTHER_SERVER;
            case SERVER_MOVED:
                return ConnackReasonCode.SERVER_MOVED;
            case UNSUPPORTED_PROTOCOL_VERSION:
                return ConnackReasonCode.UNSUPPORTED_PROTOCOL_VERSION;
            case BAD_USER_NAME_OR_PASSWORD:
                return ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD;
            case SERVER_UNAVAILABLE:
                return ConnackReasonCode.SERVER_UNAVAILABLE;
            case BANNED:
                return ConnackReasonCode.BANNED;

            //No connack reason code available for this.
            case MAXIMUM_CONNECT_TIME:
            case SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED:
            case NORMAL_DISCONNECTION:
            case DISCONNECT_WITH_WILL_MESSAGE:
            case SERVER_SHUTTING_DOWN:
            case KEEP_ALIVE_TIMEOUT:
            case SESSION_TAKEN_OVER:
            case TOPIC_FILTER_INVALID:
            case RECEIVE_MAXIMUM_EXCEEDED:
            case TOPIC_ALIAS_INVALID:
            case MESSAGE_RATE_TOO_HIGH:
            case ADMINISTRATIVE_ACTION:
            case SHARED_SUBSCRIPTION_NOT_SUPPORTED:
            default:
                return ConnackReasonCode.UNSPECIFIED_ERROR;
        }
    }

    @Nullable
    public static Mqtt5ConnAckReasonCode toMqtt5ConnAckReasonCode(@NotNull final DisconnectedReasonCode disconnectedReasonCode) {

        switch (disconnectedReasonCode) {
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
            case NOT_AUTHORIZED:
                return Mqtt5ConnAckReasonCode.NOT_AUTHORIZED;
            case SERVER_BUSY:
                return Mqtt5ConnAckReasonCode.SERVER_BUSY;
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
            case UNSUPPORTED_PROTOCOL_VERSION:
                return Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION;
            case CLIENT_IDENTIFIER_NOT_VALID:
                return Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID;
            case BAD_USER_NAME_OR_PASSWORD:
                return Mqtt5ConnAckReasonCode.BAD_USER_NAME_OR_PASSWORD;
            case SERVER_UNAVAILABLE:
                return Mqtt5ConnAckReasonCode.SERVER_UNAVAILABLE;
            case BANNED:
                return Mqtt5ConnAckReasonCode.BANNED;
            default:
                return null;
        }
    }

    @Nullable
    public static Mqtt5DisconnectReasonCode toMqtt5DisconnectReasonCode(@NotNull final DisconnectedReasonCode disconnectedReasonCode) {

        switch (disconnectedReasonCode) {
            case UNSPECIFIED_ERROR:
                return Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR;
            case MALFORMED_PACKET:
                return Mqtt5DisconnectReasonCode.MALFORMED_PACKET;
            case PROTOCOL_ERROR:
                return Mqtt5DisconnectReasonCode.PROTOCOL_ERROR;
            case IMPLEMENTATION_SPECIFIC_ERROR:
                return Mqtt5DisconnectReasonCode.IMPLEMENTATION_SPECIFIC_ERROR;
            case NOT_AUTHORIZED:
                return Mqtt5DisconnectReasonCode.NOT_AUTHORIZED;
            case SERVER_BUSY:
                return Mqtt5DisconnectReasonCode.SERVER_BUSY;
            case BAD_AUTHENTICATION_METHOD:
                return Mqtt5DisconnectReasonCode.BAD_AUTHENTICATION_METHOD;
            case TOPIC_NAME_INVALID:
                return Mqtt5DisconnectReasonCode.TOPIC_NAME_INVALID;
            case PACKET_TOO_LARGE:
                return Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE;
            case QUOTA_EXCEEDED:
                return Mqtt5DisconnectReasonCode.QUOTA_EXCEEDED;
            case PAYLOAD_FORMAT_INVALID:
                return Mqtt5DisconnectReasonCode.PAYLOAD_FORMAT_INVALID;
            case RETAIN_NOT_SUPPORTED:
                return Mqtt5DisconnectReasonCode.RETAIN_NOT_SUPPORTED;
            case QOS_NOT_SUPPORTED:
                return Mqtt5DisconnectReasonCode.QOS_NOT_SUPPORTED;
            case USE_ANOTHER_SERVER:
                return Mqtt5DisconnectReasonCode.USE_ANOTHER_SERVER;
            case SERVER_MOVED:
                return Mqtt5DisconnectReasonCode.SERVER_MOVED;
            case NORMAL_DISCONNECTION:
                return Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION;
            case DISCONNECT_WITH_WILL_MESSAGE:
                return Mqtt5DisconnectReasonCode.DISCONNECT_WITH_WILL_MESSAGE;
            case SERVER_SHUTTING_DOWN:
                return Mqtt5DisconnectReasonCode.SERVER_SHUTTING_DOWN;
            case KEEP_ALIVE_TIMEOUT:
                return Mqtt5DisconnectReasonCode.KEEP_ALIVE_TIMEOUT;
            case SESSION_TAKEN_OVER:
                return Mqtt5DisconnectReasonCode.SESSION_TAKEN_OVER;
            case TOPIC_FILTER_INVALID:
                return Mqtt5DisconnectReasonCode.TOPIC_FILTER_INVALID;
            case RECEIVE_MAXIMUM_EXCEEDED:
                return Mqtt5DisconnectReasonCode.RECEIVE_MAXIMUM_EXCEEDED;
            case TOPIC_ALIAS_INVALID:
                return Mqtt5DisconnectReasonCode.TOPIC_ALIAS_INVALID;
            case MESSAGE_RATE_TOO_HIGH:
                return Mqtt5DisconnectReasonCode.MESSAGE_RATE_TOO_HIGH;
            case ADMINISTRATIVE_ACTION:
                return Mqtt5DisconnectReasonCode.ADMINISTRATIVE_ACTION;
            case SHARED_SUBSCRIPTION_NOT_SUPPORTED:
                return Mqtt5DisconnectReasonCode.SHARED_SUBSCRIPTION_NOT_SUPPORTED;
            case CONNECTION_RATE_EXCEEDED:
                return Mqtt5DisconnectReasonCode.CONNECTION_RATE_EXCEEDED;
            case MAXIMUM_CONNECT_TIME:
                return Mqtt5DisconnectReasonCode.MAXIMUM_CONNECT_TIME;
            case SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED:
                return Mqtt5DisconnectReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED;
            case WILDCARD_SUBSCRIPTION_NOT_SUPPORTED:
                return Mqtt5DisconnectReasonCode.WILDCARD_SUBSCRIPTION_NOT_SUPPORTED;
            default:
                return null;
        }
    }
}
