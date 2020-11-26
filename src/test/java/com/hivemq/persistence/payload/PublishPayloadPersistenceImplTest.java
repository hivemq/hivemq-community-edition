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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.configuration.service.InternalConfigurations;
import net.openhft.hashing.LongHashFunction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.LogbackCapturingAppender;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
public class PublishPayloadPersistenceImplTest {

    @Mock
    PublishPayloadLocalPersistence localPersistence;
    @Mock
    ListeningScheduledExecutorService scheduledExecutorService;

    private final LongHashFunction hashFunction = LongHashFunction.xx();

    PublishPayloadPersistenceImpl persistence;

    private LogbackCapturingAppender logCapture;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InternalConfigurations.PAYLOAD_CACHE_DURATION.set(1000L);
        InternalConfigurations.PAYLOAD_CACHE_SIZE.set(1000);
        InternalConfigurations.PAYLOAD_CACHE_CONCURRENCY_LEVEL.set(1);
        InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE.set(10000);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(64);

        persistence = new PublishPayloadPersistenceImpl(localPersistence, scheduledExecutorService);
        persistence.init();
        logCapture = LogbackCapturingAppender.Factory.weaveInto(PublishPayloadPersistenceImpl.log);
    }

    @Test
    public void add_new_entries() throws Exception {
        final byte[] payload1 = "payload1".getBytes();
        final byte[] payload2 = "payload2".getBytes();
        persistence.add(payload1, 1, 123);
        persistence.add(payload2, 2, 234);

        assertEquals(1, persistence.referenceCounter.get(123L).get());
        assertEquals(2, persistence.referenceCounter.get(234L).get());
        assertNotNull(persistence.payloadCache.getIfPresent(123L));
        assertNotNull(persistence.payloadCache.getIfPresent(234L));
    }

    @Test
    public void add_existent_entry() throws Exception {
        final byte[] payload = "payload".getBytes();
        persistence.add(payload, 1, 123);
        persistence.add(payload, 2, 123);


        assertEquals(3, persistence.referenceCounter.get(123L).get());
        assertNotNull(persistence.payloadCache.getIfPresent(123L));
        assertEquals(1, persistence.payloadCache.size());
    }

    @Test
    public void get_from_cache() throws Exception {
        final byte[] payload = "payload".getBytes();
        persistence.add(payload, 1, 123);

        final long hash = hashFunction.hashBytes(payload);

        assertEquals(1, persistence.referenceCounter.get(123L).get());
        assertNotNull(persistence.payloadCache.getIfPresent(123L));
        assertEquals(1, persistence.payloadCache.size());

        final byte[] result = persistence.get(123);

        verify(localPersistence, never()).get(anyLong());
        assertEquals(true, Arrays.equals(payload, result));
    }

    @Test
    public void get_from_local_persistence() throws Exception {
        final byte[] payload = "payload".getBytes();
        persistence.add(payload, 1, 123);

        when(localPersistence.get(123)).thenReturn(payload);
        persistence.payloadCache.invalidate(123L);

        assertEquals(1, persistence.referenceCounter.get(123L).get());
        assertNull(persistence.payloadCache.getIfPresent(123L));
        assertEquals(0, persistence.payloadCache.size());

        final byte[] result = persistence.get(123);

        verify(localPersistence, times(1)).get(anyLong());
        assertEquals(true, Arrays.equals(payload, result));
    }

    @Test(expected = PayloadPersistenceException.class)
    public void get_from_local_persistence_null_payload() throws Exception {
        persistence.get(1);
    }

    @Test
    public void get_from_local_persistence_retained_message_null_payload() throws Exception {
        final byte[] bytes = persistence.getPayloadOrNull(1);
        assertNull(bytes);
    }

    @Test
    public void increment_new_reference_count() throws Exception {
        persistence.incrementReferenceCounterOnBootstrap(0L);
        assertEquals(1L, persistence.referenceCounter.get(0L).get());
    }

    @Test
    public void increment_existing_reference_count() throws Exception {
        persistence.referenceCounter.put(0L, new AtomicLong(1L));
        persistence.incrementReferenceCounterOnBootstrap(0L);
        assertEquals(2L, persistence.referenceCounter.get(0L).get());
    }

    @Test
    public void decrement_reference_count() throws Exception {
        persistence.referenceCounter.put(0L, new AtomicLong(2L));
        persistence.decrementReferenceCounter(0L);
        assertEquals(1L, persistence.referenceCounter.get(0L).get());
        assertEquals(0, persistence.removablePayloads.size());
    }

    @Test
    public void decrement_reference_count_to_zero() throws Exception {
        persistence.referenceCounter.put(0L, new AtomicLong(1L));
        persistence.decrementReferenceCounter(0L);
        assertEquals(0L, persistence.referenceCounter.get(0L).get());
        assertEquals(1, persistence.removablePayloads.size());
    }

    @Test
    public void decrement_reference_count_already_zero() throws Exception {
        persistence.referenceCounter.put(0L, new AtomicLong(0L));
        persistence.decrementReferenceCounter(0L);
        assertEquals(0L, persistence.referenceCounter.get(0L).get());
        assertEquals(0, persistence.removablePayloads.size());
    }

    @Test
    public void decrement_reference_count_null() throws Exception {
        persistence.decrementReferenceCounter(0L);
        assertNull(persistence.referenceCounter.get(0L));
        assertEquals(0, persistence.removablePayloads.size());
    }

    @Test
    public void init_persistence() throws Exception {

        InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE.set(250);
        InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_THREADS.set(4);

        persistence = new PublishPayloadPersistenceImpl(localPersistence, scheduledExecutorService);

        persistence.init();

        verify(scheduledExecutorService).scheduleAtFixedRate(any(RemoveEntryTask.class), eq(0L), eq(250L * 4L), eq(TimeUnit.MILLISECONDS));
        verify(scheduledExecutorService).scheduleAtFixedRate(any(RemoveEntryTask.class), eq(250L), eq(250L * 4L), eq(TimeUnit.MILLISECONDS));
        verify(scheduledExecutorService).scheduleAtFixedRate(any(RemoveEntryTask.class), eq(500L), eq(250L * 4L), eq(TimeUnit.MILLISECONDS));
        verify(scheduledExecutorService).scheduleAtFixedRate(any(RemoveEntryTask.class), eq(750L), eq(250L * 4L), eq(TimeUnit.MILLISECONDS));
    }

}