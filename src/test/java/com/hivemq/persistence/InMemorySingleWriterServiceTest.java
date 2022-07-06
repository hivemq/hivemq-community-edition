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

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Daniel Krüger
 */
public class InMemorySingleWriterServiceTest {

    private @NotNull InMemorySingleWriter singleWriterServiceImpl;

    @Before
    public void setUp() throws Exception {
        InternalConfigurations.PERSISTENCE_SHUTDOWN_GRACE_PERIOD_MSEC.set(200);
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(64);
        singleWriterServiceImpl = new InMemorySingleWriter();
    }

    @After
    public void tearDown() {
        singleWriterServiceImpl.stop();
    }

    @Test
    public void test_valid_amount_of_queues() {
        assertEquals(1, singleWriterServiceImpl.validAmountOfQueues(1, 64));
        assertEquals(2, singleWriterServiceImpl.validAmountOfQueues(2, 64));
        assertEquals(4, singleWriterServiceImpl.validAmountOfQueues(4, 64));
        assertEquals(8, singleWriterServiceImpl.validAmountOfQueues(5, 64));
        assertEquals(8, singleWriterServiceImpl.validAmountOfQueues(8, 64));
        assertEquals(64, singleWriterServiceImpl.validAmountOfQueues(64, 64));
    }


    @Test
    public void test_callbackExecutor_whenManyThreadsSubmitConcurrently_thenOnlyOneThreadWorksConcurrently() throws InterruptedException {
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
                    singleWriterServiceImpl.callbackExecutor("sameKey").execute(runnable);
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
        assertEquals(40_000, (int) list.pollFirst());
    }

    @Test
    public void test_getQueues_queuesAreDifferentPerType() {
        final ArrayList<ProducerQueues> queues = new ArrayList<>();

        final ProducerQueues attributeStoreQueue = singleWriterServiceImpl.getAttributeStoreQueue();
        queues.add(attributeStoreQueue);
        final ProducerQueues clientSessionQueue = singleWriterServiceImpl.getClientSessionQueue();
        queues.add(clientSessionQueue);
        final ProducerQueues queuedMessagesQueue = singleWriterServiceImpl.getQueuedMessagesQueue();
        queues.add(queuedMessagesQueue);
        final ProducerQueues retainedMessageQueue = singleWriterServiceImpl.getRetainedMessageQueue();
        queues.add(retainedMessageQueue);
        final ProducerQueues subscriptionQueue = singleWriterServiceImpl.getSubscriptionQueue();
        queues.add(subscriptionQueue);

        for (int i = 0; i < queues.size(); i++) {
            for (int j = 0; j < queues.size(); j++) {
                if (i == j) {
                    continue;
                }
                assertNotSame(queues.get(i), queues.get(j));
            }
        }
    }
}