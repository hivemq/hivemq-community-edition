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

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.buffer.ByteBuf;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Dominik Obermaier
 */
public class Strings {

    private Strings() {
        //This is a utility class, don't instantiate it!
    }

    /**
     * Returns the value of a prefixed UTF-8 String from a {@link io.netty.buffer.ByteBuf}
     * according to the MQTT spec. The UTF-8 String is prefixed with a 16-bit value which
     * indicates the actual length of the String.
     * <p>
     * <b>This method will read from the {@link io.netty.buffer.ByteBuf} and will change the reader
     * index!</b>
     *
     * @param buf the {@link io.netty.buffer.ByteBuf} to read from
     * @return The UTF-8 String or <code>null</code> if there aren't enough bytes to read. This can happen if this method
     * can not read the prefixed size of the String or if there are less bytes to read available than
     * indicated by the prefixed 16-bit length.
     * @throws java.lang.NullPointerException if the passed {@link io.netty.buffer.ByteBuf} is <code>null</code>
     */
    public static String getPrefixedString(final ByteBuf buf) {
        checkNotNull(buf);
        if (buf.readableBytes() < 2) {
            return null;
        }

        final int utf8StringLength = buf.readUnsignedShort();

        if (buf.readableBytes() < utf8StringLength) {
            return null;
        }

        return getPrefixedString(buf, utf8StringLength);
    }

    public static String getPrefixedString(final ByteBuf buf, final int utf8StringLength) {
        checkNotNull(buf);
        final String string = buf.toString(buf.readerIndex(), utf8StringLength, UTF_8);
        //The ByteBuf.toString method, doesn't move the read index, therefor we have to do this manually.
        buf.skipBytes(utf8StringLength);
        return string;
    }

    public static String getValidatedPrefixedString(@NotNull final ByteBuf buf, final int utf8StringLength, final boolean validateShouldNotCharacters) {
        checkNotNull(buf);

        if (buf.readableBytes() < utf8StringLength) {
            return null;
        }

        final byte[] bytes = new byte[utf8StringLength];

        buf.getBytes(buf.readerIndex(), bytes);

        if (Utf8Utils.containsMustNotCharacters(bytes)) {
            return null;
        }

        if (validateShouldNotCharacters && Utf8Utils.hasControlOrNonCharacter(bytes)) {
            return null;
        }
        //The ByteBuf.getBytes method, doesn't move the read index, therefor we have to do this manually.
        buf.skipBytes(utf8StringLength);
        return new String(bytes, UTF_8);
    }

    /**
     * Writes a String onto a {@link io.netty.buffer.ByteBuf}. This encodes the
     * String according to the MQTT spc. The string gets prefixed with a 16-bit value
     * which indicates the actual length of the string
     *
     * @param string the string to encode
     * @param buffer the byte buffer
     * @return the encoded string as {@link io.netty.buffer.ByteBuf}
     */
    public static ByteBuf createPrefixedBytesFromString(final String string, final ByteBuf buffer) {
        checkNotNull(string);
        checkNotNull(buffer);

        if (Utf8Utils.stringIsOneByteCharsOnly(string)) {
            // In case ther is no character in the string that is encoded with more than one byte in UTF-8,
            // We can write the string character by character without copying it to a temporary byte array.
            buffer.writeShort(string.length());
            for (int i = 0; i < string.length(); i++) {
                buffer.writeByte(string.charAt(i));
            }
        } else {
            final byte[] bytes = string.getBytes(UTF_8);
            buffer.writeShort(bytes.length);
            buffer.writeBytes(bytes);
        }


        return buffer;
    }

    /**
     * <p>This method can be used to convert a long value into a human readable byte format</p>
     *
     * <p>1024 bytes = 1.00 KB</p>
     * <p>1024*1024 bytes = 1.00 MB</p>
     * <p>1024*1024*1024 bytes = 1.00 GB</p>
     * <p>1024*1024*1024*1024 bytes = 1.00 TB</p>
     *
     * @param bytes the long value to convert
     * @return the human readable converted String
     */
    @VisibleForTesting
    public static String convertBytes(final long bytes) {
        final long kbDivisor = 1024L;
        final long mbDivisor = kbDivisor * kbDivisor;
        final long gbDivisor = mbDivisor * kbDivisor;
        final long tbDivisor = gbDivisor * kbDivisor;

        if (bytes <= kbDivisor) {
            return bytes + " B";
        } else if (bytes <= mbDivisor) {
            final double kb = (double) bytes / kbDivisor;
            return String.format(Locale.US, "%.2f", kb) + " KB";
        } else if (bytes <= gbDivisor) {
            final double mb = (double) bytes / mbDivisor;
            return String.format(Locale.US, "%.2f", mb) + " MB";
        } else if (bytes <= tbDivisor) {
            final double gb = (double) bytes / gbDivisor;
            return String.format(Locale.US, "%.2f", gb) + " GB";
        } else {
            final double tb = (double) bytes / tbDivisor;
            return String.format(Locale.US, "%.2f", tb) + " TB";
        }
    }
}
