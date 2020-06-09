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

import com.hivemq.configuration.HivemqId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Florian Limpöck
 */
public class ClientIdsTest {

    @Test
    public void test_100_000_ids_utf_well_formed_and_length_44() {

        final ClientIds clientIds = new ClientIds(new HivemqId());

        for (int i = 0; i < 100000; i++) {

            final String next = clientIds.generateNext();

            assertFalse(Utf8Utils.containsMustNotCharacters(next));
            assertFalse(Utf8Utils.hasControlOrNonCharacter(next));
            assertEquals(true, next.length() >= 44);

        }

    }
}