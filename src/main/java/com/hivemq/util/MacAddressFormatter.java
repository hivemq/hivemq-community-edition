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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A formatter for MAC Addresses.
 * Formats the addresses in a format like 'C8-29-0A-52-62-66'.
 *
 * @author Dominik Obermaier
 */
public class MacAddressFormatter {

    private static final int LENGTH = 6;

    /**
     * Formats the addresses in a format like 'C8-29-0A-52-62-66'.
     * <p>
     * So every MAC address consists of uppercase letters and dashes between each byte.
     *
     * @param hardwareAddress the hardware address as byte array.
     * @return the formatted MAC address
     * @throws NullPointerException     if you pass <code>null</code>
     * @throws IllegalArgumentException if the byte array is not exactly 6 bytes long
     */
    public static String format(final byte[] hardwareAddress) {

        checkNotNull(hardwareAddress);
        checkArgument(hardwareAddress.length == LENGTH, "Hardware address must be of length %s but was %s", LENGTH, hardwareAddress.length);

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hardwareAddress.length; i++) {
            sb.append(String.format("%02X%s", hardwareAddress[i], (i < hardwareAddress.length - 1) ? "-" : ""));
        }
        return sb.toString();
    }
}
