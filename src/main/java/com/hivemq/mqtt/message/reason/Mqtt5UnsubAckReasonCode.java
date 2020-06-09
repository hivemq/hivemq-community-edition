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
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackReasonCode;

/**
 * MQTT Reason Codes that can be used in UNSUBACK packets according to the MQTT 5 specification.
 *
 * @author Silvio Giebl
 */
public enum Mqtt5UnsubAckReasonCode implements Mqtt5ReasonCode {

    SUCCESS(MqttCommonReasonCode.SUCCESS),
    NO_SUBSCRIPTIONS_EXISTED(0x11),
    UNSPECIFIED_ERROR(MqttCommonReasonCode.UNSPECIFIED_ERROR),
    IMPLEMENTATION_SPECIFIC_ERROR(MqttCommonReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
    NOT_AUTHORIZED(MqttCommonReasonCode.NOT_AUTHORIZED),
    TOPIC_FILTER_INVALID(MqttCommonReasonCode.TOPIC_FILTER_INVALID),
    PACKET_IDENTIFIER_IN_USE(MqttCommonReasonCode.PACKET_IDENTIFIER_IN_USE);

    private static final @NotNull Mqtt5UnsubAckReasonCode[] VALUES = values();

    private final int code;
    private final @NotNull UnsubackReasonCode unsubackReasonCode;

    Mqtt5UnsubAckReasonCode(final int code) {
        this.code = code;
        unsubackReasonCode = UnsubackReasonCode.valueOf(name());
    }

    Mqtt5UnsubAckReasonCode(final @NotNull MqttCommonReasonCode reasonCode) {
        this(reasonCode.getCode());
    }

    @Override
    public int getCode() {
        return code;
    }

    public @NotNull UnsubackReasonCode toUnsubackReasonCode() {
        return unsubackReasonCode;
    }

    private static final @NotNull Mqtt5UnsubAckReasonCode @NotNull [] UNSUBACK_LOOKUP =
            new Mqtt5UnsubAckReasonCode[UnsubackReasonCode.values().length];

    static {
        for (final Mqtt5UnsubAckReasonCode reasonCode : values()) {
            UNSUBACK_LOOKUP[reasonCode.unsubackReasonCode.ordinal()] = reasonCode;
        }
    }

    /**
     * Returns the UNSUBACK Reason Code belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the UNSUBACK Reason Code belonging to the given byte code or <code>null</code> if the byte code is not a
     * valid UNSUBACK Reason Code code.
     */
    public static @Nullable Mqtt5UnsubAckReasonCode fromCode(final int code) {
        for (final Mqtt5UnsubAckReasonCode reasonCode : VALUES) {
            if (reasonCode.code == code) {
                return reasonCode;
            }
        }
        return null;
    }

    public static @NotNull Mqtt5UnsubAckReasonCode from(final @NotNull UnsubackReasonCode reasonCode) {
        return UNSUBACK_LOOKUP[reasonCode.ordinal()];
    }
}
