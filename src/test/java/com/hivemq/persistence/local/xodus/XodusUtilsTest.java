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

import com.google.common.collect.Lists;
import jetbrains.exodus.ByteIterable;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static com.hivemq.persistence.local.xodus.XodusUtils.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Dominik Obermaier
 */
public class XodusUtilsTest {

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_byteiterable_to_string_conversion() throws Exception {
        final String[] input = new String[]{"string", RandomStringUtils.randomAlphabetic(65535),
                "Я Б Г Д Ж Й", "Ł Ą Ż Ę Ć Ń Ś Ź", "てすと", "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃ ",
                "０１２３４５６７８９", "ａｂｃｄｅｆｇｈｉ", "ＡＢＣＤＥＦＧＨＩ", "Iñtërnâtiônàlizætiøn",
                "⡍⠜⠇⠑⠹ ⠺⠁⠎ ⠙⠑⠁⠙⠒ ⠞⠕ ⠃⠑⠛⠔ ⠺⠊⠹⠲ ⡹⠻⠑ ⠊⠎ ⠝⠕ ⠙⠳⠃⠞", "i ♥ u",
                "\uD843\uDED7 \uD843\uDEF9 \uD843\uDEFA \uD843\uDF2D",
                "\uD843\uDF2E \uD843\uDF4C \uD843\uDFB4 \uD843\uDFBC \uD843\uDFEA \uD844\uDC5C",
                "ვეპხის ტყაოსანი", "ฉันกินกระจกไ", "Կրնամ", "सकता", "\uD834\uDE00\uD834\uDE10",
                "\uD83D\uDE1A", "\uD83D\uDEAA", "\uD83C\uDD84", "\uD80C\uDCE6", "\uD804\uDD3D",
                "\uDB40\uDC50", "\uDB40\uDFFC", "\uD83E\uDE13", "\uD800\uDF17"};

        for (final String string : input) {

            final ByteIterable bytes = stringToByteIterable(string);
            final String result = byteIterableToString(bytes);

            assertEquals(string, result);
        }
    }

    @Test
    public void test_byteiterable_to_bytes_conversion() throws Exception {
        final List<byte[]> bytes = Lists.newArrayList(RandomUtils.nextBytes(1), RandomUtils.nextBytes(1024), RandomUtils.nextBytes(65535), RandomUtils.nextBytes(1024 * 1024 * 10));

        for (final byte[] input : bytes) {
            final ByteIterable byteIterable = bytesToByteIterable(input);
            final byte[] result = byteIterableToBytes(byteIterable);

            assertArrayEquals(input, result);
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_string_to_byte_iterable_NPE() throws Exception {
        stringToByteIterable(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_bytes_to_byte_iterable_NPE() throws Exception {
        bytesToByteIterable(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_byte_iterable_to_string_NPE() throws Exception {
        byteIterableToString(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_byte_iterable_to_bytes_NPE() throws Exception {
        byteIterableToBytes(null);
    }

}