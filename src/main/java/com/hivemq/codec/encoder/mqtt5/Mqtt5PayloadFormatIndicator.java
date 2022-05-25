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
package com.hivemq.codec.encoder.mqtt5;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;

/**
 * Payload Format Indicator according to the MQTT 5 specification.
 *
 * @author Silvio Giebl
 */
public enum Mqtt5PayloadFormatIndicator {

    UNSPECIFIED,
    UTF_8;

    private static final @NotNull Mqtt5PayloadFormatIndicator @NotNull [] VALUES = values();

    private final @NotNull PayloadFormatIndicator payloadFormatIndicator;

    Mqtt5PayloadFormatIndicator() {
        payloadFormatIndicator = PayloadFormatIndicator.valueOf(name());
    }

    /**
     * @return the byte code of this Payload Format Indicator.
     */
    public int getCode() {
        return ordinal();
    }

    public @NotNull PayloadFormatIndicator toPayloadFormatIndicator() {
        return payloadFormatIndicator;
    }

    private static final @NotNull Mqtt5PayloadFormatIndicator @NotNull [] LOOKUP =
            new Mqtt5PayloadFormatIndicator[PayloadFormatIndicator.values().length];

    static {
        for (final Mqtt5PayloadFormatIndicator payloadFormatIndicator : values()) {
            LOOKUP[payloadFormatIndicator.payloadFormatIndicator.ordinal()] = payloadFormatIndicator;
        }
    }

    /**
     * Returns the Payload Format Indicator belonging to the given byte code.
     *
     * @param code the byte code.
     * @return the Payload Format Indicator belonging to the byte code or null if the byte code is not a valid Payload
     * Format Indicator.
     */
    public static @Nullable Mqtt5PayloadFormatIndicator fromCode(final int code) {
        return (code >= 0 && code < VALUES.length) ? VALUES[code] : null;
    }

    public static @NotNull Mqtt5PayloadFormatIndicator from(
            final @NotNull PayloadFormatIndicator payloadFormatIndicator) {

        return LOOKUP[payloadFormatIndicator.ordinal()];
    }
}
