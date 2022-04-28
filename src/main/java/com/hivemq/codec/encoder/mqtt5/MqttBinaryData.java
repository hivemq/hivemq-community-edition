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
import com.hivemq.util.Utf8Utils;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Utility for decoding, encoding and checking binary data.
 *
 * @author Silvio Giebl
 */
public final class MqttBinaryData {

    private static final int MAX_LENGTH = 65_535;
    private static final int EMPTY_LENGTH = 2;

    private MqttBinaryData() {
    }

    /**
     * Decodes binary data from the given byte buffer at the current reader index.
     *
     * @param byteBuf the byte buffer to decode from.
     * @return the decoded binary data or null if there are not enough bytes in the byte buffer.
     */
    public static byte @Nullable [] decode(final @NotNull ByteBuf byteBuf) {
        if (byteBuf.readableBytes() < EMPTY_LENGTH) {
            return null;
        }
        final int length = byteBuf.readUnsignedShort();
        if (byteBuf.readableBytes() < length) {
            return null;
        }
        final byte[] binary = new byte[length];
        byteBuf.readBytes(binary);

        return binary;
    }

    public static @Nullable String decodeString(final @NotNull ByteBuf byteBuf, final boolean validateUTF8) {
        final byte[] binary = decode(byteBuf);
        if (binary != null && Utf8Utils.containsMustNotCharacters(binary)) {
            return null;
        }
        if (binary != null && validateUTF8 && Utf8Utils.hasControlOrNonCharacter(binary)) {
            return null;
        }
        return (binary == null) ? null : new String(binary, StandardCharsets.UTF_8);
    }

    /**
     * Decodes binary data from the given byte buffer at the current reader index.
     *
     * @param byteBuf the byte buffer to decode from.
     * @param direct  whether the created byte buffer should be direct.
     * @return the decoded binary data or null if there are not enough bytes in the byte buffer.
     */
    public static @Nullable ByteBuffer decode(final @NotNull ByteBuf byteBuf, final boolean direct) {
        if (byteBuf.readableBytes() < EMPTY_LENGTH) {
            return null;
        }
        final int length = byteBuf.readUnsignedShort();
        if (byteBuf.readableBytes() < length) {
            return null;
        }
        final ByteBuffer byteBuffer = allocate(length, direct);
        byteBuf.readBytes(byteBuffer);
        byteBuffer.position(0);
        return byteBuffer;
    }

    /**
     * Encodes the given byte array as binary data to the given byte buffer at the current writer index.
     * <p>
     * This method does not check if the byte array can be encoded as binary data.
     *
     * @param binary  the byte array to encode.
     * @param byteBuf the byte buffer to encode to.
     */
    public static void encode(final byte @NotNull [] binary, final @NotNull ByteBuf byteBuf) {
        byteBuf.writeShort(binary.length);
        byteBuf.writeBytes(binary);
    }

    public static void encode(final @NotNull String value, final @NotNull ByteBuf byteBuf) {
        final byte[] binary = value.getBytes(StandardCharsets.UTF_8);
        encode(binary, byteBuf);
    }

    /**
     * Encodes the given byte buffer as binary data to the given byte buffer at the current writer index.
     * <p>
     * This method does not check if the byte buffer can be encoded as binary data.
     *
     * @param byteBuffer the byte buffer to encode.
     * @param byteBuf    the byte buffer to encode to.
     */
    public static void encode(final @NotNull ByteBuffer byteBuffer, final @NotNull ByteBuf byteBuf) {
        byteBuf.writeShort(byteBuffer.remaining());
        byteBuf.writeBytes(byteBuffer.duplicate());
    }

    /**
     * Encodes a zero length binary data to the given byte buffer at the current writer index.
     *
     * @param byteBuf the byte buffer to encode to.
     */
    public static void encodeEmpty(final @NotNull ByteBuf byteBuf) {
        byteBuf.writeShort(0);
    }

    /**
     * Checks if the given byte array can be encoded as binary data.
     *
     * @param binary the byte array to check.
     * @return whether the byte array can be encoded as binary data.
     */
    public static boolean isInRange(final byte @NotNull [] binary) {
        return binary.length <= MAX_LENGTH;
    }

    /**
     * Checks if the given byte buffer can be encoded as binary data.
     *
     * @param byteBuffer the byte buffer to check.
     * @return whether the byte buffer can be encoded as binary data.
     */
    public static boolean isInRange(final @NotNull ByteBuffer byteBuffer) {
        return byteBuffer.remaining() <= MAX_LENGTH;
    }

    /**
     * Calculates the byte count of the given byte array encoded as binary data.
     * <p>
     * This method does not check if the byte array can be encoded as binary data.
     *
     * @param binary the byte array to calculate the encoded length for.
     * @return the encoded length of the byte array.
     */
    public static int encodedLength(final byte @NotNull [] binary) {
        return EMPTY_LENGTH + binary.length;
    }

    public static int encodedLength(final @NotNull String value) {
        return encodedLength(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Calculates the byte count of the given byte buffer encoded as binary data.
     * <p>
     * This method does not check if the byte buffer can be encoded as binary data.
     *
     * @param byteBuffer the byte buffer to calculate the encoded length for.
     * @return the encoded length of the byte buffer.
     */
    public static int encodedLength(final @NotNull ByteBuffer byteBuffer) {
        return EMPTY_LENGTH + byteBuffer.remaining();
    }

    public static @NotNull ByteBuffer allocate(final int capacity, final boolean direct) {
        return direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
    }

    public static @Nullable ByteBuffer wrap(final byte @Nullable [] binary) {
        return (binary == null) ? null : ByteBuffer.wrap(binary);
    }

    public static @Nullable ByteBuffer slice(final @Nullable ByteBuffer byteBuffer) {
        return (byteBuffer == null) ? null : byteBuffer.slice();
    }

    public static @Nullable ByteBuffer readOnly(final @Nullable ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        final ByteBuffer readOnlyBuffer = byteBuffer.asReadOnlyBuffer();
        readOnlyBuffer.clear();
        return readOnlyBuffer;
    }

    public static byte @NotNull [] getBytes(final @NotNull ByteBuffer byteBuffer) {
        final byte[] binary = new byte[byteBuffer.remaining()];
        byteBuffer.get(binary).position(0);
        return binary;
    }
}
