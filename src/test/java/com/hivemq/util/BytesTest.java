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

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BytesTest {


    @Test
    public void test_get_prefixed_bytes() throws Exception {
        final ByteBuf buffer = Unpooled.buffer();
        final byte[] bytes = "test".getBytes(UTF_8);
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);

        final byte[] prefixedBytes = Bytes.getPrefixedBytes(buffer);

        assertEquals(bytes.length, prefixedBytes.length);
    }

    @Test
    public void test_get_prefixed_random_data() throws Exception {
        final ByteBuf buffer = Unpooled.buffer();
        final byte[] bytes = RandomUtils.nextBytes(5000);
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);

        final byte[] prefixedBytes = Bytes.getPrefixedBytes(buffer);

        assertEquals(bytes.length, prefixedBytes.length);
    }

    @Test
    public void test_get_prefixed_bytes_buffer_not_filled() throws Exception {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(10);
        buffer.writeBytes(RandomUtils.nextBytes(9));

        final byte[] prefixedBytes = Bytes.getPrefixedBytes(buffer);

        assertNull(prefixedBytes);
    }

    @Test
    public void test_get_prefixed_bytes_empty() throws Exception {
        final ByteBuf buffer = Unpooled.buffer();

        final byte[] prefixedBytes = Bytes.getPrefixedBytes(buffer);

        assertNull(prefixedBytes);
    }

    @Test
    public void test_get_prefixed_bytes_empty_byte_array() throws Exception {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(0);

        final byte[] prefixedBytes = Bytes.getPrefixedBytes(buffer);

        assertEquals(0, prefixedBytes.length);
    }

    @Test(expected = NullPointerException.class)
    public void test_get_prefixed_bytes_null_passed() throws Exception {

        Bytes.getPrefixedBytes(null);
    }

    @Test
    public void test_prefix_bytes() throws Exception {
        final ByteBuf byteBuf = Bytes.prefixBytes(new byte[]{0, 1, (byte) 128}, Unpooled.buffer());
        final int size = byteBuf.readUnsignedShort();

        assertEquals(3, size);
        assertEquals(0, byteBuf.readByte());
        assertEquals(1, byteBuf.readByte());
        assertEquals((byte) 128, byteBuf.readByte());
        assertEquals(false, byteBuf.isReadable());
    }


    @Test
    public void test_prefix_bytes_empty() throws Exception {
        final ByteBuf byteBuf = Bytes.prefixBytes(new byte[0], Unpooled.buffer());
        final int size = byteBuf.readUnsignedShort();

        assertEquals(0, size);
        assertEquals(false, byteBuf.isReadable());
    }

    @Test(expected = NullPointerException.class)
    public void test_prefixed_bytes_null_passed() throws Exception {

        Bytes.prefixBytes(null, Unpooled.buffer());
    }

    @Test(expected = NullPointerException.class)
    public void test_prefixed_bytes_null_buffer() throws Exception {

        Bytes.prefixBytes(new byte[]{1, 2, 3}, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_is_bit_setinvalid_argument_too_high() throws Exception {
        Bytes.isBitSet((byte) 0, 8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_is_bit_setinvalid_argument_too_small() throws Exception {
        Bytes.isBitSet((byte) 0, -1);
    }

    @Test
    public void test_no_bit_set() throws Exception {

        final byte b = 0b0000_0000;

        for (int i = 0; i < 8; i++) {
            assertEquals(false, Bytes.isBitSet(b, i));
        }
    }

    @Test
    public void test_all_bits_set() throws Exception {

        final byte b = (byte) 0b1111_1111;

        for (int i = 0; i < 8; i++) {
            assertEquals(true, Bytes.isBitSet(b, i));
        }
    }

    @Test
    public void test_bit_1_set() throws Exception {

        final byte b = (byte) 0b0000_0001;

        assertEquals(true, Bytes.isBitSet(b, 0));
        assertEquals(false, Bytes.isBitSet(b, 1));
        assertEquals(false, Bytes.isBitSet(b, 2));
        assertEquals(false, Bytes.isBitSet(b, 3));
        assertEquals(false, Bytes.isBitSet(b, 4));
        assertEquals(false, Bytes.isBitSet(b, 5));
        assertEquals(false, Bytes.isBitSet(b, 6));
        assertEquals(false, Bytes.isBitSet(b, 7));
    }

    @Test
    public void test_bit_2_set() throws Exception {

        final byte b = (byte) 0b0000_0010;

        assertEquals(false, Bytes.isBitSet(b, 0));
        assertEquals(true, Bytes.isBitSet(b, 1));
        assertEquals(false, Bytes.isBitSet(b, 2));
        assertEquals(false, Bytes.isBitSet(b, 3));
        assertEquals(false, Bytes.isBitSet(b, 4));
        assertEquals(false, Bytes.isBitSet(b, 5));
        assertEquals(false, Bytes.isBitSet(b, 6));
        assertEquals(false, Bytes.isBitSet(b, 7));
    }

    @Test
    public void test_bit_3_set() throws Exception {

        final byte b = (byte) 0b0000_0100;

        assertEquals(false, Bytes.isBitSet(b, 0));
        assertEquals(false, Bytes.isBitSet(b, 1));
        assertEquals(true, Bytes.isBitSet(b, 2));
        assertEquals(false, Bytes.isBitSet(b, 3));
        assertEquals(false, Bytes.isBitSet(b, 4));
        assertEquals(false, Bytes.isBitSet(b, 5));
        assertEquals(false, Bytes.isBitSet(b, 6));
        assertEquals(false, Bytes.isBitSet(b, 7));
    }

    @Test
    public void test_bit_4_set() throws Exception {

        final byte b = (byte) 0b0000_1000;

        assertEquals(false, Bytes.isBitSet(b, 0));
        assertEquals(false, Bytes.isBitSet(b, 1));
        assertEquals(false, Bytes.isBitSet(b, 2));
        assertEquals(true, Bytes.isBitSet(b, 3));
        assertEquals(false, Bytes.isBitSet(b, 4));
        assertEquals(false, Bytes.isBitSet(b, 5));
        assertEquals(false, Bytes.isBitSet(b, 6));
        assertEquals(false, Bytes.isBitSet(b, 7));
    }

    @Test
    public void test_bit_5_set() throws Exception {

        final byte b = (byte) 0b0001_0000;

        assertEquals(false, Bytes.isBitSet(b, 0));
        assertEquals(false, Bytes.isBitSet(b, 1));
        assertEquals(false, Bytes.isBitSet(b, 2));
        assertEquals(false, Bytes.isBitSet(b, 3));
        assertEquals(true, Bytes.isBitSet(b, 4));
        assertEquals(false, Bytes.isBitSet(b, 5));
        assertEquals(false, Bytes.isBitSet(b, 6));
        assertEquals(false, Bytes.isBitSet(b, 7));
    }

    @Test
    public void test_bit_6_set() throws Exception {

        final byte b = (byte) 0b0010_0000;

        assertEquals(false, Bytes.isBitSet(b, 0));
        assertEquals(false, Bytes.isBitSet(b, 1));
        assertEquals(false, Bytes.isBitSet(b, 2));
        assertEquals(false, Bytes.isBitSet(b, 3));
        assertEquals(false, Bytes.isBitSet(b, 4));
        assertEquals(true, Bytes.isBitSet(b, 5));
        assertEquals(false, Bytes.isBitSet(b, 6));
        assertEquals(false, Bytes.isBitSet(b, 7));
    }

    @Test
    public void test_bit_7_set() throws Exception {

        final byte b = (byte) 0b0100_0000;

        assertEquals(false, Bytes.isBitSet(b, 0));
        assertEquals(false, Bytes.isBitSet(b, 1));
        assertEquals(false, Bytes.isBitSet(b, 2));
        assertEquals(false, Bytes.isBitSet(b, 3));
        assertEquals(false, Bytes.isBitSet(b, 4));
        assertEquals(false, Bytes.isBitSet(b, 5));
        assertEquals(true, Bytes.isBitSet(b, 6));
        assertEquals(false, Bytes.isBitSet(b, 7));
    }

    @Test
    public void test_bit_8_set() throws Exception {

        final byte b = (byte) 0b1000_0000;

        assertEquals(false, Bytes.isBitSet(b, 0));
        assertEquals(false, Bytes.isBitSet(b, 1));
        assertEquals(false, Bytes.isBitSet(b, 2));
        assertEquals(false, Bytes.isBitSet(b, 3));
        assertEquals(false, Bytes.isBitSet(b, 4));
        assertEquals(false, Bytes.isBitSet(b, 5));
        assertEquals(false, Bytes.isBitSet(b, 6));
        assertEquals(true, Bytes.isBitSet(b, 7));
    }

    @Test
    public void test_some_bits_set() throws Exception {

        final byte b = (byte) 0b1010_1010;

        assertEquals(false, Bytes.isBitSet(b, 0));
        assertEquals(true, Bytes.isBitSet(b, 1));
        assertEquals(false, Bytes.isBitSet(b, 2));
        assertEquals(true, Bytes.isBitSet(b, 3));
        assertEquals(false, Bytes.isBitSet(b, 4));
        assertEquals(true, Bytes.isBitSet(b, 5));
        assertEquals(false, Bytes.isBitSet(b, 6));
        assertEquals(true, Bytes.isBitSet(b, 7));
    }

    @Test
    public void test_read_long() throws Exception {
        assertEquals(10, Bytes.readLong(Longs.toByteArray(10L), 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_read_long_buffer_overflow() throws Exception {
        Bytes.readLong(new byte[10], 3);
    }

    @Test
    public void test_read_int() throws Exception {
        assertEquals(10, Bytes.readInt(Ints.toByteArray(10), 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_read_int_buffer_overflow() throws Exception {
        Bytes.readLong(new byte[6], 3);
    }

    @Test
    public void test_copy_int_to_array() {
        final byte[] bytes = new byte[4];
        Bytes.copyIntToByteArray(Integer.MAX_VALUE, bytes, 0);
        assertEquals(Integer.MAX_VALUE, Bytes.readInt(bytes, 0));

        Bytes.copyIntToByteArray(Integer.MIN_VALUE, bytes, 0);
        assertEquals(Integer.MIN_VALUE, Bytes.readInt(bytes, 0));

        Bytes.copyIntToByteArray(1, bytes, 0);
        assertEquals(1, Bytes.readInt(bytes, 0));

        Bytes.copyIntToByteArray(-1, bytes, 0);
        assertEquals(-1, Bytes.readInt(bytes, 0));

        Bytes.copyIntToByteArray(0, bytes, 0);
        assertEquals(0, Bytes.readInt(bytes, 0));

        final byte[] withOffset = new byte[7];
        Bytes.copyIntToByteArray(Integer.MIN_VALUE, withOffset, 3);
        assertEquals(Integer.MIN_VALUE, Bytes.readInt(withOffset, 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_int_to_array_to_short() {
        final byte[] bytes = new byte[7];
        Bytes.copyIntToByteArray(Integer.MAX_VALUE, bytes, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_int_to_array_offset_negative() {
        final byte[] bytes = new byte[10];
        Bytes.copyIntToByteArray(Integer.MAX_VALUE, bytes, -1);
    }

    @Test(expected = NullPointerException.class)
    public void test_copy_int_to_array_null() {
        Bytes.copyIntToByteArray(Integer.MAX_VALUE, null, 0);
    }

    @Test
    public void test_copy_long_to_byte_array() throws Exception {
        final byte[] bytes = new byte[8];

        Bytes.copyLongToByteArray(Long.MAX_VALUE, bytes, 0);
        assertEquals(Long.MAX_VALUE, Bytes.readLong(bytes, 0));

        Bytes.copyLongToByteArray(Long.MIN_VALUE, bytes, 0);
        assertEquals(Long.MIN_VALUE, Bytes.readLong(bytes, 0));

        Bytes.copyLongToByteArray(0, bytes, 0);
        assertEquals(0, Bytes.readLong(bytes, 0));

        Bytes.copyLongToByteArray(-1, bytes, 0);
        assertEquals(-1, Bytes.readLong(bytes, 0));

        Bytes.copyLongToByteArray(1, bytes, 0);
        assertEquals(1, Bytes.readLong(bytes, 0));

        final byte[] withOffset = new byte[11];
        Bytes.copyLongToByteArray(Long.MIN_VALUE, withOffset, 3);
        assertEquals(Long.MIN_VALUE, Bytes.readLong(withOffset, 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_long_to_array_to_short() {
        final byte[] bytes = new byte[11];
        Bytes.copyLongToByteArray(Long.MAX_VALUE, bytes, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_long_to_array_offset_negative() {
        final byte[] bytes = new byte[10];
        Bytes.copyLongToByteArray(Long.MAX_VALUE, bytes, -1);
    }

    @Test(expected = NullPointerException.class)
    public void test_copy_long_to_array_null() {
        Bytes.copyLongToByteArray(Long.MAX_VALUE, null, 0);
    }

    @Test
    public void test_copy_short_to_array() {
        final byte[] bytes = new byte[2];
        Bytes.copyUnsignedShortToByteArray(65535, bytes, 0);
        assertEquals(65535, Bytes.readUnsignedShort(bytes, 0));

        Bytes.copyUnsignedShortToByteArray(0, bytes, 0);
        assertEquals(0, Bytes.readUnsignedShort(bytes, 0));

        Bytes.copyUnsignedShortToByteArray(1, bytes, 0);
        assertEquals(1, Bytes.readUnsignedShort(bytes, 0));

        Bytes.copyUnsignedShortToByteArray(1, bytes, 0);
        assertEquals(1, Bytes.readUnsignedShort(bytes, 0));

        Bytes.copyUnsignedShortToByteArray(0, bytes, 0);
        assertEquals(0, Bytes.readUnsignedShort(bytes, 0));

        final byte[] withOffset = new byte[7];
        Bytes.copyUnsignedShortToByteArray(65535, withOffset, 5);
        assertEquals(65535, Bytes.readUnsignedShort(withOffset, 5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_short_less_than_zero() {
        final byte[] bytes = new byte[2];
        Bytes.copyUnsignedShortToByteArray(-1, bytes, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_short_more_than_max() {
        final byte[] bytes = new byte[2];
        Bytes.copyUnsignedShortToByteArray(65535 + 1, bytes, 0);
    }

    @Test(expected = NullPointerException.class)
    public void test_copy_short_null() {
        Bytes.copyUnsignedShortToByteArray(65535, null, 0);
    }
}