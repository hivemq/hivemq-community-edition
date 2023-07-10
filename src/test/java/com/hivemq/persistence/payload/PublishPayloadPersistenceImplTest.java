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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("ConstantConditions")
public class PublishPayloadPersistenceImplTest {

    private final @NotNull PublishPayloadLocalPersistence localPersistence = mock(PublishPayloadLocalPersistence.class);
    private final @NotNull ListeningScheduledExecutorService scheduledExecutorService =
            mock(ListeningScheduledExecutorService.class);

    private @NotNull PublishPayloadPersistenceImpl persistence;

    @Before
    public void setUp() throws Exception {
        InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE_MSEC.set(10000);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(64);

        persistence = new PublishPayloadPersistenceImpl(localPersistence, scheduledExecutorService);
        persistence.init();
    }

    @Test
    public void add_forNewPayloadIds_setsReferenceCounters() {
        final byte[] payload1 = "payload1".getBytes();
        final byte[] payload2 = "payload2".getBytes();
        persistence.add(payload1, 123);
        persistence.add(payload2, 234);

        assertEquals(1, persistence.getReferenceCountersAsMap().get(123L).intValue());
        assertEquals(1, persistence.getReferenceCountersAsMap().get(234L).intValue());
    }

    @Test
    public void add_forTheSamePayloadId_increasesReferenceCounter() {
        final byte[] payload = "payload".getBytes();
        persistence.add(payload, 123);
        persistence.add(payload, 123);
        persistence.add(payload, 123);

        assertEquals(3, persistence.getReferenceCountersAsMap().get(123L).intValue());
    }

    @Test
    public void get_readsFromLocalPersistence() {
        final byte[] payload = "payload".getBytes();
        persistence.add(payload, 123);
        when(localPersistence.get(123)).thenReturn(payload);
        assertEquals(1, persistence.getReferenceCountersAsMap().get(123L).intValue());
        final byte[] result = persistence.get(123);

        verify(localPersistence, times(1)).get(anyLong());
        assertArrayEquals(payload, result);
    }

    @Test
    public void get_forNonExistingPayloadId_returnsNull() {
        final byte[] bytes = persistence.get(1);
        assertNull(bytes);
    }

    @Test
    public void incrementReferenceCounter_forNewPayloadId_setsReferenceCounter() {
        persistence.incrementReferenceCounterOnBootstrap(0L);
        assertEquals(1, persistence.getReferenceCountersAsMap().get(0L).intValue());
    }

    @Test
    public void incrementReferenceCounter_forExistingPayloadId_incrementsReferenceCounter() {
        persistence.incrementReferenceCounterOnBootstrap(0L);
        persistence.incrementReferenceCounterOnBootstrap(0L);
        assertEquals(2, persistence.getReferenceCountersAsMap().get(0L).intValue());
    }

    @Test
    public void decrementReferenceCounter_forExistingPayloadId_decrementsReferenceCounter() {
        persistence.incrementReferenceCounterOnBootstrap(0L);
        persistence.incrementReferenceCounterOnBootstrap(0L);
        persistence.decrementReferenceCounter(0L);
        assertEquals(1, persistence.getReferenceCountersAsMap().get(0L).intValue());
    }

    @Test
    public void decrementReferenceCounter_forExistingPayloadId_decrementsReferenceCounterToZero() {
        persistence.incrementReferenceCounterOnBootstrap(0L);
        persistence.decrementReferenceCounter(0L);
    }

    @Test
    public void decrementReferenceCounter_forExistingPayloadId_decrementsReferenceCounterMaxToZero() {
        persistence.incrementReferenceCounterOnBootstrap(0L);
        persistence.decrementReferenceCounter(0L);
        persistence.decrementReferenceCounter(0L);
    }

    @Test
    public void decrementReferenceCounter_forNonExistingPayloadId_createsNoReferenceCount() {
        persistence.decrementReferenceCounter(0L);
        assertNull(persistence.getReferenceCountersAsMap().get(0L));
    }

