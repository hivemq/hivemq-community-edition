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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StringsTest {

    private ByteBuf buffer;

    @Before
    public void setUp() throws Exception {

        buffer = Unpooled.buffer();

    }

    @Test
    public void test_get_prefixed_bytes() throws Exception {
        final byte[] bytes = "test".getBytes(UTF_8);
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);

        assertEquals("test", Strings.getPrefixedString(buffer));
    }

    @Test
    public void test_get_prefixed_random_data() throws Exception {
        final String randomString = RandomStringUtils.random(5000);
        final byte[] bytes = randomString.getBytes(UTF_8);
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);

        assertEquals(randomString, Strings.getPrefixedString(buffer));
    }

    @Test
    public void test_get_prefixed_bytes_buffer_not_filled() throws Exception {
        buffer.writeShort(10);
        buffer.writeBytes(RandomUtils.nextBytes(9));

        assertNull(Strings.getPrefixedString(buffer));
    }

    @Test
    public void test_get_prefixed_string_empty() throws Exception {

        assertNull(Strings.getPrefixedString(buffer));
    }

    @Test
    public void test_get_prefixed_string_empty_byte_array() throws Exception {
        buffer.writeShort(0);

        assertEquals(0, Strings.getPrefixedString(buffer).length());
    }

    @Test
    public void test_get_prefixed_string() throws Exception {

        assertNull(Strings.getPrefixedString(buffer));
    }

    @Test(expected = NullPointerException.class)
    public void test_get_prefixed_bytes_null_passed() throws Exception {

        Strings.getPrefixedString(null);
    }

    @Test
    public void test_create_prefixed_bytes_empty() throws Exception {
        final ByteBuf buf = Strings.createPrefixedBytesFromString("", Unpooled.buffer());

        assertEquals(0, buf.readShort());
        assertEquals(false, buf.isReadable());
    }

    @Test
    public void test_create_prefixed_bytes() throws Exception {
        final String input = "thisisatést";
        final ByteBuf buf = Strings.createPrefixedBytesFromString(input, Unpooled.buffer());

        System.out.println(buf);
        assertEquals(12, buf.readShort());
        buf.readBytes(12);
        assertEquals(false, buf.isReadable());
    }

    @Test
    public void test_create_prefixed_max_length() throws Exception {
        final String randomString = RandomStringUtils.randomAscii(Short.MAX_VALUE);
        final ByteBuf buf = Strings.createPrefixedBytesFromString(randomString, Unpooled.buffer());

        assertEquals(Short.MAX_VALUE, buf.readShort());
        buf.readBytes(Short.MAX_VALUE);
        assertEquals(false, buf.isReadable());
    }

    @Test(expected = NullPointerException.class)
    public void test_create_prefixed_bytes_null_bytes() throws Exception {
        Strings.createPrefixedBytesFromString(null, buffer);
    }

    @Test(expected = NullPointerException.class)
    public void test_create_prefixed_bytes_null_buffer() throws Exception {
        Strings.createPrefixedBytesFromString("string", null);
    }
}