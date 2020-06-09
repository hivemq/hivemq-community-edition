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
package com.hivemq.mqtt.message.reason;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;

/**
 * MQTT Reason Codes that can be used in CONNACK packets according to the MQTT 5 specification.
 *
 * @author Silvio Giebl
 */
public enum Mqtt5ConnAckReasonCode implements Mqtt5ReasonCode {

    SUCCESS(MqttCommonReasonCode.SUCCESS),
    UNSPECIFIED_ERROR(MqttCommonReasonCode.UNSPECIFIED_ERROR),
    MALFORMED_PACKET(MqttCommonReasonCode.MALFORMED_PACKET),
    PROTOCOL_ERROR(MqttCommonReasonCode.PROTOCOL_ERROR),
    IMPLEMENTATION_SPECIFIC_ERROR(MqttCommonReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
    UNSUPPORTED_PROTOCOL_VERSION(0x84),
    CLIENT_IDENTIFIER_NOT_VALID(0x85),
    BAD_USER_NAME_OR_PASSWORD(0x86),
    NOT_AUTHORIZED(MqttCommonReasonCode.NOT_AUTHORIZED),
    SERVER_UNAVAILABLE(0x88),
    SERVER_BUSY(MqttCommonReasonCode.SERVER_BUSY),
    BANNED(0x8A),
    BAD_AUTHENTICATION_METHOD(MqttCommonReasonCode.BAD_AUTHENTICATION_METHOD),
    TOPIC_NAME_INVALID(MqttCommonReasonCode.TOPIC_NAME_INVALID),
    PACKET_TOO_LARGE(MqttCommonReasonCode.PACKET_TOO_LARGE),
    QUOTA_EXCEEDED(MqttCommonReasonCode.QUOTA_EXCEEDED),
    PAYLOAD_FORMAT_INVALID(MqttCommonReasonCode.PAYLOAD_FORMAT_INVALID),
    RETAIN_NOT_SUPPORTED(MqttCommonReasonCode.RETAIN_NOT_SUPPORTED),
    QOS_NOT_SUPPORTED(MqttCommonReasonCode.QOS_NOT_SUPPORTED),
    USE_ANOTHER_SERVER(MqttCommonReasonCode.USE_ANOTHER_SERVER),
    SERVER_MOVED(MqttCommonReasonCode.SERVER_MOVED),
    CONNECTION_RATE_EXCEEDED(MqttCommonReasonCode.CONNECTION_RATE_EXCEEDED);

    private final int code;
    private final @NotNull ConnackReasonCode connackReasonCode;
    private final @NotNull DisconnectedReasonCode disconnectedReasonCode;

    Mqtt5ConnAckReasonCode(final int code) {
        this.code = code;
        connackReasonCode = ConnackReasonCode.valueOf(name());
        disconnectedReasonCode = DisconnectedReasonCode.valueOf(name());
    }

    Mqtt5ConnAckReasonCode(final @NotNull MqttCommonReasonCode reasonCode) {
        this(reasonCode.getCode());
    }

    @Override
    public int getCode() {
        return code;
    }

    public @NotNull ConnackReasonCode toConnackReasonCode() {
        return connackReasonCode;
    }

    public @NotNull DisconnectedReasonCode toDisconnectedReasonCode() {
        return disconnectedReasonCode;
    }

    private static final int ERROR_CODE_MIN = UNSPECIFIED_ERROR.code;
    private static final int ERROR_CODE_MAX = CONNECTION_RATE_EXCEEDED.code;
    private static final @Nullable Mqtt5ConnAckReasonCode @NotNull [] ERROR_CODE_LOOKUP =
            new Mqtt5ConnAckReasonCode[ERROR_CODE_MAX - ERROR_CODE_MIN + 1];

    private static final @NotNull Mqtt5ConnAckReasonCode @NotNull [] CONNACK_LOOKUP =
            new Mqtt5ConnAckReasonCode[ConnackReasonCode.values().length];
    private static final @Nullable Mqtt5ConnAckReasonCode @NotNull [] DISCONNECTED_LOOKUP =
            new Mqtt5ConnAckReasonCode[DisconnectedReasonCode.values().length];

    static {
        for (final Mqtt5ConnAckReasonCode reasonCode : values()) {
            if (reasonCode != SUCCESS) {
                ERROR_CODE_LOOKUP[reasonCode.code - ERROR_CODE_MIN] = reasonCode;
            }
            CONNACK_LOOKUP[reasonCode.connackReasonCode.ordinal()] = reasonCode;
            DISCONNECTED_LOOKUP[reasonCode.disconnectedReasonCode.ordinal()] = reasonCode;
        }
    }

    /**
     * Returns the CONNACK Reason Code belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the CONNACK Reason Code belonging to the given byte code or <code>null</code> if the byte code is not a
     * valid CONNACK Reason Code code.
     */
    public static @Nullable Mqtt5ConnAckReasonCode fromCode(final int code) {
        if (code == SUCCESS.code) {
            return SUCCESS;
        }
        if (code < ERROR_CODE_MIN || code > ERROR_CODE_MAX) {
            return null;
        }
        return ERROR_CODE_LOOKUP[code - ERROR_CODE_MIN];
    }

    public static @NotNull Mqtt5ConnAckReasonCode from(final @NotNull ConnackReasonCode reasonCode) {
        return CONNACK_LOOKUP[reasonCode.ordinal()];
    }

    public static @Nullable Mqtt5ConnAckReasonCode from(final @NotNull DisconnectedReasonCode reasonCode) {
        return DISCONNECTED_LOOKUP[reasonCode.ordinal()];
    }

    @NotNull
    public static Mqtt5ConnAckReasonCode fromReturnCode(@NotNull final Mqtt3ConnAckReturnCode returnCode) {
        switch (returnCode) {
            case ACCEPTED:
                return Mqtt5ConnAckReasonCode.SUCCESS;
            case REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                return Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION;
            case REFUSED_IDENTIFIER_REJECTED:
                return Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID;
            case REFUSED_SERVER_UNAVAILABLE:
                return Mqtt5ConnAckReasonCode.SERVER_UNAVAILABLE;
            case REFUSED_BAD_USERNAME_OR_PASSWORD:
                return Mqtt5ConnAckReasonCode.BAD_USER_NAME_OR_PASSWORD;
            case REFUSED_NOT_AUTHORIZED:
                return Mqtt5ConnAckReasonCode.NOT_AUTHORIZED;
            default:
                throw new IllegalStateException();
        }
    }

    @NotNull
    public static Mqtt5ConnAckReasonCode fromDisconnectReasonCode(@NotNull final DisconnectReasonCode disconnectReasonCode) {
        switch (disconnectReasonCode) {
            case NORMAL_DISCONNECTION:
            case DISCONNECT_WITH_WILL_MESSAGE:
            case KEEP_ALIVE_TIMEOUT:
            case UNSPECIFIED_ERROR:
            case SESSION_TAKEN_OVER:
            case RECEIVE_MAXIMUM_EXCEEDED:
            case TOPIC_ALIAS_INVALID:
            case MESSAGE_RATE_TOO_HIGH:
            case ADMINISTRATIVE_ACTION:
            case SHARED_SUBSCRIPTION_NOT_SUPPORTED:
            case MAXIMUM_CONNECT_TIME:
            case SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED:
            case WILDCARD_SUBSCRIPTION_NOT_SUPPORTED:
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
            case SERVER_SHUTTING_DOWN:
                return Mqtt5ConnAckReasonCode.SERVER_UNAVAILABLE;
            case BAD_AUTHENTICATION_METHOD:
                return Mqtt5ConnAckReasonCode.BAD_AUTHENTICATION_METHOD;
            case CLIENT_IDENTIFIER_NOT_VALID:
                return Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID;
            case TOPIC_FILTER_INVALID:
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

    @NotNull
    public static Mqtt5ConnAckReasonCode fromAckReasonCode(@NotNull final AckReasonCode ackReasonCode) {

        switch (ackReasonCode) {
            case SUCCESS:
                return Mqtt5ConnAckReasonCode.SUCCESS;
            case NO_MATCHING_SUBSCRIBERS:
            case PACKET_IDENTIFIER_IN_USE:
            case UNSPECIFIED_ERROR:
                return Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR;
            case IMPLEMENTATION_SPECIFIC_ERROR:
                return Mqtt5ConnAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR;
            case NOT_AUTHORIZED:
                return Mqtt5ConnAckReasonCode.NOT_AUTHORIZED;
            case TOPIC_NAME_INVALID:
                return Mqtt5ConnAckReasonCode.TOPIC_NAME_INVALID;
            case QUOTA_EXCEEDED:
                return Mqtt5ConnAckReasonCode.QUOTA_EXCEEDED;
            case PAYLOAD_FORMAT_INVALID:
                return Mqtt5ConnAckReasonCode.PAYLOAD_FORMAT_INVALID;
            default:
                return Mqtt5ConnAckReasonCode.UNSPECIFIED_ERROR;
        }
    }

}
