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
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.*;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.util.ThreadFactoryUtil;
import org.jctools.queues.MpscUnboundedArrayQueue;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.persistence.SingleWriterService.Task;


/**
 * @author Daniel Krüger
 * <p>
 * This class is responsible for the access of the persistences when in-memory persistences are used.
 * Access must be single-threaded for each bucket. This is achieved by guarding the entrance with an AtomicInteger (wips).
 * If another thread wants to access the same bucket at the same time, it will put the task in a queue, which will be consumed
 * by the thread that is currently working in the bucket. This way the access is single-threaded, non-blocking and context switches are
 * avoided.
 */
public class InMemoryProducerQueues implements ProducerQueues {

    private final int amountOfQueues;

    private final int bucketsPerQueue;

    private final @NotNull AtomicBoolean shutdown = new AtomicBoolean(false);

    private @Nullable ListenableFuture<Void> closeFuture;

    private final int persistenceBucketCount;

    public final @NotNull MpscUnboundedArrayQueue<Runnable> @NotNull [] queues;
    public final @NotNull AtomicInteger @NotNull [] wips;

    private final long shutdownGracePeriod;
    private long shutdownStartTime = Long.MAX_VALUE; // Initialized as long max value, to ensure that the grace period condition is not met, when shutdown is true but the start time is not yet set.

    public InMemoryProducerQueues(final int persistenceBucketCount, final int amountOfQueues) {

        this.persistenceBucketCount = persistenceBucketCount;
        this.amountOfQueues = amountOfQueues;
        bucketsPerQueue = persistenceBucketCount / amountOfQueues;
        shutdownGracePeriod = InternalConfigurations.PERSISTENCE_SHUTDOWN_GRACE_PERIOD_MSEC.get();

        final ImmutableList.Builder<AtomicLong> counterBuilder = ImmutableList.builder();

        queues = new MpscUnboundedArrayQueue[amountOfQueues];
        wips = new AtomicInteger[amountOfQueues];
        for (int i = 0; i < amountOfQueues; i++) {
            queues[i] = new MpscUnboundedArrayQueue<>(32);
            wips[i] = new AtomicInteger();
        }

        for (int i = 0; i < amountOfQueues; i++) {
            counterBuilder.add(new AtomicLong(0));
        }
    }

    @VisibleForTesting
    @NotNull ImmutableList<Integer> createBucketIndexes(final int queueIndex, final int bucketsPerQueue) {
        final ImmutableList.Builder<Integer> builder = ImmutableList.builder();
        for (int i = bucketsPerQueue * queueIndex; i < bucketsPerQueue * (queueIndex + 1); i++) {
            builder.add(i);
        }
        return builder.build();
    }

    public <R> @NotNull ListenableFuture<R> submit(final @NotNull String key, final @NotNull Task<R> task) {
        //noinspection ConstantConditions (future is never null if the callbacks are null)
        return submitInternal(getBucket(key), task, null, null, false);
    }

    public <R> @NotNull ListenableFuture<R> submit(final int bucketIndex,
                                          @NotNull final Task<R> task) {
        //noinspection ConstantConditions (futuer is never null if the callbacks are null)
        return submitInternal(bucketIndex, task, null, null, false);
    }


    public <R> @Nullable ListenableFuture<R> submit(final int bucketIndex,
                                                    final @NotNull Task<R> task,
                                                    @Nullable final SingleWriterService.SuccessCallback<R> successCallback,
                                                    @Nullable final SingleWriterService.FailedCallback failedCallback) {

        return submitInternal(bucketIndex, task, successCallback, failedCallback, false);
    }


    private <R> @Nullable ListenableFuture<R> submitInternal(final int bucketIndex,
                                                     final @NotNull Task<R> task,
                                                     @Nullable final SingleWriterService.SuccessCallback<R> successCallback,
                                                     @Nullable final SingleWriterService.FailedCallback failedCallback,
                                                     final boolean ignoreShutdown) {
        if (!ignoreShutdown && shutdown.get() && System.currentTimeMillis() - shutdownStartTime > shutdownGracePeriod) {
            return SettableFuture.create(); // Future will never return since we are shutting down.
        }
        final int queueIndex = bucketIndex / bucketsPerQueue;
        final SettableFuture<R> resultFuture;
        if (successCallback == null) {
            resultFuture = SettableFuture.create();
        } else {
            resultFuture = null;
        }

        final MpscUnboundedArrayQueue<Runnable> queue = queues[queueIndex];
        final AtomicInteger wip = wips[queueIndex];
        queue.offer(() -> {
            try {
                final R result = task.doTask(bucketIndex);
                if (resultFuture != null) {
                    resultFuture.set(result);
                } else {
                    successCallback.afterTask(result);
                }
            } catch (final Exception e) {
                if (resultFuture != null) {
                    resultFuture.setException(e);
                } else {
                    if (failedCallback != null) {
                        failedCallback.afterTask(e);
                    }
                }
            }
        });

        // here is the big difference between the the ProducerQueueImpl and the InMemoryProducerQueue
        // 1. test, whether another thread is already accessing the bucket (wip would be !=0)
        // 2. Consume the Queue
        // 3. Double check,
        //    first check: queue empty?, if true get out of the inner loop, else keep on consuming
        //    second check: check again whether another thread has queued a task meanwhile. If yes, return to work and consume the queue.
        //    these two checks are necessary, because queue poll/add and wip increase/decrease are not atomic
        if (wip.getAndIncrement() == 0) {
            int missed = 1;
            do {
                while (true) {
                    final Runnable runnable = queue.poll();
                    if (runnable != null) {
                        runnable.run();
                    } else {
                        break;
                    }
                }
                missed = wip.addAndGet(-missed);
            } while (missed != 0);
        }
        return resultFuture;
    }

