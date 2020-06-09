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
package com.hivemq.persistence.local.xodus.clientsession;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Dominik Obermaier
 */
public class ClientSessionTimestampSerializerTest {

    private ClientSessionTimestampSerializer serializer;

    @Before
    public void setUp() throws Exception {
        serializer = new ClientSessionTimestampSerializer();

    }

    @Test
    public void test_timestamp_conversions_standalone() throws Exception {
        final long[] values = {1, System.currentTimeMillis(), /* max 5 byte*/ 549755813887l, /* max 6 byte */ 140737488355327l};

        for (final long value : values) {
            final byte[] bytes = serializer.timestampLongToByteArray(value);
            final long result = serializer.byteArrayToTimestampLong(bytes);
            assertEquals(value, result);
        }
    }

}