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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doThrow;

/**
 * @author Lukas Brandl
 */
public class RemoveEntryTaskTest {

    @Mock
    private PublishPayloadLocalPersistence localPersistence;

    private Cache<Long, byte[]> payloadCache;
    private BucketLock bucketLock;
    private Queue<RemovablePayload> removablePayloads;
    private final ConcurrentHashMap<Long, AtomicLong> referenceCounter = new ConcurrentHashMap<>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        payloadCache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .build();
        bucketLock = new BucketLock(1);
        removablePayloads = new LinkedTransferQueue<>();
    }

    @Test
    public void test_no_remove_during_delay() throws Exception {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis()));
        payloadCache.put(1L, "test".getBytes());
        referenceCounter.put(1L, new AtomicLong(0));
        final RemoveEntryTask task = new RemoveEntryTask(payloadCache, localPersistence, bucketLock, removablePayloads, 10000L, referenceCounter, 10000);
        task.run();
        assertNotNull(payloadCache.getIfPresent(1L));
        assertEquals(1, removablePayloads.size());
        assertEquals(1, referenceCounter.size());
    }

    @Test
    public void test_no_remove_if_refcount_not_zero() throws Exception {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100L));
        payloadCache.put(1L, "test".getBytes());
        referenceCounter.put(1L, new AtomicLong(1));
        final RemoveEntryTask task = new RemoveEntryTask(payloadCache, localPersistence, bucketLock, removablePayloads, 10L, referenceCounter, 10000);
        task.run();
        assertNotNull(payloadCache.getIfPresent(1L));
        assertEquals(0, removablePayloads.size());
        assertEquals(1, referenceCounter.size());
    }

    @Test
    public void test_remove_after_delay() throws Exception {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100L));
        payloadCache.put(1L, "test".getBytes());
        referenceCounter.put(1L, new AtomicLong(0));
        final RemoveEntryTask task = new RemoveEntryTask(payloadCache, localPersistence, bucketLock, removablePayloads, 10L, referenceCounter, 10000);
        task.run();
        assertNull(payloadCache.getIfPresent(1L));
        assertEquals(0, removablePayloads.size());
        assertEquals(0, referenceCounter.size());
    }

    @Test
    public void test_both() throws Exception {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100000L));
        removablePayloads.add(new RemovablePayload(2, System.currentTimeMillis()));
        payloadCache.put(1L, "test".getBytes());
        payloadCache.put(2L, "test".getBytes());
        referenceCounter.put(1L, new AtomicLong(0));
        referenceCounter.put(2L, new AtomicLong(0));
        final RemoveEntryTask task = new RemoveEntryTask(payloadCache, localPersistence, bucketLock, removablePayloads, 10000L, referenceCounter, 10000);
        task.run();
        assertNull(payloadCache.getIfPresent(1L));
        assertNotNull(payloadCache.getIfPresent(2L));
        assertEquals(1, removablePayloads.size());
        assertEquals(1, referenceCounter.size());
    }

    @Test
    public void test_remove_if_marked_twice() throws Exception {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100L));
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 500L));
        payloadCache.put(1L, "test".getBytes());
        referenceCounter.put(1L, new AtomicLong(0));
        final RemoveEntryTask task = new RemoveEntryTask(payloadCache, localPersistence, bucketLock, removablePayloads, 10L, referenceCounter, 10000);
        task.run();
        assertNull(payloadCache.getIfPresent(1L));
        assertEquals(0, removablePayloads.size());
        assertEquals(0, referenceCounter.size());
    }

    @Test(timeout = 5000)
    public void test_dont_stop_in_case_of_exception() throws Exception {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100L));
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100L));
        payloadCache.put(1L, "test".getBytes());
        referenceCounter.put(1L, new AtomicLong(0));
        doThrow(new RuntimeException("expected")).doNothing().when(localPersistence).remove(anyLong());
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final RemoveEntryTask task = new RemoveEntryTask(payloadCache, localPersistence, bucketLock, removablePayloads, 10L, referenceCounter, 10000);
        executorService.scheduleAtFixedRate(task, 10, 10, TimeUnit.MILLISECONDS);

        while (payloadCache.getIfPresent(1L) != null || removablePayloads.size() > 0 || referenceCounter.size() > 0) {
            Thread.sleep(10);
        }

        assertNull(payloadCache.getIfPresent(1L));
        assertEquals(0, removablePayloads.size());
        assertEquals(0, referenceCounter.size());
        executorService.shutdown();
    }
}