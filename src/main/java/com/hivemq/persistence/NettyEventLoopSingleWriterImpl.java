package com.hivemq.persistence;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.bootstrap.netty.NettyConfiguration;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Daniel Krüger
 * <p>
 * This SingleWriter implementation does not use ExecutorThreadpools, but a trampoline approach like RxJava Schedulers.trampoline()
 * The advantage is that there are less thread switches (resulting in context switches).
 * The requirement is that no submitted task is blocking (that is only true for in-memory persistences)
 */
public class NettyEventLoopSingleWriterImpl implements SingleWriterService {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SingleWriterServiceImpl.class);

    private static final int AMOUNT_OF_PRODUCERS = 5;
    private static final int RETAINED_MESSAGE_QUEUE_INDEX = 0;
    private static final int CLIENT_SESSION_QUEUE_INDEX = 1;
    private static final int SUBSCRIPTION_QUEUE_INDEX = 2;
    private static final int QUEUED_MESSAGES_QUEUE_INDEX = 3;
    private static final int ATTRIBUTE_STORE_QUEUE_INDEX = 4;

    private final int persistenceBucketCount;
    private final int amountOfQueues;

    private final EventExecutor @NotNull [] eventExecutors;
    private final ProducerQueues @NotNull [] producers = new ProducerQueues[AMOUNT_OF_PRODUCERS];

    @Inject
    public NettyEventLoopSingleWriterImpl(final @NotNull NettyConfiguration nettyConfiguration) {
        log.info("Instantiated NettyEventLoopSingleWriter.");
        persistenceBucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
        final int threadPoolSize = InternalConfigurations.SINGLE_WRITER_THREAD_POOL_SIZE.get();
        amountOfQueues = validAmountOfQueues(threadPoolSize, persistenceBucketCount);
        final List<EventExecutor> executors = new ArrayList<>();
        for (final EventExecutor executor : nettyConfiguration.getChildEventLoopGroup()) {
            executors.add(executor);
        }
        eventExecutors = executors.toArray(new EventExecutor[0]);

        for (int i = 0; i < producers.length; i++) {
            producers[i] = new NettyEventLoopProducerQueuesImpl(this, amountOfQueues, eventExecutors);
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

    @NotNull
    public ExecutorService callbackExecutor(@NotNull final String key) {
        final int bucketsPerQueue = persistenceBucketCount / amountOfQueues;
        final int bucketIndex = BucketUtils.getBucket(key, persistenceBucketCount);
        final int queueIndex = bucketIndex / bucketsPerQueue;
        return eventExecutors[queueIndex % eventExecutors.length];
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
