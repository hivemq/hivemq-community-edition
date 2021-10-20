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
package com.hivemq.mqtt.message.pool;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dominik Obermaier
 */
public class SequentialMessageIDPoolImplTest {

    @Test
    public void test_message_id_pool_produces_sequential() throws Exception {

        final List<Integer> integers = new ArrayList<>();
        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        for (int i = 0; i < 65535 * 2; i++) {

            final int id = messageIDPool.takeNextId();
            integers.add(id);
            messageIDPool.returnId(id);
        }

        assertTrue(areConsecutiveMessageIds(Lists.partition(integers, 65535).get(0)));
        assertTrue(areConsecutiveMessageIds(Lists.partition(integers, 65535).get(1)));
    }

    @Test
    public void test_prepopulation() throws Exception {

        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        messageIDPool.prepopulateWithUnavailableIds(1, 2, 3, 4, 5);

        assertEquals(6, messageIDPool.takeNextId());
    }

    @Test(expected = NoMessageIdAvailableException.class)
    public void test_no_ids_available() throws Exception {

        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        final List<Integer> ints = new ArrayList<>();
        for (int i = 1; i <= 65535; i++) {
            ints.add(i);
        }
        messageIDPool.prepopulateWithUnavailableIds(Ints.toArray(ints));

        messageIDPool.takeNextId();
    }

    @Test
    public void test_only_one_id_available() throws Exception {

        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        final List<Integer> ints = new ArrayList<>();
        for (int i = 1; i <= 65535; i++) {
            ints.add(i);
        }
        messageIDPool.prepopulateWithUnavailableIds(Ints.toArray(ints));

        messageIDPool.returnId(33333);
        assertEquals(33333, messageIDPool.takeNextId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_prepopulate_contains_invalid_message_id() throws Exception {
        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        messageIDPool.prepopulateWithUnavailableIds(1, 2, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_return_invalid_message_id() throws Exception {
        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        messageIDPool.returnId(70_000);
    }

    @Test
    public void test_prepopulate_and_return_id() throws Exception {

        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        for (int i = 0; i < 65534; i++) {

            messageIDPool.takeNextId();
        }

        messageIDPool.prepopulateWithUnavailableIds(65535);

        messageIDPool.returnId(1);
        assertEquals(1, messageIDPool.takeNextId());
    }

    @Test(expected = NoMessageIdAvailableException.class)
    public void test_take_when_no_id_available() throws Exception {

        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        for (int i = 0; i < 65535; i++) {

            messageIDPool.takeNextId();
        }

        messageIDPool.takeIfAvailable(42);
    }

    @Test
    public void test_take_sequential() throws Exception {

        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        messageIDPool.takeIfAvailable(42);

        assertEquals(43, messageIDPool.takeNextId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_take_invalid_message_id() throws Exception {
        final SequentialMessageIDPoolImpl messageIDPool = new SequentialMessageIDPoolImpl();

        messageIDPool.takeIfAvailable(70_000);
    }

    private static boolean areConsecutiveMessageIds(final @NotNull List<Integer> integerList) {

        int last = 0;
        for (int i = 0; i < 65535; i++) {
            final Integer integer = integerList.get(i);
            if (last + 1 != integer) {
                return false;
            }
            last = integer;
        }
        return true;
    }
}
