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

package com.hivemq.mqtt.message.reason;

import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;

/**
 * MQTT Reason Codes that can be used in DISCONNECT packets according to the MQTT 5 specification.
 *
 * @author Silvio Giebl
 */
public enum Mqtt5DisconnectReasonCode implements Mqtt5ReasonCode {

    NORMAL_DISCONNECTION(0x00),
    @Deprecated DISCONNECT_WITH_WILL_MESSAGE(0x04),
    UNSPECIFIED_ERROR(MqttCommonReasonCode.UNSPECIFIED_ERROR),
    MALFORMED_PACKET(MqttCommonReasonCode.MALFORMED_PACKET),
    PROTOCOL_ERROR(MqttCommonReasonCode.PROTOCOL_ERROR),
    IMPLEMENTATION_SPECIFIC_ERROR(MqttCommonReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
    NOT_AUTHORIZED(MqttCommonReasonCode.NOT_AUTHORIZED),
    SERVER_BUSY(MqttCommonReasonCode.SERVER_BUSY),
    SERVER_SHUTTING_DOWN(0x8B),
    @Deprecated BAD_AUTHENTICATION_METHOD(MqttCommonReasonCode.BAD_AUTHENTICATION_METHOD),
    KEEP_ALIVE_TIMEOUT(0x8D),
    SESSION_TAKEN_OVER(0x8E),
    TOPIC_FILTER_INVALID(MqttCommonReasonCode.TOPIC_FILTER_INVALID),
    TOPIC_NAME_INVALID(MqttCommonReasonCode.TOPIC_NAME_INVALID),
    RECEIVE_MAXIMUM_EXCEEDED(0x93),
    TOPIC_ALIAS_INVALID(0x94),
    PACKET_TOO_LARGE(MqttCommonReasonCode.PACKET_TOO_LARGE),
    MESSAGE_RATE_TOO_HIGH(0x96),
    QUOTA_EXCEEDED(MqttCommonReasonCode.QUOTA_EXCEEDED),
    ADMINISTRATIVE_ACTION(0x98),
    PAYLOAD_FORMAT_INVALID(MqttCommonReasonCode.PAYLOAD_FORMAT_INVALID),
    RETAIN_NOT_SUPPORTED(MqttCommonReasonCode.RETAIN_NOT_SUPPORTED),
    QOS_NOT_SUPPORTED(MqttCommonReasonCode.QOS_NOT_SUPPORTED),
    USE_ANOTHER_SERVER(MqttCommonReasonCode.USE_ANOTHER_SERVER),
    SERVER_MOVED(MqttCommonReasonCode.SERVER_MOVED),
    SHARED_SUBSCRIPTION_NOT_SUPPORTED(MqttCommonReasonCode.SHARED_SUBSCRIPTION_NOT_SUPPORTED),
    CONNECTION_RATE_EXCEEDED(MqttCommonReasonCode.CONNECTION_RATE_EXCEEDED),
    MAXIMUM_CONNECT_TIME(0xA0),
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED(MqttCommonReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED),
    WILDCARD_SUBSCRIPTION_NOT_SUPPORTED(MqttCommonReasonCode.WILDCARD_SUBSCRIPTION_NOT_SUPPORTED);

    private final int code;

    Mqtt5DisconnectReasonCode(final int code) {
        this.code = code;
    }

    Mqtt5DisconnectReasonCode(@NotNull final MqttCommonReasonCode reasonCode) {
        this(reasonCode.getCode());
    }

    /**
     * @return the byte code of this DISCONNECT Reason Code.
     */
    public int getCode() {
        return code;
    }


    private static final int ERROR_CODE_MIN = UNSPECIFIED_ERROR.code;
    private static final int ERROR_CODE_MAX = WILDCARD_SUBSCRIPTION_NOT_SUPPORTED.code;
    private static final Mqtt5DisconnectReasonCode[] ERROR_CODE_LOOKUP =
            new Mqtt5DisconnectReasonCode[ERROR_CODE_MAX - ERROR_CODE_MIN + 1];

    static {
        for (final Mqtt5DisconnectReasonCode reasonCode : values()) {
            if (reasonCode != NORMAL_DISCONNECTION && reasonCode != DISCONNECT_WITH_WILL_MESSAGE) {
                ERROR_CODE_LOOKUP[reasonCode.code - ERROR_CODE_MIN] = reasonCode;
            }
        }
    }

    /**
     * Returns the DISCONNECT Reason Code belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the DISCONNECT Reason Code belonging to the given byte code or null if the byte code is not a valid
     * DISCONNECT Reason Code code.
     */
    @Nullable
    public static Mqtt5DisconnectReasonCode fromCode(final int code) {
        if (code == NORMAL_DISCONNECTION.code) {
            return NORMAL_DISCONNECTION;
        }
        if (code == DISCONNECT_WITH_WILL_MESSAGE.code) {
            return DISCONNECT_WITH_WILL_MESSAGE;
        }
        if (code < ERROR_CODE_MIN || code > ERROR_CODE_MAX) {
            return null;
        }
        return ERROR_CODE_LOOKUP[code - ERROR_CODE_MIN];
    }

    /**
     * Returns the DISCONNECT Reason Code belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the DISCONNECT Reason Code belonging to the given byte code or null if the byte code is not a valid
     * DISCONNECT Reason Code code.
     */
    @NotNull
    public static DisconnectReasonCode toPluginCode(final @NotNull Mqtt5DisconnectReasonCode code) {
        return DisconnectReasonCode.valueOf(code.name());
    }

    /**
     * Returns the DISCONNECT Reason Code belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the DISCONNECT Reason Code belonging to the given byte code or null if the byte code is not a valid
     * DISCONNECT Reason Code code.
     */
    @NotNull
    public static DisconnectedReasonCode toPluginDisconnectedCode(final @NotNull Mqtt5DisconnectReasonCode code) {
        return DisconnectedReasonCode.valueOf(code.name());
    }

}
