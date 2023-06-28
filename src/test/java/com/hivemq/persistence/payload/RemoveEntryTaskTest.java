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

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RemoveEntryTaskTest {

    private @NotNull PayloadReferenceCounterRegistry referenceCounter;
    private @NotNull PublishPayloadLocalPersistence localPersistence;
    private @NotNull BucketLock bucketLock;
    private @NotNull RemovablePayloads removablePayloads;

    @Before
    public void setUp() throws Exception {
        referenceCounter = new PayloadReferenceCounterRegistryImpl(new BucketLock(1));
        localPersistence = mock(PublishPayloadLocalPersistence.class);
        bucketLock = new BucketLock(1);
        removablePayloads = new RemovablePayloads(0, new LinkedList<>());
    }

    @Test
    public void run_whenPayloadNoMoreInUse_removesPayloadEntirely() {
        removablePayloads.getQueue().add(1L);
        referenceCounter.getAndIncrement(1L);
        referenceCounter.decrementAndGet(1L);
        final RemoveEntryTask task = new RemoveEntryTask(bucketLock,
                referenceCounter,
                localPersistence,
                new RemovablePayloads[]{removablePayloads});
        task.run();
        assertEquals(0, removablePayloads.getQueue().size());
        assertEquals(0, referenceCounter.size());
        verify(localPersistence, times(1)).remove(anyLong());
    }

    @Test
    public void run_whenThePayloadIsInUse_removesThePayloadFromRemovablePayloadsWithoutDecrementing() {
        removablePayloads.getQueue().add(1L);
        referenceCounter.getAndIncrement(1L);
        final RemoveEntryTask task = new RemoveEntryTask(bucketLock,
                referenceCounter,
                localPersistence,
                new RemovablePayloads[]{removablePayloads});
        task.run();
        assertEquals(0, removablePayloads.getQueue().size());
        assertEquals(1, referenceCounter.size());
        verify(localPersistence, never()).remove(anyLong());
    }

    @Test
    public void run_whenSomePayloadsInUseAndOthersNot_removesOnlyPayloadsWhichAreNotInUse() {
        removablePayloads.getQueue().add(1L);
        removablePayloads.getQueue().add(1L);
        referenceCounter.getAndIncrement(1L);
        referenceCounter.getAndIncrement(2L);
        referenceCounter.decrementAndGet(1L);
        referenceCounter.decrementAndGet(2L);
        final RemoveEntryTask task = new RemoveEntryTask(bucketLock,
                referenceCounter,
                localPersistence,
                new RemovablePayloads[]{removablePayloads});
        task.run();
        assertEquals(0, removablePayloads.getQueue().size());
        assertEquals(1, referenceCounter.size());
        verify(localPersistence, times(1)).remove(anyLong());
    }

    @Test
    public void run_forDuplicateEntries_removesAPayloadOnlyOnce() {
        removablePayloads.getQueue().add(1L);
        removablePayloads.getQueue().add(1L);
        referenceCounter.getAndIncrement(1L);
        referenceCounter.decrementAndGet(1L);
        final RemoveEntryTask task = new RemoveEntryTask(bucketLock,
                referenceCounter,
                localPersistence,
                new RemovablePayloads[]{removablePayloads});
        task.run();
        assertEquals(0, removablePayloads.getQueue().size());
        assertEquals(0, referenceCounter.size());
        verify(localPersistence, times(1)).remove(anyLong());
    }

    @Test
    public void run_whenAThrowableIsThrownDuringRemoval_thenDontReThrow() {
        final RemoveEntryTask task = createWithThrowableDuringRun(new Throwable("this is expected"));
        task.run();
        assertEquals(1, removablePayloads.getQueue().size());
        assertEquals(1, referenceCounter.size());
    }

    @Test(expected = Error.class)
    public void run_whenAnErrorIsThrownDuringRemoval_thenReThrow() {
        final RemoveEntryTask task = createWithThrowableDuringRun(new Error("this is expected"));
        task.run();
        assertEquals(1, removablePayloads.getQueue().size());
        assertEquals(1, referenceCounter.size());
    }

    private @NotNull RemoveEntryTask createWithThrowableDuringRun(final @NotNull Throwable throwable) {
        // Cover duplicate adds, too.
        removablePayloads.getQueue().add(1L);
        removablePayloads.getQueue().add(1L);
        referenceCounter.getAndIncrement(1L);
        referenceCounter.decrementAndGet(1L);
        doAnswer(invocation -> {
            throw throwable;
        }).when(localPersistence).remove(anyLong());
        return new RemoveEntryTask(bucketLock,
                referenceCounter,
                localPersistence,
                new RemovablePayloads[]{removablePayloads});
    }
}
