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
package com.hivemq.persistence.local.xodus;

import com.google.common.base.Utf8;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.Bytes;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;

import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Various utilities for dealing with Xodus. This util class
 * mainly contains handy conversion methods
 *
 * @author Dominik Obermaier
 */
public class XodusUtils {

    private XodusUtils() {
        //Utility class, don't instantiate
    }

    /**
     * Converts a UTF-8 String to a ByteIterable
     *
     * @param string a UTF-8 String
     * @return a ByteIterable
     */
    @NotNull
    public static ByteIterable stringToByteIterable(@NotNull final String string) {
        checkNotNull(string, "String must not be null");

        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        return new ArrayByteIterable(bytes);
    }

    /**
     * Converts a byte array to a ByteIterable.
     *
     * @param bytes the byte array
     * @return a byte iterable, backed by the byte array
     */
    @NotNull
    public static ByteIterable bytesToByteIterable(@NotNull final byte[] bytes) {
        checkNotNull(bytes, "bytes must not be null");

        return new ArrayByteIterable(bytes);
    }

    /**
     * Converts a ByteIterable to a String, encoded as UTF-8
     *
     * @param byteIterable a ByteIterable
     * @return an UTF-8 String
     */
    @NotNull
    public static String byteIterableToString(@NotNull final ByteIterable byteIterable) {
        checkNotNull(byteIterable, "ByteIterable must not be null");

        return new String(byteIterable.getBytesUnsafe(), 0, byteIterable.getLength(), StandardCharsets.UTF_8);
    }

    /**
     * Converts a ByteIterable to a byte array.
     *
     * @param byteIterable the ByteIterable to convert
     * @return a byte array
     */
    @NotNull
    public static byte[] byteIterableToBytes(@NotNull final ByteIterable byteIterable) {
        checkNotNull(byteIterable, "ByteIterable must not be null");

        final byte[] unsafeBytes = byteIterable.getBytesUnsafe();
        if (unsafeBytes.length == byteIterable.getLength()) {
            //Awesome, we don't need to copy the byte array
            return unsafeBytes;
        }
        //We need to copy the array to an array with the exact size :(
        final byte[] bytes = new byte[byteIterable.getLength()];
        System.arraycopy(unsafeBytes, 0, bytes, 0, bytes.length);

        return bytes;
    }

    public static int serializeByte(final byte b, @NotNull final byte[] serialized, final int offset) {
        serialized[offset] = b;
        return offset + 1;
    }

    public static int serializeShort(final int s, @NotNull final byte[] serialized, final int offset) {
        Bytes.copyUnsignedShortToByteArray(s, serialized, offset);
        return offset + Short.BYTES;
    }

    public static int serializeLong(final long l, @NotNull final byte[] serialized, final int offset) {
        Bytes.copyLongToByteArray(l, serialized, offset);
        return offset + Long.BYTES;
    }

    public static int shortLengthStringSize(@Nullable final String string) {
        return Short.BYTES + ((string == null) ? 0 : Utf8.encodedLength(string));
    }

    public static int shortLengthArraySize(@Nullable final byte[] bytes) {
        return Short.BYTES + ((bytes == null) ? 0 : bytes.length);
    }

    public static int serializeShortLengthString(@Nullable final String string, @NotNull final byte[] serialized, final int offset) {
        return serializeShortLengthArray((string == null) ? null : string.getBytes(StandardCharsets.UTF_8), serialized, offset);
    }

    public static int serializeShortLengthArray(@Nullable final byte[] bytes, @NotNull final byte[] serialized, int offset) {
        final int length = (bytes == null) ? 0 : bytes.length;
        Bytes.copyUnsignedShortToByteArray(length, serialized, offset);
        offset += Short.BYTES;
        if (length == 0) {
            return offset;
        }
        System.arraycopy(bytes, 0, serialized, offset, length);
        return offset + length;
    }

}
