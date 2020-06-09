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
import com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode;

/**
 * MQTT Reason Codes that can be used in AUTH packets according to the MQTT 5 specification.
 *
 * @author Silvio Giebl
 */
public enum Mqtt5AuthReasonCode implements Mqtt5ReasonCode {

    SUCCESS(MqttCommonReasonCode.SUCCESS),
    CONTINUE_AUTHENTICATION(0x18),
    REAUTHENTICATE(0x19);

    private final int code;
    private final @NotNull AuthReasonCode authReasonCode;

    Mqtt5AuthReasonCode(final int code) {
        this.code = code;
        authReasonCode = AuthReasonCode.valueOf(name());
    }

    Mqtt5AuthReasonCode(final @NotNull MqttCommonReasonCode reasonCode) {
        this(reasonCode.getCode());
    }

    @Override
    public int getCode() {
        return code;
    }

    public @NotNull AuthReasonCode toAuthReasonCode() {
        return authReasonCode;
    }

    private static final @NotNull Mqtt5AuthReasonCode @NotNull [] AUTH_LOOKUP =
            new Mqtt5AuthReasonCode[AuthReasonCode.values().length];

    static {
        for (final Mqtt5AuthReasonCode reasonCode : values()) {
            AUTH_LOOKUP[reasonCode.authReasonCode.ordinal()] = reasonCode;
        }
    }

    /**
     * Returns the AUTH Reason Code belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the AUTH Reason Code belonging to the given byte code or <code>null</code> if the byte code is not a
     * valid AUTH Reason Code.
     */
    public static @Nullable Mqtt5AuthReasonCode fromCode(final int code) {
        if (code == SUCCESS.code) {
            return SUCCESS;
        } else if (code == CONTINUE_AUTHENTICATION.code) {
            return CONTINUE_AUTHENTICATION;
        } else if (code == REAUTHENTICATE.code) {
            return REAUTHENTICATE;
        }
        return null;
    }

    public static @NotNull Mqtt5AuthReasonCode from(final @NotNull AuthReasonCode reasonCode) {
        return AUTH_LOOKUP[reasonCode.ordinal()];
    }

    @Override
    public boolean canBeSentByServer() {
        return this != REAUTHENTICATE;
    }

    @Override
    public boolean canBeSentByClient() {
        return this != SUCCESS;
    }
}
