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
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * Util for encoders of MQTT messages for different versions.
 *
 * @author Silvio Giebl
 */
public class MqttMessageEncoderUtil {

    private MqttMessageEncoderUtil() {
    }

    /**
     * Calculates the encoded length of a MQTT message with the given remaining length.
     *
     * @param remainingLength the remaining length of the MQTT message.
     * @return the encoded length of the MQTT message.
     */
    public static int encodedPacketLength(final int remainingLength) {
        return 1 + encodedLengthWithHeader(remainingLength);
    }

    /**
     * Calculates the encoded length with a prefixed header.
     *
     * @param encodedLength the encoded length.
     * @return the encoded length with a prefixed header.
     */
    public static int encodedLengthWithHeader(final int encodedLength) {
        return MqttVariableByteInteger.encodedLength(encodedLength) + encodedLength;
    }

    public static int nullableEncodedLength(@Nullable final String string) {
        return (string == null) ? 0 : MqttBinaryData.encodedLength(string);
    }

    public static int nullableEncodedLength(@Nullable final ByteBuffer byteBuffer) {
        return (byteBuffer == null) ? 0 : MqttBinaryData.encodedLength(byteBuffer);
    }

    public static int encodedOrEmptyLength(@Nullable final ByteBuffer byteBuffer) {
        return (byteBuffer == null) ? MqttBinaryData.EMPTY_LENGTH : MqttBinaryData.encodedLength(byteBuffer);
    }

    public static void encodeNullable(@Nullable final String string, @NotNull final ByteBuf out) {
        if (string != null) {
            MqttBinaryData.encode(string, out);
        }
    }

    public static void encodeNullable(@Nullable final ByteBuffer byteBuffer, @NotNull final ByteBuf out) {
        if (byteBuffer != null) {
            MqttBinaryData.encode(byteBuffer, out);
        }
    }

    public static void encodeOrEmpty(@Nullable final ByteBuffer byteBuffer, @NotNull final ByteBuf out) {
        if (byteBuffer != null) {
            MqttBinaryData.encode(byteBuffer, out);
        } else {
            MqttBinaryData.encodeEmpty(out);
        }
    }

}
