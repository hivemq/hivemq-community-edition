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
package com.hivemq.util;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dominik Obermaier
 */
public class Bytes {

    private Bytes() {
        //This is a utility class, don't instantiate it!
    }

    /**
     * Tests for a given Integer if the bit in a specific position is set.
     */
    public static boolean isBitSet(final byte number, final int bitPosition) {
        checkArgument(bitPosition < 8);
        checkArgument(bitPosition >= 0);
        return (number & (1 << bitPosition)) != 0;
    }

    /**
     * Sets/Unsets a bit at the specified position
     */
    public static byte setBit(final byte number, final int bitPosition, final boolean value) {
        checkArgument(bitPosition < 8);
        checkArgument(bitPosition >= 0);
        if (value) {
            return setBit(number, bitPosition);
        }
        return unsetBit(number, bitPosition);
    }

    /**
     * Sets a bit at the specified position
     */
    public static byte setBit(final byte number, final int bitPosition) {
        checkArgument(bitPosition < 8);
        checkArgument(bitPosition >= 0);
        return (byte) (number | (1 << bitPosition));
    }

    /**
     * Unsets a bit at the specified position
     */
    public static byte unsetBit(final byte number, final int bitPosition) {
        checkArgument(bitPosition < 8);
        checkArgument(bitPosition >= 0);
        return (byte) (number & ~(1 << bitPosition));
    }

    /**
     * Returns the value of a prefixed byte array from a {@link io.netty.buffer.ByteBuf}
     * according to the MQTT spec. The byte array is prefixed with a 16-bit value which
     * indicates the actual size of the byte array.
     * <p>
     * <p>
     * <b>This method will read from the {@link io.netty.buffer.ByteBuf} and will change the reader
     * index!</b>
     *
     * @param buf the {@link io.netty.buffer.ByteBuf} to read from
     * @return The byte array or <code>null</code> if there aren't enough bytes to read. This can happen if this method
     * can not read the size of the byte array or if there are less bytes to read available than
     * indicated by the prefixed 16-bit length.
     * @throws java.lang.NullPointerException if the passed {@link io.netty.buffer.ByteBuf} is <code>null</code>
     */
    public static byte[] getPrefixedBytes(final ByteBuf buf) {
        checkNotNull(buf);
        if (buf.readableBytes() < 2) {
            return null;
        }
        final int byteLength = buf.readUnsignedShort();

        if (buf.readableBytes() < byteLength) {
            return null;
        }

        final byte[] rawBytes = new byte[byteLength];
        buf.readBytes(rawBytes);

        return rawBytes;
    }


    /**
     * Prefixes a byte array with the length of the bytes according to the MQTT specification.
     *
     * @param bytes the byte array to prefix
     * @return a {@link io.netty.buffer.ByteBuf} which is prefixed with the length of the given bytes
     * @throws java.lang.NullPointerException if the passed byte[] is <code>null</code>
     */
    public static ByteBuf prefixBytes(final byte[] bytes) {
        checkNotNull(bytes);

        final ByteBuf buf = Unpooled.buffer();
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);

