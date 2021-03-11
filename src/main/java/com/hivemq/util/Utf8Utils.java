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
import com.google.common.base.Utf8;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.buffer.ByteBuf;

/**
 * @author Lukas Brandl
 * @author Florian Limpöck
 */
public class Utf8Utils {

    /**
     * This method checks if all the characters in a string can be encode with one byte in UTF-8.
     * It is used check if a string can be written onto a buffer character by character, without actually encoding it.
     *
     * @param sequence The character sequence that should be checked
     * @return true if each character in the sequence can be encoded with one byte in UTF-8
     */
    public static boolean stringIsOneByteCharsOnly(final CharSequence sequence) {
        for (int i = 0, len = sequence.length(); i < len; i++) {
            final char ch = sequence.charAt(i);
            if (ch > 0x7F) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the length in bytes, that a string would have when encoded in utf-8.
     *
     * @param sequence The character sequence for which the length should be calculated
     * @return the length in utf-8 bytes
     */
    public static int encodedLength(final CharSequence sequence) {
        int count = 0;
        for (int i = 0, len = sequence.length(); i < len; i++) {
            final char c = sequence.charAt(i);
            if (c <= 0x7F) {
                count++;
            } else if (c <= 0x7FF) {
                count += 2;
            } else if (Character.isHighSurrogate(c)) {
                count += 4;
                ++i;
            } else {
                count += 3;
            }
        }
        return count;
    }

    /**
     * Checks if a <byte[]> contains a control character or a non character.
     *
     * @param bytes The bytes to test.
     * @return true if the array contains a control character or a non character, else false.
     */
    public static boolean hasControlOrNonCharacter(final byte @NotNull [] bytes) {
        Preconditions.checkNotNull(bytes);

        for (int i = 0; i < bytes.length; i++) {

            final byte byte1 = bytes[i];

            //control byte1s
            if (byte1 >= 1 && byte1 <= 31 || byte1 == (byte) 0x7F) {
                return true;
            }

            if (byte1 > 31) {
                continue;
            }

            if (byte1 < (byte) 0xE0) {
                // Two-byte form
                if (i == bytes.length - 1) {
                    return false;
                }

                if (byte1 == (byte) 0xC2 && bytes[i + 1] <= (byte) 0x9F) {
                    return true;
                }

            } else if (byte1 < (byte) 0xF0) {
                // Three-byte form.
                if (i == bytes.length - 2) {
                    continue;
                }

//              '\uFDD0' - '\uFDEF'
                if (byte1 == (byte) 0xEF && bytes[i + 1] == (byte) 0xB7 && (bytes[i + 2] >= (byte) 0x90 || bytes[i + 2] <= (byte) 0xAF)) {
                    return true;
                }

//              '\uFFFE' | '\uFFFF'
                if (byte1 == (byte) 0xEF && bytes[i + 1] == (byte) 0xBF && (bytes[i + 2] == (byte) 0xBE || bytes[i + 2] == (byte) 0xBF)) {
                    return true;
                }
            } else {
                // Four-byte form
                if (i == bytes.length - 3) {
                    continue;
                }

                if (byte1 > (byte) 0xF4) {
                    continue;
                }

                final byte byte2 = bytes[i + 1];
                final byte byte3 = bytes[i + 2];
                final byte byte4 = bytes[i + 3];

                if (!(byte3 == (byte) 0xBF && (byte4 == (byte) 0xBE || byte4 == (byte) 0xBF))) {
                    continue;
                }

//              U+1FFFE|F - U10FFFE|F
                if (byte1 == (byte) 0xF0) {
                    if (byte2 == (byte) 0x9F || byte2 == (byte) 0xAF || byte2 == (byte) 0xBF) {
                        return true;
                    }
                } else {
                    if (byte2 == (byte) 0x8F || byte2 == (byte) 0x9F || byte2 == (byte) 0xAF || byte2 == (byte) 0xBF) {
                        return true;
                    }
                }

            }

        }

        return false;
    }

    /**
     * Checks if a String contains a control character or a non character.
     *
     * @param text The string to test
     * @return true if the text contains a control character or a non character else false
     */
    public static boolean hasControlOrNonCharacter(final @NotNull String text) {
        Preconditions.checkNotNull(text);

        for (int i = 0; i < text.length(); i++) {

            final char character = text.charAt(i);

            //control characters
            if (character >= '\u0001' && character <= '\u001F' || character >= '\u007F' && character <= '\u009F') {
                return true;
            }

            //non characters
            if (character >= '\uFDD0' && character <= '\uFDEF' || character == '\uFFFE' || character == '\uFFFF') {
                return true;
            }

            if (i == text.length() - 1) {
                return false;
            }

            final char next = text.charAt(i + 1);

            if (character == '\uD83F' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uD87F' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uD8BF' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uD8FF' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uD93F' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uD97F' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uD9BF' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uD9FF' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uDA3F' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uDA7F' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uDABF' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uDAFF' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uDB3F' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uDB7F' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uDBBF' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }
            if (character == '\uDBFF' && (next == '\uDFFE' || next == '\uDFFF')) {
                return true;
            }

        }

        return false;
    }

    /**
     * ByteBuf implementation of guavas Utf8.isWellFormed(final byte[] bytes)
     */
    public static boolean isWellFormed(final @NotNull ByteBuf byteBuf, final int utf8StringLength) {

        Preconditions.checkNotNull(byteBuf);

        byteBuf.markReaderIndex();

        final boolean wellFormed = isSliceWellFormed(byteBuf.slice(byteBuf.readerIndex(), utf8StringLength), utf8StringLength);

        byteBuf.resetReaderIndex();

        return wellFormed;

    }

    private static boolean isSliceWellFormed(final ByteBuf byteBuf, final int len) {

        Preconditions.checkPositionIndexes(0, len, byteBuf.readableBytes());
        for (int i = 0; i < len; ++i) {
            byteBuf.markReaderIndex();
            if (byteBuf.readByte() < 0) {
                byteBuf.resetReaderIndex();
                return isWellFormedSlowPath(byteBuf);
            }
        }

        return true;

    }

    private static boolean isWellFormedSlowPath(final ByteBuf byteBuf) {

        while (true) {
            byte byte1;
            do {
                if (byteBuf.readableBytes() == 0) {
                    return true;
                }
            } while ((byte1 = byteBuf.readByte()) >= 0);

            if (byte1 < -32) {
                if (byteBuf.readableBytes() == 0) {
                    return false;
                }

                if (byte1 < -62 || byteBuf.readByte() > -65) {
                    return false;
                }
            } else {
                final byte byte2;
                if (byte1 < -16) {
                    if (byteBuf.readableBytes() < 2) {
                        return false;
                    }

                    byte2 = byteBuf.readByte();
                    if (byte2 > -65 || byte1 == -32 && byte2 < -96 || byte1 == -19 && -96 <= byte2 || byteBuf.readByte() > -65) {
                        return false;
                    }
                } else {
                    if (byteBuf.readableBytes() < 3) {
                        return false;
                    }

                    byte2 = byteBuf.readByte();
                    if (byte2 > -65 || (byte1 << 28) + (byte2 - -112) >> 30 != 0 || byteBuf.readByte() > -65 || byteBuf.readByte() > -65) {
                        return false;
                    }
                }
            }
        }
    }

    /**
     * Checks whether the given UTF-8 encoded byte array contains characters a UTF-8 encoded String must not according
     * to the MQTT 5 specification.
     * <p>
     * These characters are the null character U+0000 and UTF-16 surrogates.
     *
     * @param binary the UTF-8 encoded byte array.
     * @return whether the binary data contains characters a UTF-8 encoded String must not.
     */
    public static boolean containsMustNotCharacters(@NotNull final byte[] binary) {
        if (!Utf8.isWellFormed(binary)) {
            return true;
        }
        for (final byte b : binary) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given UTF-16 encoded Java string contains characters a UTF-8 encoded String must not according
     * to the MQTT 5 specification.
     * <p>
     * These characters are the null character U+0000 and UTF-16 surrogates.
     *
     * @param string the UTF-16 encoded Java string
     * @return whether the string contains characters a UTF-8 encoded String must not.
     */
    public static boolean containsMustNotCharacters(@NotNull final String string) {
        boolean highSurrogate = false;
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (c == 0) {
                return true;
            }
            if (highSurrogate == !Character.isLowSurrogate(c)) {
                return true;
            }
            highSurrogate = Character.isHighSurrogate(c);
        }
        return highSurrogate;
    }
}
