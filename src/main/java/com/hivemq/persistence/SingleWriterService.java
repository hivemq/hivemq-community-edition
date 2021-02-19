package com.hivemq.persistence;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Daniel Krüger
 */
public interface SingleWriterService {


    /**
     * @param key associated with the task
     * @return an executor that will is single threaded and guarantied to be the same for equal keys
     */
    @NotNull ExecutorService callbackExecutor(@NotNull final String key);

    @NotNull ProducerQueues getRetainedMessageQueue();

    @NotNull ProducerQueues getClientSessionQueue();

    @NotNull ProducerQueues getSubscriptionQueue();

    @NotNull ProducerQueues getQueuedMessagesQueue();

    @NotNull ProducerQueues getAttributeStoreQueue();

    int getPersistenceBucketCount();

    int getCreditsPerExecution();

    long getShutdownGracePeriod();

    int getThreadPoolSize();

    @NotNull AtomicLong getGlobalTaskCount();


    @NotNull AtomicInteger getRunningThreadsCount();

    void stop();


    interface Task<R> {

        @NotNull R doTask(int bucketIndex, @NotNull ImmutableList<Integer> queueBuckets, int queueIndex);
    }

    interface SuccessCallback<R> {

        void afterTask(@NotNull R result);
    }

    interface FailedCallback {

        void afterTask(@NotNull Exception exception);
    }
}
