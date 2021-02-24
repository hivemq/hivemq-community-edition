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

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.jctools.queues.MpscUnboundedArrayQueue
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Daniel Krüger
 * <p>
 * This SingleWriterService implementation does not use ExecutorThreadpools, but a trampoline approach like RxJava Schedulers.trampoline()
 * The advantage is that there are less thread switches (resulting in context switches).
 * The requirement is that no submitted task is blocking (that is only true for in-memory persistences)
 */
public class InMemorySingleWriterImpl implements SingleWriterService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SingleWriterServiceImpl.class);

    private static final int AMOUNT_OF_PRODUCERS = 5;
    private static final int RETAINED_MESSAGE_QUEUE_INDEX = 0;
    private static final int CLIENT_SESSION_QUEUE_INDEX = 1;
    private static final int SUBSCRIPTION_QUEUE_INDEX = 2;
    private static final int QUEUED_MESSAGES_QUEUE_INDEX = 3;
    private static final int ATTRIBUTE_STORE_QUEUE_INDEX = 4;

    private final @NotNull InMemoryProducerQueuesImpl @NotNull [] producers = new InMemoryProducerQueuesImpl[AMOUNT_OF_PRODUCERS];
    private final @NotNull InMemoryProducerQueuesImpl callbackProducerQueue;

    public final @NotNull MpscUnboundedArrayQueue<Runnable> @NotNull [] queues;
    public final @NotNull AtomicInteger @NotNull [] wips;

    private final int persistenceBucketCount;

    @Inject
    public InMemorySingleWriterImpl() {

        persistenceBucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
        final int threadPoolSize = InternalConfigurations.SINGLE_WRITER_THREAD_POOL_SIZE.get();
        final int creditsPerExecution = InternalConfigurations.SINGLE_WRITER_CREDITS_PER_EXECUTION.get();
        final int amountOfQueues = validAmountOfQueues(threadPoolSize, persistenceBucketCount);

        for (int i = 0; i < producers.length; i++) {
            producers[i] = new InMemoryProducerQueuesImpl(this, amountOfQueues);
        }
        callbackProducerQueue = new InMemoryProducerQueuesImpl(this, amountOfQueues);

        queues = new MpscUnboundedArrayQueue[amountOfQueues];
        wips = new AtomicInteger[amountOfQueues];
        for (int i = 0; i < amountOfQueues; i++) {
            queues[i] = new MpscUnboundedArrayQueue<>(256);
            wips[i] = new AtomicInteger();
        }
    }

    @VisibleForTesting
    int validAmountOfQueues(final int processorCount, final int bucketCount) {
        for (int i = processorCount; i < bucketCount; i++) {
            if (bucketCount % i == 0) {
                return i;
            }
        }
        return persistenceBucketCount;
    }

    public @NotNull ProducerQueues getRetainedMessageQueue() {
        return producers[RETAINED_MESSAGE_QUEUE_INDEX];
    }

    public @NotNull ProducerQueues getClientSessionQueue() {
        return producers[CLIENT_SESSION_QUEUE_INDEX];
    }

    public @NotNull ProducerQueues getSubscriptionQueue() {
        return producers[SUBSCRIPTION_QUEUE_INDEX];
    }

    public @NotNull ProducerQueues getQueuedMessagesQueue() {
        return producers[QUEUED_MESSAGES_QUEUE_INDEX];
    }

    public @NotNull ProducerQueues getAttributeStoreQueue() {
        return producers[ATTRIBUTE_STORE_QUEUE_INDEX];
    }

    public @NotNull Executor callbackExecutor(@NotNull final String key) {
        return command -> callbackProducerQueue.submit(key, (bucketIndex, queueBuckets, queueIndex) -> {
                    command.run();
                    return null; // this is fine, because Executors dont return anything. The return value will not be used.
                }
        );
    }

    public int getPersistenceBucketCount() {
        return persistenceBucketCount;
    }

    public void stop() {
        if (log.isTraceEnabled()) {
            log.trace("Shutting down single writer");
        }
    }

}
