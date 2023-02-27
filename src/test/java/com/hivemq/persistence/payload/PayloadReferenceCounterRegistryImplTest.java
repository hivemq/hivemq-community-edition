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

import static com.hivemq.persistence.payload.PayloadReferenceCounterRegistry.UNKNOWN_PAYLOAD;
import static org.junit.Assert.assertEquals;

public class PayloadReferenceCounterRegistryImplTest {

    private @NotNull PayloadReferenceCounterRegistryImpl payloadReferenceCounterRegistry;
    private @NotNull BucketLock bucketLock;

    @Before
    public void setUp() throws Exception {
        bucketLock = new BucketLock(10);
        payloadReferenceCounterRegistry = new PayloadReferenceCounterRegistryImpl(bucketLock);
    }

    @Test
    public void test_get_whenNodeIsUnknown_thenReturn0() {
        final int referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertEquals(UNKNOWN_PAYLOAD, referenceCounter);
    }

    @Test
    public void test_get_whenReferenceCounterIsPresent_thenReturnCorrectCount() {
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        final int referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertEquals(1, referenceCounter);
    }

    @Test
    public void test_increment_whenNodeIsUnknown_thenAddNewEntry() {
        final int referenceCounter = payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        assertEquals(UNKNOWN_PAYLOAD, referenceCounter);
        final int referenceCounter2 = payloadReferenceCounterRegistry.get(1L);
        assertEquals(1, referenceCounter2);
    }

    @Test
    public void test_increment_whenNodeIsKnownButUniqueIsUnknown_thenAddNewEntry() {
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        final int referenceCounter = payloadReferenceCounterRegistry.getAndIncrementBy(2L, 1);
        assertEquals(UNKNOWN_PAYLOAD, referenceCounter);
        final int referenceCounter2 = payloadReferenceCounterRegistry.get(2L);
        assertEquals(1, referenceCounter2);
    }

    @Test
    public void test_increment_whenEntryIsAlreadyPresent_thenIncrementEntry() {
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        final int incremented = payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        assertEquals(1, incremented);
        final int referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertEquals(2, referenceCounter);
    }

    @Test
    public void test_add_whenNodeIsUnknown_thenAddNewEntry() {
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        final int referenceCounter = payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        assertEquals(1, referenceCounter);
        final int referenceCounter2 = payloadReferenceCounterRegistry.get(1L);
        assertEquals(2, referenceCounter2);
    }

    @Test
    public void test_add_whenEntryIsAlreadyPresent_thenIncrementEntry() {
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        final int incremented = payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        assertEquals(1, incremented);
        final int referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertEquals(2, referenceCounter);
    }


    @Test
    public void test_decrement_whenNodeIsUnknown_thenReturnNegativeValueButDontSetValueInRegistry() {
        final int referenceCounter = payloadReferenceCounterRegistry.decrementAndGet(1L);
        assertEquals(-1, referenceCounter);
        final int referenceCounter2 = payloadReferenceCounterRegistry.get(1L);
        assertEquals(UNKNOWN_PAYLOAD, referenceCounter2);
    }


    @Test
    public void test_decrement_whenNodeIsKnownButUniqueIsUnknown_thenReturnNegativeValueButDontSetValueInRegistry() {
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        final int referenceCounter = payloadReferenceCounterRegistry.decrementAndGet(2L);
        assertEquals(-1, referenceCounter);
        final int referenceCounter2 = payloadReferenceCounterRegistry.get(2L);
        assertEquals(UNKNOWN_PAYLOAD, referenceCounter2);
    }

    @Test
    public void test_decrement_whenNodeIsKnownButEntryIsUnknown_thenReturnNegativeValueButDontSetValueInRegistry() {
        final int decrement = payloadReferenceCounterRegistry.decrementAndGet(1L);
        assertEquals(UNKNOWN_PAYLOAD, decrement);
        final int referenceCounter = payloadReferenceCounterRegistry.get(2L);
        assertEquals(UNKNOWN_PAYLOAD, referenceCounter);
    }

    @Test
    public void test_decrement_whenEntryIsAlreadyPresent_thenDecrementEntry() {
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);

        final int decrement = payloadReferenceCounterRegistry.decrementAndGet(1L);
        assertEquals(2, decrement);
        final int referenceCounter = payloadReferenceCounterRegistry.get(1L);
        assertEquals(2, referenceCounter);
    }


    @Test
    public void test_size_whenMultipleNodesArePresent_thenSizeCoversAll() {
        payloadReferenceCounterRegistry.getAndIncrementBy(1L, 1);

        payloadReferenceCounterRegistry.getAndIncrementBy(2L, 1);
        payloadReferenceCounterRegistry.getAndIncrementBy(3L, 1);

        payloadReferenceCounterRegistry.getAndIncrementBy(4L, 1);
        payloadReferenceCounterRegistry.getAndIncrementBy(5L, 1);
        payloadReferenceCounterRegistry.getAndIncrementBy(6L, 1);

        final int size = payloadReferenceCounterRegistry.size();
        assertEquals(6, size);
    }
}
