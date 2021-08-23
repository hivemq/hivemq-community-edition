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
package com.hivemq.persistence;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 */
public class ProducerQueuesImplTest {

    @Mock
    @NotNull SingleWriterServiceImpl singleWriterServiceImpl;

    @NotNull ProducerQueuesImpl producerQueues;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(singleWriterServiceImpl.getPersistenceBucketCount()).thenReturn(64);
        when(singleWriterServiceImpl.getThreadPoolSize()).thenReturn(4);
        when(singleWriterServiceImpl.getGlobalTaskCount()).thenReturn(new AtomicLong());

        producerQueues = new ProducerQueuesImpl(singleWriterServiceImpl, 4);
    }

    @Test
    public void submit_task() throws Exception {
        producerQueues.submit("key", bucketIndex -> null);
        final int queueIndex = producerQueues.getBucket("key") / producerQueues.bucketsPerQueue;
        final Queue<ProducerQueuesImpl.TaskWithFuture<?>> queue = producerQueues.queues.get(queueIndex);
        assertEquals(1, queue.size());
    }

    @Test
    public void submitToAllBucketsParallel_allTasksSubmitted() throws Exception {
        producerQueues.submitToAllBucketsParallel(bucketIndex -> null);
        assertFalse(producerQueues.queues.isEmpty());
        for (final Queue<ProducerQueuesImpl.TaskWithFuture<?>> queue : producerQueues.queues) {
            assertEquals(64 / 4, queue.size());
        }
    }

    @Test
    public void submitToAllBucketsSequential_onlyOneTaskSubmitted() throws Exception {
        producerQueues.submitToAllBucketsSequential(bucketIndex -> null);
        assertFalse(producerQueues.queues.isEmpty());
        boolean found = false;
        for (final Queue<ProducerQueuesImpl.TaskWithFuture<?>> queue : producerQueues.queues) {
            if(!found){
                if(queue.size() == 1){
                    found = true;
                }
            } else {
                if(queue.size() == 1){
                    fail();
                }
            }
        }
    }
}
