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

import com.google.common.base.Charsets;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.QoS;
import io.netty.buffer.ByteBuf;

/**
 * Util for decoders of MQTT 5 messages.
 *
 * @author Silvio Giebl
 */
class Mqtt5MessageEncoderUtil {

    private Mqtt5MessageEncoderUtil() {
    }

    static int propertyEncodedLength(@NotNull final String string) {
        return 1 + MqttBinaryData.encodedLength(string);
    }

    static int nullablePropertyEncodedLength(@Nullable final String string) {
        return (string == null) ? 0 : propertyEncodedLength(string);
    }

    static int nullablePropertyEncodedLength(@Nullable final byte[] binary) {
        return (binary == null) ? 0 : 1 + MqttBinaryData.encodedLength(binary);
    }

    static int nullablePropertyEncodedLength(@Nullable final Mqtt5PayloadFormatIndicator payloadFormatIndicator) {
        return (payloadFormatIndicator == null) ? 0 : 2;
    }

    static int nullablePropertyEncodedLength(@Nullable final QoS qoS) {
        return (qoS != null && qoS != QoS.EXACTLY_ONCE) ? 2 : 0;
    }

    static int booleanPropertyEncodedLength(final boolean value, final boolean defaultValue) {
        return (value == defaultValue) ? 0 : 2;
    }

    static int shortPropertyEncodedLength(final int value, final int defaultValue) {
        return (value == defaultValue) ? 0 : 3;
    }

    static int intPropertyEncodedLength(final long value, final long defaultValue) {
        return (value == defaultValue) ? 0 : 5;
    }

    static int variableByteIntegerPropertyEncodedLength(final int value) {
        return 1 + MqttVariableByteInteger.encodedLength(value);
    }

    static void encodeProperty(final int propertyIdentifier, @NotNull final String string, @NotNull final ByteBuf out) {

        out.writeByte(propertyIdentifier);
        MqttBinaryData.encode(string.getBytes(Charsets.UTF_8), out);
    }

    static void encodeNullableProperty(final int propertyIdentifier, @Nullable final String string, @NotNull final ByteBuf out) {

        if (string != null) {
            encodeProperty(propertyIdentifier, string, out);
        }
    }

    static void encodeNullableProperty(final int propertyIdentifier, @Nullable final byte[] binary, @NotNull final ByteBuf out) {

        if (binary != null) {
            out.writeByte(propertyIdentifier);
            MqttBinaryData.encode(binary, out);
        }
    }

    static void encodeNullableProperty(final int propertyIdentifier, @Nullable final QoS qoS, @NotNull final ByteBuf out) {

        if ((qoS != null) && (qoS.getQosNumber() < 2)) {
            out.writeByte(propertyIdentifier);
            out.writeByte(qoS.getQosNumber());
        }
    }

    static void encodeNullableProperty(final int propertyIdentifier, @Nullable final Mqtt5PayloadFormatIndicator payloadFormatIndicator,
                                       @NotNull final ByteBuf out) {

        if (payloadFormatIndicator != null) {
            out.writeByte(propertyIdentifier);
            out.writeByte(payloadFormatIndicator.getCode());
        }
    }

    static void encodeBooleanProperty(final int propertyIdentifier, final boolean value, final boolean defaultValue, @NotNull final ByteBuf out) {

        if (value != defaultValue) {
            out.writeByte(propertyIdentifier);
            out.writeByte(value ? 1 : 0);
        }
    }

    static void encodeShortProperty(final int propertyIdentifier, final int value, final int defaultValue, @NotNull final ByteBuf out) {

        if (value != defaultValue) {
            out.writeByte(propertyIdentifier);
            out.writeShort(value);
        }
    }

    static void encodeIntProperty(final int propertyIdentifier, final long value, final long defaultValue, @NotNull final ByteBuf out) {

        if (value != defaultValue) {
            out.writeByte(propertyIdentifier);
            out.writeInt((int) value);
        }
    }

    static void encodeVariableByteIntegerProperty(final int propertyIdentifier, final int value, @NotNull final ByteBuf out) {

        out.writeByte(propertyIdentifier);
        MqttVariableByteInteger.encode(value, out);
    }

}
