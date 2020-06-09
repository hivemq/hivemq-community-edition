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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Silvio Giebl
 * @author Lukas Brandl
 */
public class MqttBinaryDataTest {
    private final Random random = new Random();

    @Test
    public void test_decodeBinaryData_zero_length() {
        final ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(0).writeByte(0);
        final byte[] expected = {};
        final byte[] actual = MqttBinaryData.decode(byteBuf);
        assertArrayEquals(expected, actual);
        byteBuf.release();
    }

    @Test
    public void test_decodeBinaryData_full_length() {
        final ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(0xFF).writeByte(0xFF);
        final byte[] expected = new byte[65_535];
        byteBuf.writeBytes(expected);
        final byte[] actual = MqttBinaryData.decode(byteBuf);
        assertArrayEquals(expected, actual);
        byteBuf.release();
    }

    @Test
    public void test_decodeBinaryData_not_enough_bytes_for_length() {
        final ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(0);
        assertNull(MqttBinaryData.decode(byteBuf));
    }

    @Test
    public void test_decodeBinaryData_not_enough_bytes_for_value() {
        final ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(0).writeByte(2).writeByte(1);
        assertNull(MqttBinaryData.decode(byteBuf));
    }

    @Test
    public void test_encodeBinaryData_zero_length() {
        final ByteBuf byteBuf = Unpooled.buffer();
        final byte[] binary = {};
        MqttBinaryData.encode(binary, byteBuf);
        assertEquals(0, byteBuf.readUnsignedByte());
        assertEquals(0, byteBuf.readUnsignedByte());
        assertFalse(byteBuf.isReadable());
        byteBuf.release();
    }

    @Test
    public void test_encodeBinaryData_random_full_length() {
        final ByteBuf byteBuf = Unpooled.buffer();
        final byte[] binary = new byte[65_535];
        random.nextBytes(binary);
        MqttBinaryData.encode(binary, byteBuf);
        assertEquals(0xFF, byteBuf.readUnsignedByte());
        assertEquals(0xFF, byteBuf.readUnsignedByte());
        for (final byte b : binary) {
            assertEquals(b, byteBuf.readByte());
        }
        assertFalse(byteBuf.isReadable());
        byteBuf.release();
    }

    @Test
    public void test_isInBinaryDataLength() {
        final byte[] binary = new byte[65_535];
        random.nextBytes(binary);
        assertTrue(MqttBinaryData.isInRange(binary));
        final byte[] binary2 = new byte[65_536];
        random.nextBytes(binary2);
        assertFalse(MqttBinaryData.isInRange(binary2));
    }

    @Test
    public void test_encodedBinaryDataLength() {
        final byte[] binary = new byte[random.nextInt(65_535)];
        random.nextBytes(binary);
        assertEquals(2 + binary.length, MqttBinaryData.encodedLength(binary));
    }
}