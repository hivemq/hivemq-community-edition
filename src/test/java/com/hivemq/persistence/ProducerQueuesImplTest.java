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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 */
public class ProducerQueuesImplTest {

    @Mock
    SingleWriterServiceImpl singleWriterServiceImpl;

    ProducerQueuesImpl producerQueues;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(singleWriterServiceImpl.getPersistenceBucketCount()).thenReturn(64);
        when(singleWriterServiceImpl.getThreadPoolSize()).thenReturn(4);
        when(singleWriterServiceImpl.getGlobalTaskCount()).thenReturn(new AtomicLong());

        producerQueues = new ProducerQueuesImpl(singleWriterServiceImpl, 4);
    }

    @Test
    public void test_create_bucket_indexes() throws Exception {
        final ImmutableList<Integer> indexes0 = producerQueues.createBucketIndexes(0, 3);
        assertTrue(indexes0.contains(0));
        assertTrue(indexes0.contains(1));
        assertTrue(indexes0.contains(2));
        assertEquals(3, indexes0.size());

        final ImmutableList<Integer> indexes1 = producerQueues.createBucketIndexes(1, 3);
        assertTrue(indexes1.contains(3));
        assertTrue(indexes1.contains(4));
        assertTrue(indexes1.contains(5));
        assertEquals(3, indexes1.size());

        final ImmutableList<Integer> indexes2 = producerQueues.createBucketIndexes(0, 1);
        assertTrue(indexes2.contains(0));
        assertEquals(1, indexes2.size());
    }

    @Test
    public void submit_task() throws Exception {
        producerQueues.submit("key", new SingleWriterServiceImpl.Task<Object>() {
            @Override
            public Object doTask(final int bucketIndex, final @NotNull ImmutableList<Integer> queueBuckets, final int queueIndex) {
                return null;
            }
        });
        final int queueIndex = producerQueues.getBucket("key") / producerQueues.bucketsPerQueue;
        final Queue<ProducerQueuesImpl.TaskWithFuture> queue = producerQueues.queues.get(queueIndex);
        assertEquals(1, queue.size());
    }

    @Test
    public void submit_task_to_all_queues() throws Exception {
        producerQueues.submitToAllQueues(new SingleWriterServiceImpl.Task<Object>() {
            @Override
            public Object doTask(final int bucketIndex, final @NotNull ImmutableList<Integer> queueBuckets, final int queueIndex) {
                return null;
            }
        });

        for (final Queue<ProducerQueuesImpl.TaskWithFuture> queue : producerQueues.queues) {
            assertEquals(1, queue.size());
        }
    }
}