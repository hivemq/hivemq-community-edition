package com.hivemq.persistence;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.util.internal.shaded.org.jctools.queues.MpscUnboundedArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Daniel Krüger
 * <p>
 * This SingleWriter implementation does not use ExecutorThreadpools, but a trampoline approach like RxJava Schedulers.trampoline()
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

    private final int persistenceBucketCount;
    private final int threadPoolSize;
    private final int creditsPerExecution;
    private final long shutdownGracePeriod;

    private final @NotNull AtomicBoolean postConstruct = new AtomicBoolean(true);
    private final @NotNull AtomicLong nonemptyQueueCounter = new AtomicLong(0);
    private final @NotNull AtomicInteger runningThreadsCount = new AtomicInteger(0);
    private final @NotNull AtomicLong globalTaskCount = new AtomicLong(0);


    private final @NotNull InMemoryProducerQueuesImpl @NotNull [] producers = new InMemoryProducerQueuesImpl[AMOUNT_OF_PRODUCERS];
    public final @NotNull MpscUnboundedArrayQueue<Runnable> @NotNull [] queues;
    public final @NotNull AtomicInteger @NotNull [] wips;

    @Inject
    public InMemorySingleWriterImpl() {

        persistenceBucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
        threadPoolSize = InternalConfigurations.SINGLE_WRITER_THREAD_POOL_SIZE.get();
        creditsPerExecution = InternalConfigurations.SINGLE_WRITER_CREDITS_PER_EXECUTION.get();
        shutdownGracePeriod = InternalConfigurations.PERSISTENCE_SHUTDOWN_GRACE_PERIOD.get();
        final int amountOfQueues = validAmountOfQueues(threadPoolSize, persistenceBucketCount);

        for (int i = 0; i < producers.length; i++) {
            producers[i] = new InMemoryProducerQueuesImpl(this, amountOfQueues);
        }

        queues = new MpscUnboundedArrayQueue[amountOfQueues];
        wips = new AtomicInteger[amountOfQueues];
        for (int i = 0; i < amountOfQueues; i++) {
            queues[i] = new MpscUnboundedArrayQueue<>(256);
            wips[i] = new AtomicInteger();
        }

        final ThreadFactory checkThreadFactory =
                new ThreadFactoryBuilder().setNameFormat("single-writer-scheduled-check-%d").build();
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

    public void decrementNonemptyQueueCounter() {
        nonemptyQueueCounter.decrementAndGet();
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

    public int getPersistenceBucketCount() {
        return persistenceBucketCount;
    }

    public int getCreditsPerExecution() {
        return creditsPerExecution;
    }

    public long getShutdownGracePeriod() {
        return shutdownGracePeriod;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public @NotNull AtomicLong getGlobalTaskCount() {
        return globalTaskCount;
    }


    public @NotNull AtomicInteger getRunningThreadsCount() {
        return runningThreadsCount;
    }

    public void stop() {
        if (log.isTraceEnabled()) {
            log.trace("Shutting down single writer");
        }
    }

}
