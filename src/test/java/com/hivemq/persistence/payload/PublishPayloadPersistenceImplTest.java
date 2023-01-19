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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.LogbackCapturingAppender;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
@SuppressWarnings("ConstantConditions")
public class PublishPayloadPersistenceImplTest {

    @Mock
    private @NotNull PublishPayloadLocalPersistence localPersistence;
    
    @Mock
    private @NotNull ListeningScheduledExecutorService scheduledExecutorService;

    private @NotNull PublishPayloadPersistenceImpl persistence;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE_MSEC.set(10000);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(64);

        persistence = new PublishPayloadPersistenceImpl(localPersistence, scheduledExecutorService);
        persistence.init();
        LogbackCapturingAppender.Factory.weaveInto(PublishPayloadPersistenceImpl.log);
    }

    @After
    public void tearDown() throws Exception {
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void add_forNewPayloadIds_setsReferenceCounters() {
        final byte[] payload1 = "payload1".getBytes();
        final byte[] payload2 = "payload2".getBytes();
        persistence.add(payload1, 1, 123);
        persistence.add(payload2, 2, 234);

        assertEquals(1, persistence.getReferenceCountersAsMap().get(123L).intValue());
        assertEquals(2, persistence.getReferenceCountersAsMap().get(234L).intValue());
    }

    @Test
    public void add_forTheSamePayloadId_increasesReferenceCounter() {
        final byte[] payload = "payload".getBytes();
        persistence.add(payload, 1, 123);
        persistence.add(payload, 2, 123);

        assertEquals(3, persistence.getReferenceCountersAsMap().get(123L).intValue());
    }

    @Test
    public void get_readsFromLocalPersistence() {
        final byte[] payload = "payload".getBytes();
        persistence.add(payload, 1, 123);
        when(localPersistence.get(123)).thenReturn(payload);
        assertEquals(1, persistence.getReferenceCountersAsMap().get(123L).intValue());
        final byte[] result = persistence.get(123);

        verify(localPersistence, times(1)).get(anyLong());
        assertArrayEquals(payload, result);
    }

    @Test(expected = PayloadPersistenceException.class)
    public void get_forExistingPayloadId_throwsPayloadPersistenceException() {
        persistence.get(1);
    }

    @Test
    public void getPayloadOrNull_forNonExistingPayloadId_returnsNull() {
        final byte[] bytes = persistence.getPayloadOrNull(1);
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

        verify(scheduledExecutorService).scheduleAtFixedRate(any(RemoveEntryTask.class), eq(0L), eq(250L * 4L), eq(TimeUnit.MILLISECONDS));
        verify(scheduledExecutorService).scheduleAtFixedRate(any(RemoveEntryTask.class), eq(250L), eq(250L * 4L), eq(TimeUnit.MILLISECONDS));
        verify(scheduledExecutorService).scheduleAtFixedRate(any(RemoveEntryTask.class), eq(500L), eq(250L * 4L), eq(TimeUnit.MILLISECONDS));
        verify(scheduledExecutorService).scheduleAtFixedRate(any(RemoveEntryTask.class), eq(750L), eq(250L * 4L), eq(TimeUnit.MILLISECONDS));
    }
}