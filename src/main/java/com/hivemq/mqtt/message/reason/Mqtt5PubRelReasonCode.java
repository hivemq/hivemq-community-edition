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
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelReasonCode;

/**
 * MQTT Reason Codes that can be used in PUBREL packets according to the MQTT 5 specification.
 *
 * @author Silvio Giebl
 */
public enum Mqtt5PubRelReasonCode implements Mqtt5ReasonCode {

    SUCCESS(MqttCommonReasonCode.SUCCESS),
    PACKET_IDENTIFIER_NOT_FOUND(MqttCommonReasonCode.PACKET_IDENTIFIER_NOT_FOUND);

    private final int code;
    private final @NotNull PubrelReasonCode pubrelReasonCode;

    Mqtt5PubRelReasonCode(final int code) {
        this.code = code;
        pubrelReasonCode = PubrelReasonCode.valueOf(name());
    }

    Mqtt5PubRelReasonCode(final @NotNull MqttCommonReasonCode reasonCode) {
        this(reasonCode.getCode());
    }

    @Override
    public int getCode() {
        return code;
    }

    public @NotNull PubrelReasonCode toPubrelReasonCode() {
        return pubrelReasonCode;
    }

    private static final @NotNull Mqtt5PubRelReasonCode @NotNull [] PUBREL_LOOKUP =
            new Mqtt5PubRelReasonCode[PubrelReasonCode.values().length];

    static {
        for (final Mqtt5PubRelReasonCode reasonCode : values()) {
            PUBREL_LOOKUP[reasonCode.pubrelReasonCode.ordinal()] = reasonCode;
        }
    }

    /**
     * Returns the PUBREL Reason Code belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the PUBREL Reason Code belonging to the given byte code or <code>null</code> if the byte code is not a
     * valid PUBREL Reason Code.
     */
    public static @Nullable Mqtt5PubRelReasonCode fromCode(final int code) {
        if (code == SUCCESS.code) {
            return SUCCESS;
        } else if (code == PACKET_IDENTIFIER_NOT_FOUND.code) {
            return PACKET_IDENTIFIER_NOT_FOUND;
        }
        return null;
    }

    public static @NotNull Mqtt5PubRelReasonCode from(final @NotNull PubrelReasonCode reasonCode) {
        return PUBREL_LOOKUP[reasonCode.ordinal()];
    }

    @Override
    public boolean canBeSentByClient() {
        return true;
    }
}
