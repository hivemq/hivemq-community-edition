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
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Daniel Krüger
 */
public class InMemoryProducerQueuesTest {

    @NotNull
    private InMemoryProducerQueues producerQueues;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        producerQueues = new InMemoryProducerQueues(64, 4);
    }

    @Test
    public void test_create_bucket_indexes() {
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
    public void test_submit_whenManyThreadsSubmitConcurrently_thenOnlyOneThreadWorksConcurrently() throws InterruptedException {

        final LinkedList<Integer> list = new LinkedList<>();
        list.add(0);

        final List<Thread> threads = new ArrayList<>();
        final int numberOfConcurrentThreads = 4;

        // this is highly un-thread-safe, when this is concurrently executed
        // there are many sources for exceptions
        final Runnable runnable = () -> {
            final Integer poll = list.pollFirst();
            list.add(poll + 1);
        };

        for (int j = 0; j < 4; j++) {
            final Thread thread = new Thread(() -> {
                for (int i = 0; i < 10_000; i++) {
                    producerQueues.submit("same", (bucketIndex) -> {
                        runnable.run();
                        return null;
                    });
                }
            });
            threads.add(thread);
            thread.start();
        }

        //wait for all threads to finish their submissions
        for (final Thread thread : threads) {
            thread.join();
        }

        // 4 threads with 10_000 adds = 40_000
        assertEquals(40_000, (int) list.getFirst());
    }

}