    @Test
    public void init_schedulesPayloadCleanup() {
        InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE_MSEC.set(250);
        InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_THREADS.set(4);
        persistence = new PublishPayloadPersistenceImpl(localPersistence, scheduledExecutorService);
        persistence.init();

        verify(scheduledExecutorService, times(4)).scheduleWithFixedDelay(any(RemoveEntryTask.class),
                eq(250L),
                eq(250L),
                eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void partitionBucketResponsibilities_whenRemovablePayloadsDividesEvenly_thenEveryThreadHasSameAmountOfResponsibilities() {

        final RemovablePayloads[] removablePayloadsArray = {
                new RemovablePayloads(1, new LinkedList<>()),
                new RemovablePayloads(2, new LinkedList<>()),
                new RemovablePayloads(3, new LinkedList<>()),
                new RemovablePayloads(4, new LinkedList<>())};

        final @NotNull RemovablePayloads[] @NotNull [] removablePayloads =
                PublishPayloadPersistenceImpl.partitionBucketResponsibilities(removablePayloadsArray, 2);

        assertNotNull(removablePayloads);
        assertEquals(2, removablePayloads.length);

        assertEquals(2, removablePayloads[0].length);
        assertEquals(1, removablePayloads[0][0].getBucketIndex());
        assertEquals(2, removablePayloads[0][1].getBucketIndex());

        assertEquals(2, removablePayloads[1].length);
        assertEquals(3, removablePayloads[1][0].getBucketIndex());
        assertEquals(4, removablePayloads[1][1].getBucketIndex());
    }

    @Test
    public void partitionBucketResponsibilities_whenRemovablePayloadsDividesUnevenly_thenRemainingBucketsAreDistributed() {

        final LinkedList<Long> queue = new LinkedList<>();

        final RemovablePayloads[] removablePayloadsArray = {
                new RemovablePayloads(1, queue),
                new RemovablePayloads(2, queue),
                new RemovablePayloads(3, queue),
                new RemovablePayloads(4, queue),
                new RemovablePayloads(5, queue),
                new RemovablePayloads(6, queue),
                new RemovablePayloads(7, queue),
                new RemovablePayloads(8, queue),
                new RemovablePayloads(9, queue),
                new RemovablePayloads(10, queue),
                new RemovablePayloads(11, queue)};

        final @NotNull RemovablePayloads[] @NotNull [] removablePayloads =
                PublishPayloadPersistenceImpl.partitionBucketResponsibilities(removablePayloadsArray, 3);

        assertNotNull(removablePayloads);
        assertEquals(3, removablePayloads.length);

        assertEquals(4, removablePayloads[0].length);
        assertEquals(1, removablePayloads[0][0].getBucketIndex());
        assertEquals(2, removablePayloads[0][1].getBucketIndex());
        assertEquals(3, removablePayloads[0][2].getBucketIndex());
        assertEquals(11, removablePayloads[0][3].getBucketIndex());

        assertEquals(4, removablePayloads[1].length);
        assertEquals(4, removablePayloads[1][0].getBucketIndex());
        assertEquals(5, removablePayloads[1][1].getBucketIndex());
        assertEquals(6, removablePayloads[1][2].getBucketIndex());
        assertEquals(10, removablePayloads[1][3].getBucketIndex());

        assertEquals(3, removablePayloads[2].length);
        assertEquals(7, removablePayloads[2][0].getBucketIndex());
        assertEquals(8, removablePayloads[2][1].getBucketIndex());
        assertEquals(9, removablePayloads[2][2].getBucketIndex());
    }

    @Test
    public void partitionBucketResponsibilities_whenRemovablePayloadsLessThenThreads_thenAllBucketsAreDistributed() {

        final LinkedList<Long> queue = new LinkedList<>();

        final RemovablePayloads[] removablePayloadsArray = {
                new RemovablePayloads(1, queue),
                new RemovablePayloads(2, queue),
                new RemovablePayloads(3, queue),
                new RemovablePayloads(4, queue),};

        final @NotNull RemovablePayloads[] @NotNull [] removablePayloads =
                PublishPayloadPersistenceImpl.partitionBucketResponsibilities(removablePayloadsArray, 6);

        assertNotNull(removablePayloads);
        assertEquals(6, removablePayloads.length);

        assertEquals(1, removablePayloads[0].length);
        assertEquals(4, removablePayloads[0][0].getBucketIndex());

        assertEquals(1, removablePayloads[1].length);
        assertEquals(3, removablePayloads[1][0].getBucketIndex());

        assertEquals(1, removablePayloads[2].length);
        assertEquals(2, removablePayloads[2][0].getBucketIndex());

        assertEquals(1, removablePayloads[3].length);
        assertEquals(1, removablePayloads[3][0].getBucketIndex());
    }
}
