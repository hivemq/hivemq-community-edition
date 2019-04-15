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
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;

/**
 * MQTT Reason Codes that can be used in pubrec packets according to the MQTT 5 specification.
 *
 * @author Silvio Giebl
 */
public enum Mqtt5PubRecReasonCode implements Mqtt5ReasonCode {

    SUCCESS(MqttCommonReasonCode.SUCCESS),
    NO_MATCHING_SUBSCRIBERS(MqttCommonReasonCode.NO_MATCHING_SUBSCRIBERS),
    UNSPECIFIED_ERROR(MqttCommonReasonCode.UNSPECIFIED_ERROR),
    IMPLEMENTATION_SPECIFIC_ERROR(MqttCommonReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
    NOT_AUTHORIZED(MqttCommonReasonCode.NOT_AUTHORIZED),
    TOPIC_NAME_INVALID(MqttCommonReasonCode.TOPIC_NAME_INVALID),
    PACKET_IDENTIFIER_IN_USE(MqttCommonReasonCode.PACKET_IDENTIFIER_IN_USE),
    QUOTA_EXCEEDED(MqttCommonReasonCode.QUOTA_EXCEEDED),
    PAYLOAD_FORMAT_INVALID(MqttCommonReasonCode.PAYLOAD_FORMAT_INVALID);

    private final int code;

    Mqtt5PubRecReasonCode(final int code) {
        this.code = code;
    }

    Mqtt5PubRecReasonCode(@NotNull final MqttCommonReasonCode reasonCode) {
        this(reasonCode.getCode());
    }

    /**
     * @return the byte code of this pubrec Reason Code.
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the pubrec Reason Code belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the pubrec Reason Code belonging to the given byte code or null if the byte code is not a valid pubrec
     * Reason Code code.
     */
    @Nullable
    public static Mqtt5PubRecReasonCode fromCode(final int code) {
        for (final Mqtt5PubRecReasonCode reasonCode : values()) {
            if (reasonCode.code == code) {
                return reasonCode;
            }
        }
        return null;
    }

    @NotNull
    public static Mqtt5PubRecReasonCode fromAckReasonCode(@NotNull final AckReasonCode reasonCode) {
        switch (reasonCode) {
            case SUCCESS:
                return Mqtt5PubRecReasonCode.SUCCESS;
            case NO_MATCHING_SUBSCRIBERS:
                return Mqtt5PubRecReasonCode.NO_MATCHING_SUBSCRIBERS;
            case UNSPECIFIED_ERROR:
                return Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR;
            case IMPLEMENTATION_SPECIFIC_ERROR:
                return Mqtt5PubRecReasonCode.IMPLEMENTATION_SPECIFIC_ERROR;
            case NOT_AUTHORIZED:
                return Mqtt5PubRecReasonCode.NOT_AUTHORIZED;
            case TOPIC_NAME_INVALID:
                return Mqtt5PubRecReasonCode.TOPIC_NAME_INVALID;
            case PACKET_IDENTIFIER_IN_USE:
                return Mqtt5PubRecReasonCode.PACKET_IDENTIFIER_IN_USE;
            case QUOTA_EXCEEDED:
                return Mqtt5PubRecReasonCode.QUOTA_EXCEEDED;
            case PAYLOAD_FORMAT_INVALID:
                return Mqtt5PubRecReasonCode.PAYLOAD_FORMAT_INVALID;
            default:
                throw new IllegalArgumentException("Unknown AckReason code");
        }
    }

}
