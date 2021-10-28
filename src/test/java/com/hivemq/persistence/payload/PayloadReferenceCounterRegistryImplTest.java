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
package com.hivemq.persistence.payload;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PayloadReferenceCounterRegistryImplTest {


    private @NotNull PayloadReferenceCounterRegistryImpl payloadReferenceCounterRegistry;
    private @NotNull BucketLock bucketLock;

    @Before
    public void setUp() throws Exception {
        bucketLock = new BucketLock(10);
        payloadReferenceCounterRegistry = new PayloadReferenceCounterRegistryImpl(bucketLock);
    }
    

    @Test
    public void test_get_whenIdUnknown_thenReturnNull() {
        final Integer referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertNull(referenceCounter);
    }


    @Test
    public void test_get_whenReferenceCounterIsPresent_thenReturnCorrectCount() {
        payloadReferenceCounterRegistry.add(1L, 1);
        final Integer referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertEquals(1, (int) referenceCounter);
    }

    @Test
    public void test_increment_whenNodeIsUnknown_thenAddNewEntry() {
        final int referenceCounter = payloadReferenceCounterRegistry.increment(1L);
        assertEquals(1, referenceCounter);
        final Integer referenceCounter2 = payloadReferenceCounterRegistry.get(1L);
        assertEquals(1, (int) referenceCounter2);
    }


    @Test
    public void test_increment_whenNodeIsKnownButUniqueIsUnknown_thenAddNewEntry() {
        payloadReferenceCounterRegistry.increment(1L);
        final int referenceCounter = payloadReferenceCounterRegistry.increment(2L);
        assertEquals(1, referenceCounter);
        final Integer referenceCounter2 = payloadReferenceCounterRegistry.get(2L);
        assertEquals(1, (int) referenceCounter2);
    }

    @Test
    public void test_increment_whenNodeIsKnownBut_thenAddNewEntry() {
        payloadReferenceCounterRegistry.increment(1L);
        final int referenceCounter = payloadReferenceCounterRegistry.increment(2L);
        assertEquals(1, referenceCounter);
    }

    @Test
    public void test_increment_whenEntryIsAlreadyPresent_thenIncrementEntry() {
        payloadReferenceCounterRegistry.put(1L, 1);

        final int incremented = payloadReferenceCounterRegistry.increment(1L);
        assertEquals(2, incremented);
        final int referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertEquals(2, referenceCounter);
    }

    @Test
    public void test_add_whenNodeIsUnknown_thenAddNewEntry() {
        final int referenceCounter = payloadReferenceCounterRegistry.add(1L, 5);
        assertEquals(5, referenceCounter);
        final Integer referenceCounter2 = payloadReferenceCounterRegistry.get(1L);
        assertEquals(5, (int) referenceCounter2);
    }


    @Test
    public void test_add_whenNodeIsKnownButUniqueIsUnknown_thenAddNewEntry() {
        payloadReferenceCounterRegistry.increment(1L);
        final int referenceCounter = payloadReferenceCounterRegistry.add(2L, 5);
        assertEquals(5, referenceCounter);
        final Integer referenceCounter2 = payloadReferenceCounterRegistry.get(2L);
        assertEquals(5, (int) referenceCounter2);
    }

    @Test
    public void test_add_whenNodeIsKnownBut_thenAddNewEntry() {
        payloadReferenceCounterRegistry.increment(1L);
        final int referenceCounter = payloadReferenceCounterRegistry.add(2L, 3);
        assertEquals(3, referenceCounter);
    }

    @Test
    public void test_add_whenEntryIsAlreadyPresent_thenIncrementEntry() {
        payloadReferenceCounterRegistry.put(1L, 3);

        final int incremented = payloadReferenceCounterRegistry.add(1L, 5);
        assertEquals(8, incremented);
        final int referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertEquals(8, referenceCounter);
    }


    @Test
    public void test_decrement_whenNodeIsUnknown_thenReturnNegativeValueButDontSetValueInRegistry() {
        final int referenceCounter = payloadReferenceCounterRegistry.decrement(1L);
        assertEquals(-1, referenceCounter);
        final Integer referenceCounter2 = payloadReferenceCounterRegistry.get(1L);
        assertNull(referenceCounter2);
    }


    @Test
    public void test_decrement_whenNodeIsKnownButUniqueIsUnknown_thenReturnNegativeValueButDontSetValueInRegistry() {
        payloadReferenceCounterRegistry.increment(1L);
        final int referenceCounter = payloadReferenceCounterRegistry.decrement(2L);
        assertEquals(-1, referenceCounter);
        final Integer referenceCounter2 = payloadReferenceCounterRegistry.get(2L);
        assertNull(referenceCounter2);
    }

    @Test
    public void test_decrement_whenNodeIsKnownButEntryIsUnknown_thenReturnNegativeValueButDontSetValueInRegistry() {
        final int decrement = payloadReferenceCounterRegistry.decrement(1L);
        assertEquals(-1, decrement);
        final Integer referenceCounter = payloadReferenceCounterRegistry.get(2L);
        assertNull(referenceCounter);
    }

    @Test
    public void test_decrement_whenEntryIsAlreadyPresent_thenDecrementEntry() {
        payloadReferenceCounterRegistry.put(1L, 3);

        final int decrement = payloadReferenceCounterRegistry.decrement(1L);
        assertEquals(2, decrement);
        final Integer referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertEquals(2, (int) referenceCounter);
    }


    @Test
    public void test_size() {
        payloadReferenceCounterRegistry.put(1L, 3);

        payloadReferenceCounterRegistry.put(2L, 3);
        payloadReferenceCounterRegistry.put(3L, 3);

        payloadReferenceCounterRegistry.put(4L, 3);
        payloadReferenceCounterRegistry.put(5L, 3);
        payloadReferenceCounterRegistry.put(6L, 3);

        final int size = payloadReferenceCounterRegistry.size();
        assertEquals(6, size);
    }


    @Test
    public void test_remove_whenNodeIsUnknown_thenNoException() {
        payloadReferenceCounterRegistry.remove(1L);
    }


    @Test
    public void test_remove_whenNodeIsKnownButUniqueIsUnknown_thenNoException() {
        payloadReferenceCounterRegistry.increment(1L);
        payloadReferenceCounterRegistry.remove(2L);
    }


    @Test
    public void test_increment_whenEntryIsPresent_thenRemoveEntry() {
        payloadReferenceCounterRegistry.put(1L, 1);
        payloadReferenceCounterRegistry.remove(1L);
        assertEquals(0, payloadReferenceCounterRegistry.size());
        assertNull(payloadReferenceCounterRegistry.get(1L));
    }

}