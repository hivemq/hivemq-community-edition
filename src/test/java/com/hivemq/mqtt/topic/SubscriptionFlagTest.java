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
package com.hivemq.mqtt.topic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SubscriptionFlagTest {

    @Test
    public void test_getDefaultFlags() {

        final byte flags = SubscriptionFlag.getDefaultFlags(true, true, true);
        assertEquals((byte) 0b1110, flags);

        final byte flags2 = SubscriptionFlag.getDefaultFlags(false, true, true);
        assertEquals((byte) 0b1100, flags2);

        final byte flags3 = SubscriptionFlag.getDefaultFlags(false, false, false);
        assertEquals((byte) 0b0000, flags3);

        final byte flags4 = SubscriptionFlag.getDefaultFlags(false, false, false);
        assertEquals((byte) 0b0000, flags4);

        final byte flags5 = SubscriptionFlag.getDefaultFlags(true, false, false);
        assertEquals((byte) 0b0010, flags5);

        final byte flags6 = SubscriptionFlag.getDefaultFlags(false, true, false);
        assertEquals((byte) 0b0100, flags6);

        final byte flags7 = SubscriptionFlag.getDefaultFlags(false, false, true);
        assertEquals((byte) 0b1000, flags7);
    }

}