        return buf;
    }

    /**
     * Prefixes a byte array with the length of the bytes according to the MQTT specification.
     *
     * @param bytes  the byte array to prefix
     * @param buffer the buffer that the bytes are written onto
     * @return a {@link io.netty.buffer.ByteBuf} which is prefixed with the length of the given bytes
     * @throws java.lang.NullPointerException if the passed byte[] is <code>null</code>
     */
    public static ByteBuf prefixBytes(final byte[] bytes, final ByteBuf buffer) {
        checkNotNull(bytes);
        checkNotNull(buffer);

        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);

        return buffer;
    }

    /**
     * Read a serialized long value for a given byte array
     *
     * @param buffer        A byte array that contains the serialized long value to read.
     * @param startPosition The position of the first bit of the long value in the buffer
     * @return The next 8 bits from startPosition as long
     * @throws IllegalArgumentException if the buffer is to small to read a long value from the start position
     */
    public static long readLong(final byte[] buffer, final int startPosition) {
        if (startPosition + Long.BYTES > buffer.length) {
            throw new IllegalArgumentException("The provided array[" + buffer.length + "] is to small to read 8 bytes from start position " + startPosition);
        }
        return Longs.fromBytes(buffer[startPosition], buffer[startPosition + 1], buffer[startPosition + 2], buffer[startPosition + 3],
                buffer[startPosition + 4], buffer[startPosition + 5], buffer[startPosition + 6], buffer[startPosition + 7]);
    }

    /**
     * Read a serialized int value for a given byte array
     *
     * @param buffer        A byte array that contains the serialized int value to read.
     * @param startPosition The position of the first bit of the int value in the buffer
     * @return The next 4 bits from startPosition as int
     * @throws IllegalArgumentException if the buffer is to small to read a int value from the start position
     */
    public static int readInt(final byte[] buffer, final int startPosition) {
        if (startPosition + Integer.BYTES > buffer.length) {
            throw new IllegalArgumentException("The provided array[" + buffer.length + "] is to small to read 4 bytes from start position " + startPosition);
        }
        return Ints.fromBytes(buffer[startPosition], buffer[startPosition + 1], buffer[startPosition + 2], buffer[startPosition + 3]);
    }

    /**
     * Read a serialized unsigned short for a given byte array
     *
     * @param buffer        A byte array that contains the serialized unsigned short value to read.
     * @param startPosition The position of the first bit of the unsigned short value in the buffer
     * @return The next 2 bits from startPosition as int
     * @throws IllegalArgumentException if the buffer is to small to read a int value from the start position
     */
    public static int readUnsignedShort(final byte[] buffer, final int startPosition) {
        if (startPosition + Short.BYTES > buffer.length) {
            throw new IllegalArgumentException("The provided array[" + buffer.length + "] is to small to read 2 bytes from start position " + startPosition);
        }
        return Ints.fromBytes((byte) 0, (byte) 0, buffer[startPosition], buffer[startPosition + 1]);
    }

    /**
     * Copies an int into an existing byte array.
     *
     * @param anInt  an int.
     * @param bytes  the destination byte array. Its length must be at least prefix + 4.
     * @param prefix the position in the destination byte array, where to start.
     */
    public static void copyIntToByteArray(final int anInt, final byte[] bytes, final int prefix) {
        checkNotNull(bytes, "bytes must not be null");
        checkArgument(bytes.length >= prefix + Integer.BYTES, "bytes needs to be at least prefix + 4 in length");
        checkArgument(prefix >= 0, "prefix can't be less than 0");

        bytes[prefix] = (byte) (anInt >> 24);
        bytes[prefix + 1] = (byte) (anInt >> 16);
        bytes[prefix + 2] = (byte) (anInt >> 8);
        bytes[prefix + 3] = (byte) anInt;
    }

    /**
     * Copies a long into an existing byte array.
     *
     * @param aLong  a long.
     * @param bytes  the destination byte array. Its length must be at least prefix + 8.
     * @param prefix the position in the destination byte array, where to start.
     */
    public static void copyLongToByteArray(final long aLong, final byte[] bytes, final int prefix) {
        checkNotNull(bytes, "bytes must not be null");
        checkArgument(bytes.length >= prefix + Long.BYTES, "bytes needs to be at least prefix + 8 in length");
        checkArgument(prefix >= 0, "prefix can't be less than 0");

        bytes[prefix] = (byte) (aLong >> 56);
        bytes[prefix + 1] = (byte) (aLong >> 48);
        bytes[prefix + 2] = (byte) (aLong >> 40);
        bytes[prefix + 3] = (byte) (aLong >> 32);
        bytes[prefix + 4] = (byte) (aLong >> 24);
        bytes[prefix + 5] = (byte) (aLong >> 16);
        bytes[prefix + 6] = (byte) (aLong >> 8);
        bytes[prefix + 7] = (byte) aLong;
    }

    /**
     * Copies a long into an existing byte array.
     *
     * @param unsignedShort a integer within the 0 and 65535
     * @param bytes         the destination byte array. Its length must be at least prefix + 8.
     * @param prefix        the position in the destination byte array, where to start.
     */
    public static void copyUnsignedShortToByteArray(final int unsignedShort, final byte[] bytes, final int prefix) {
        checkNotNull(bytes, "bytes must not be null");
        checkArgument(bytes.length >= prefix + Short.BYTES, "bytes needs to be at least prefix + 8 in length");
        checkArgument(prefix >= 0, "prefix can't be less than 0");
        checkArgument(unsignedShort <= 65535, "value must be less than 65535");
        checkArgument(unsignedShort >= 0, "value must be more than 0");

        bytes[prefix] = (byte) (unsignedShort >> 8);
        bytes[prefix + 1] = (byte) unsignedShort;
    }

    /**
     * @param optional of a {@link ByteBuffer}
     * @return the bytes of an {@link Optional} of a {@link ByteBuffer} as byte array or null if the optional is not present
     */
    @Nullable
    public static byte[] getBytesFromReadOnlyBuffer(@NotNull final Optional<ByteBuffer> optional) {
        Preconditions.checkNotNull(optional, "optional must never be null");
        return optional.map(Bytes::fromReadOnlyBuffer).orElse(null);
    }

    @Nullable
    public static byte[] fromReadOnlyBuffer(final @Nullable ByteBuffer byteBuffer){
        if(byteBuffer == null){
            return null;
        }
        final ByteBuffer rewind = byteBuffer.asReadOnlyBuffer().rewind();
        final byte[] array = new byte[rewind.remaining()];
        rewind.get(array);
        return array;
    }
}
