package com.hivemq.persistence;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.Executor;

/**
 * @author Daniel Krüger
 */
public interface SingleWriterService {

    @NotNull ProducerQueues getRetainedMessageQueue();

    @NotNull ProducerQueues getClientSessionQueue();

    @NotNull ProducerQueues getSubscriptionQueue();

    @NotNull ProducerQueues getQueuedMessagesQueue();

    @NotNull ProducerQueues getAttributeStoreQueue();

    @NotNull Executor callbackExecutor(@NotNull final String key);

    int getPersistenceBucketCount();

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
