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

    private static final @NotNull Mqtt5PubRecReasonCode[] VALUES = values();

    private final int code;
    private final @NotNull AckReasonCode ackReasonCode;

    Mqtt5PubRecReasonCode(final int code) {
        this.code = code;
        ackReasonCode = AckReasonCode.valueOf(name());
    }

    Mqtt5PubRecReasonCode(final @NotNull MqttCommonReasonCode reasonCode) {
        this(reasonCode.getCode());
    }

    @Override
    public int getCode() {
        return code;
    }

    public @NotNull AckReasonCode toAckReasonCode() {
        return ackReasonCode;
    }

    private static final @NotNull Mqtt5PubRecReasonCode @NotNull [] ACK_LOOKUP =
            new Mqtt5PubRecReasonCode[AckReasonCode.values().length];

    static {
        for (final Mqtt5PubRecReasonCode reasonCode : VALUES) {
            ACK_LOOKUP[reasonCode.ackReasonCode.ordinal()] = reasonCode;
        }
    }

    /**
     * Returns the PUBREC Reason Code belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the PUBREC Reason Code belonging to the given byte code or <code>null</code> if the byte code is not a
     * valid PUBREC Reason Code.
     */
    public static @Nullable Mqtt5PubRecReasonCode fromCode(final int code) {
        for (final Mqtt5PubRecReasonCode reasonCode : VALUES) {
            if (reasonCode.code == code) {
                return reasonCode;
            }
        }
        return null;
    }

    public static @NotNull Mqtt5PubRecReasonCode from(final @NotNull AckReasonCode reasonCode) {
        return ACK_LOOKUP[reasonCode.ordinal()];
    }

    @Override
    public boolean canBeSentByClient() {
        return this != NO_MATCHING_SUBSCRIBERS;
    }
}