    /**
     * submits the task for all buckets either parallel or sequential
     *
     * @param task     the task to submit
     * @param <R>      the returned object
     * @param parallel true for parallel, false for sequential
     * @return a list of listenableFutures of type R
     */
    public @NotNull <R> List<ListenableFuture<R>> submitToAllBuckets(final @NotNull Task<R> task, final boolean parallel) {
        if (parallel) {
            return submitToAllBucketsParallel(task, false);
        } else {
            return submitToAllBucketsSequential(task);
        }
    }

    /**
     * submits the task for all buckets at once
     *
     * @param task the task to submit
     * @param <R>  the returned object
     * @return a list of listenableFutures of type R
     */
    public @NotNull <R> List<ListenableFuture<R>> submitToAllBucketsParallel(final @NotNull Task<R> task) {
        return submitToAllBucketsParallel(task, false);
    }

    private @NotNull <R> List<ListenableFuture<R>> submitToAllBucketsParallel(final @NotNull Task<R> task, final boolean ignoreShutdown) {
        final ImmutableList.Builder<ListenableFuture<R>> builder = ImmutableList.builder();
        for (int bucket = 0; bucket < persistenceBucketCount; bucket++) {
            //noinspection ConstantConditions (futuer is never null if the callbacks are null)
            builder.add(submitInternal(bucket, task, null, null, ignoreShutdown));
        }
        return builder.build();
    }

    public @NotNull <R> List<ListenableFuture<R>> submitToAllBucketsSequential(final @NotNull Task<R> task) {

        final ImmutableList.Builder<ListenableFuture<R>> builder = ImmutableList.builder();

        ListenableFuture<R> previousFuture = Futures.immediateFuture(null);
        for (int bucket = 0; bucket < persistenceBucketCount; bucket++) {
            final int finalBucket = bucket;
            final SettableFuture<R> future = SettableFuture.create();
            previousFuture.addListener(() -> future.setFuture(submit(finalBucket, task)),
                    MoreExecutors.directExecutor());
            previousFuture = future;
            builder.add(future);
        }
        return builder.build();
    }


    public int getBucket(final @NotNull String key) {
        return BucketUtils.getBucket(key, persistenceBucketCount);
    }


    @NotNull
    public ListenableFuture<Void> shutdown(final @Nullable Task<Void> finalTask) {
        if (shutdown.getAndSet(true)) {
            //guard from being called twice
            //needed for integration tests because shutdown hooks for every Embedded HiveMQ are added to the JVM
            //if the persistence is stopped manually this would result in errors, because the shutdown hook might be called twice.
            if (closeFuture != null) {
                return closeFuture;
            }
            return Futures.immediateFuture(null);
        }

        shutdownStartTime = System.currentTimeMillis();
        // We create a temporary single thread executor when we shut down, so we don't waste a thread at runtime.
        final ThreadFactory threadFactory = ThreadFactoryUtil.create("persistence-shutdown-%d");
        final ListeningScheduledExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(threadFactory));

        closeFuture = executorService.schedule(() -> {
            // Even if no task has to be executed on shutdown, we still have to delay the success of the close future by the shutdown grace period.
            if (finalTask != null) {
                Futures.allAsList(submitToAllBucketsParallel(finalTask, true)).get();
            } else {
                Futures.allAsList(submitToAllBucketsParallel((Task<Void>) (bucketIndex) -> null, true)).get();
            }
            return null;
        }, shutdownGracePeriod + 50, TimeUnit.MILLISECONDS); // We may have to delay the task for some milliseconds, because a task could just get enqueued.

        Futures.addCallback(closeFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final Void aVoid) {
                executorService.shutdown();
            }

            @Override
            public void onFailure(final @NotNull Throwable throwable) {
                executorService.shutdown();
            }
        }, executorService);
        return closeFuture;
    }

}
