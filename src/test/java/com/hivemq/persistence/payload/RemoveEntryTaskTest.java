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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

public class RemoveEntryTaskTest {

    @Mock
    private @NotNull PublishPayloadLocalPersistence localPersistence;

    private @NotNull BucketLock bucketLock;
    private @NotNull Queue<RemovablePayload> removablePayloads;
    private @NotNull PayloadReferenceCounterRegistry referenceCounterRegistry;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        bucketLock = new BucketLock(1);
        removablePayloads = new LinkedTransferQueue<>();
        referenceCounterRegistry = new PayloadReferenceCounterRegistryImpl(bucketLock);
    }

    @Test
    public void run_whenARemoveDelayIsSet_doesNotRemoveAPayloadIfNotYetExpired() {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis()));
        referenceCounterRegistry.getAndIncrement(1L);
        referenceCounterRegistry.decrementAndGet(1L);
        final RemoveEntryTask task = new RemoveEntryTask(localPersistence,
                bucketLock,
                removablePayloads,
                10000L,
                referenceCounterRegistry,
                10000);
        task.run();
        assertEquals(1, removablePayloads.size());
        assertEquals(1, referenceCounterRegistry.size());
    }

    @Test
    public void run_whenTheRemoveDelayIsExpired_removesThePayloadWithoutDecrementingTheReferenceCounter() {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100L));
        referenceCounterRegistry.getAndIncrement(1);
        final RemoveEntryTask task = new RemoveEntryTask(localPersistence,
                bucketLock,
                removablePayloads,
                10L,
                referenceCounterRegistry,
                10000);
        task.run();
        assertEquals(0, removablePayloads.size());
        assertEquals(1, referenceCounterRegistry.size());
    }

    @Test
    public void run_whenPayloadsHaveExpiredRemoveDelaysOrNot_removesExpiredPayloadsOnlyWithoutDecrementingTheReferenceCounters() {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100000L));
        removablePayloads.add(new RemovablePayload(2, System.currentTimeMillis()));
        final RemoveEntryTask task = new RemoveEntryTask(localPersistence,
                bucketLock,
                removablePayloads,
                10000L,
                referenceCounterRegistry,
                10000);
        referenceCounterRegistry.getAndIncrement(1L);
        referenceCounterRegistry.getAndIncrement(2L);
        task.run();
        assertEquals(1, removablePayloads.size());
        assertEquals(1, referenceCounterRegistry.size());
    }

    @Test
    public void run_forDuplicateEntries_removesAPayloadOnlyOnce() {
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100L));
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 500L));
        final RemoveEntryTask task = new RemoveEntryTask(localPersistence,
                bucketLock,
                removablePayloads,
                10L,
                referenceCounterRegistry,
                10000);
        referenceCounterRegistry.getAndIncrement(1L);
        task.run();
        assertEquals(0, removablePayloads.size());
        assertEquals(0, referenceCounterRegistry.size());
    }

    @Test
    public void run_whenAThrowableIsThrownDuringRemoval_thenDontReThrow() {
        final RemoveEntryTask task = createWithThrowableDuringRun(new Throwable("this is expected"));
        task.run();
        assertEquals(1, removablePayloads.size());
        assertEquals(1, referenceCounterRegistry.size());
    }

    @Test(expected = Error.class)
    public void run_whenAnErrorIsThrownDuringRemoval_thenReThrow() {
        final RemoveEntryTask task = createWithThrowableDuringRun(new Error("this is expected"));
        task.run();
        assertEquals(1, removablePayloads.size());
        assertEquals(1, referenceCounterRegistry.size());
    }

    private @NotNull RemoveEntryTask createWithThrowableDuringRun(final @NotNull Throwable throwable) {
        // Cover duplicate adds, too.
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100L));
        removablePayloads.add(new RemovablePayload(1, System.currentTimeMillis() - 100L));
        referenceCounterRegistry.getAndIncrement(1L);
        doAnswer(invocation -> {
            throw throwable;
        }).when(localPersistence).remove(anyLong());
        return new RemoveEntryTask(localPersistence,
                bucketLock,
                removablePayloads,
                10L,
                referenceCounterRegistry,
                10000);
    }
}
