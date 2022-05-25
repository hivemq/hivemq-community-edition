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
package com.hivemq.mqtt.message.mqtt5;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * Retain Handling according to the MQTT 5 specification.
 *
 * @author Silvio Giebl
 * @author Florian Limpöck
 *
 * @since 4.0.0
 */
public enum Mqtt5RetainHandling {

    SEND(0),
    SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST(1),
    DO_NOT_SEND(2);

    private static final @NotNull Mqtt5RetainHandling @NotNull [] VALUES = values();

    final int code;

    Mqtt5RetainHandling(final int code) {
        this.code = code;
    }

    /**
     * @return the byte code of this Retain Handling.
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the Retain Handling belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the Retain Handling belonging to the byte code or null if the byte code is not a valid Retain Handling.
     */
    @Nullable
    public static Mqtt5RetainHandling fromCode(final int code) {
        return (code >= 0 && code < VALUES.length) ? VALUES[code] : null;
    }

}