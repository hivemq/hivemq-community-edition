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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Dominik Obermaier
 */
public class MacAddressFormatterTest {


    @Test
    public void test_format_mac_address() throws Exception {
        final byte[] bytes = new byte[]{-56, 42, 20, 83, 98, 102};

        final String macString = MacAddressFormatter.format(bytes);

        assertEquals("C8-2A-14-53-62-66", macString);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_empty_byte_array() throws Exception {
        MacAddressFormatter.format(new byte[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_too_short_byte_array() throws Exception {
        MacAddressFormatter.format(new byte[5]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_too_long_byte_array() throws Exception {
        MacAddressFormatter.format(new byte[7]);
    }

    @Test(expected = NullPointerException.class)
    public void test_null_passed() throws Exception {
        MacAddressFormatter.format(null);
    }
}
