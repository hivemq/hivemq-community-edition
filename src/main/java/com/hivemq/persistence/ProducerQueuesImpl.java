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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.util.ThreadFactoryUtil;

import java.util.List;
import java.util.Queue;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.persistence.SingleWriterServiceImpl.Task;

/**
 * @author Lukas Brandl
 *         <p>
 *         The ProducerQueuesImpl class is a part of the single writer concept. There is one Instance of this class for
 *         each persistence that utilizes the single writer service.
 */
@SuppressWarnings("unchecked")
public class ProducerQueuesImpl implements ProducerQueues {

    private final AtomicLong taskCount = new AtomicLong(0);
    private final int amountOfQueues;
    @VisibleForTesting
    final int bucketsPerQueue;

    @VisibleForTesting
    final @NotNull ImmutableList<Queue<TaskWithFuture<?>>> queues;

    // Atomic booleans are more efficient than locks here, since we never actually wait for the lock.
    // Lock.tryLock() seams to park and unpark the thread each time :(
    private final @NotNull ImmutableList<AtomicBoolean> locks;
    private final @NotNull ImmutableList<AtomicLong> queueTaskCounter;
    private final @NotNull SingleWriterServiceImpl singleWriterServiceImpl;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private @Nullable ListenableFuture<Void> closeFuture;
    private long shutdownStartTime = Long.MAX_VALUE;
    // Initialized as long max value, to ensure the the grace period condition is not met, when shutdown is true but the start time is net yet set.


    public ProducerQueuesImpl(final SingleWriterServiceImpl singleWriterServiceImpl, final int amountOfQueues) {
        this.singleWriterServiceImpl = singleWriterServiceImpl;

        final int bucketCount = singleWriterServiceImpl.getPersistenceBucketCount();
        this.amountOfQueues = amountOfQueues;
        bucketsPerQueue = bucketCount / amountOfQueues;

        final ImmutableList.Builder<Queue<TaskWithFuture<?>>> queuesBuilder = ImmutableList.builder();
        for (int i = 0; i < amountOfQueues; i++) {
            queuesBuilder.add(new ConcurrentLinkedQueue<>());
        }
        queues = queuesBuilder.build();
        final ImmutableList.Builder<AtomicBoolean> locksBuilder = ImmutableList.builder();
        final ImmutableList.Builder<AtomicLong> counterBuilder = ImmutableList.builder();

        for (int i = 0; i < amountOfQueues; i++) {
            locksBuilder.add(new AtomicBoolean());
            counterBuilder.add(new AtomicLong(0));
        }
        locks = locksBuilder.build();
        queueTaskCounter = counterBuilder.build();
    }

    @NotNull
    public <R> ListenableFuture<R> submit(@NotNull final String key, @NotNull final Task<R> task) {
        //noinspection ConstantConditions (futuer is never null if the callbacks are null)
        return submitInternal(getBucket(key), task, null, null, false);
    }

    @NotNull
    public <R> ListenableFuture<R> submit(final int bucketIndex, @NotNull final Task<R> task) {
        //noinspection ConstantConditions (futuer is never null if the callbacks are null)
        return submitInternal(bucketIndex, task, null, null, false);
    }

    @Nullable
    public <R> ListenableFuture<R> submit(
            final int bucketIndex,
            @NotNull final Task<R> task,
            @Nullable final SingleWriterServiceImpl.SuccessCallback<R> successCallback,
            @Nullable final SingleWriterServiceImpl.FailedCallback failedCallback) {
        return submitInternal(bucketIndex, task, successCallback, failedCallback, false);
    }

    @Nullable
    public <R> ListenableFuture<R> submitInternal(
            final int bucketIndex,
            @NotNull final Task<R> task,
            @Nullable final SingleWriterServiceImpl.SuccessCallback<R> successCallback,
            @Nullable final SingleWriterServiceImpl.FailedCallback failedCallback,
            final boolean ignoreShutdown) {
        if (!ignoreShutdown &&
                shutdown.get() &&
                System.currentTimeMillis() - shutdownStartTime > singleWriterServiceImpl.getShutdownGracePeriod()) {
            return SettableFuture.create(); // Future will never return since we are shutting down.
        }
        final int queueIndex = bucketIndex / bucketsPerQueue;
        final Queue<TaskWithFuture<?>> queue = queues.get(queueIndex);
        final SettableFuture<R> resultFuture;
        if (successCallback == null) {
            resultFuture = SettableFuture.create();
        } else {
            resultFuture = null;
        }

        queue.add(new TaskWithFuture<>(resultFuture, task, bucketIndex, successCallback, failedCallback));
        taskCount.incrementAndGet();
        singleWriterServiceImpl.getGlobalTaskCount().incrementAndGet();
        if (queueTaskCounter.get(queueIndex).getAndIncrement() == 0) {
            singleWriterServiceImpl.incrementNonemptyQueueCounter();
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
    public @NotNull <R> List<ListenableFuture<R>> submitToAllBuckets(
            final @NotNull Task<R> task, final boolean parallel) {
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

    private @NotNull <R> List<ListenableFuture<R>> submitToAllBucketsParallel(
            final @NotNull Task<R> task, final boolean ignoreShutdown) {
        final ImmutableList.Builder<ListenableFuture<R>> builder = ImmutableList.builder();
        final int bucketCount = singleWriterServiceImpl.getPersistenceBucketCount();
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            //noinspection ConstantConditions (futuer is never null if the callbacks are null)
            builder.add(submitInternal(bucket, task, null, null, ignoreShutdown));
        }
        return builder.build();
    }

    public @NotNull <R> List<ListenableFuture<R>> submitToAllBucketsSequential(final @NotNull Task<R> task) {

        final ImmutableList.Builder<ListenableFuture<R>> builder = ImmutableList.builder();
        final int bucketCount = singleWriterServiceImpl.getPersistenceBucketCount();

        ListenableFuture<R> previousFuture = Futures.immediateFuture(null);
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            final int finalBucket = bucket;
            final SettableFuture<R> future = SettableFuture.create();
            previousFuture.addListener(() -> future.setFuture(submit(finalBucket, task)),
                    MoreExecutors.directExecutor());
            previousFuture = future;
            builder.add(future);
        }
        return builder.build();
    }

    public int getBucket(@NotNull final String key) {
        return BucketUtils.getBucket(key, singleWriterServiceImpl.getPersistenceBucketCount());
    }

    public void execute(final @NotNull SplittableRandom random) {
        final int queueIndex = random.nextInt(amountOfQueues);
        if (queueTaskCounter.get(queueIndex).get() == 0) {
            return;
        }
        final AtomicBoolean lock = locks.get(queueIndex);
        if (!lock.getAndSet(true)) {
            try {
                final Queue<TaskWithFuture<?>> queue = queues.get(queueIndex);
                int creditCount = 0;
                while (creditCount < singleWriterServiceImpl.getCreditsPerExecution()) {
                    final TaskWithFuture taskWithFuture = queue.poll();
                    if (taskWithFuture == null) {
                        return;
                    }
                    creditCount++;
                    try {
                        final Object result = taskWithFuture.getTask().doTask(taskWithFuture.getBucketIndex());
                        if (taskWithFuture.getFuture() != null) {
                            taskWithFuture.getFuture().set(result);
                        } else {
                            if (taskWithFuture.getSuccessCallback() != null) {
                                singleWriterServiceImpl.getCallbackExecutors()[queueIndex].submit(() -> taskWithFuture.getSuccessCallback()
                                        .afterTask(result));
                            }
                        }
                    } catch (final Throwable e) {
                        if (taskWithFuture.getFuture() != null) {
                            taskWithFuture.getFuture().setException(e);
                        } else {
                            if (taskWithFuture.getFailedCallback() != null) {
                                singleWriterServiceImpl.getCallbackExecutors()[queueIndex].submit(() -> taskWithFuture.getFailedCallback()
                                        .afterTask(e));
                            }
                        }
                    }
                    taskCount.decrementAndGet();
                    singleWriterServiceImpl.getGlobalTaskCount().decrementAndGet();
                    if (queueTaskCounter.get(queueIndex).decrementAndGet() == 0) {
                        singleWriterServiceImpl.decrementNonemptyQueueCounter();
                    }
                }
            } finally {
                lock.set(false);
            }
        }
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
        final ListeningScheduledExecutorService executorService =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(threadFactory));

        closeFuture = executorService.schedule(() -> {
                    // Even if no task has to be executed on shutdown, we still have to delay the success of the close future by the shutdown grace period.
                    if (finalTask != null) {
                        Futures.allAsList(submitToAllBucketsParallel(finalTask, true)).get();
                    } else {
                        Futures.allAsList(submitToAllBucketsParallel((Task<Void>) (bucketIndex) -> null, true)).get();
                    }
                    return null;
                },
                singleWriterServiceImpl.getShutdownGracePeriod() + 50,
                TimeUnit.MILLISECONDS); // We may have to delay the task for some milliseconds, because a task could just get enqueued.

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

    @NotNull
    public AtomicLong getTaskCount() {
        return taskCount;
    }

    @VisibleForTesting
    static class TaskWithFuture<T> {

        private final @Nullable SettableFuture<T> future;
        private final @NotNull Task task;
        private final int bucketIndex;
        private final @Nullable SingleWriterServiceImpl.SuccessCallback<T> successCallback;
        private final @Nullable SingleWriterServiceImpl.FailedCallback failedCallback;

        private TaskWithFuture(
                final @Nullable SettableFuture<T> future,
                final @NotNull Task task,
                final int bucketIndex,
                final @Nullable SingleWriterServiceImpl.SuccessCallback<T> successCallback,
                final @Nullable SingleWriterServiceImpl.FailedCallback failedCallback) {
            this.future = future;
            this.task = task;
            this.bucketIndex = bucketIndex;
            this.successCallback = successCallback;
            this.failedCallback = failedCallback;
        }

        @Nullable
        public SettableFuture getFuture() {
            return future;
        }

        @NotNull
        public Task getTask() {
            return task;
        }

        public int getBucketIndex() {
            return bucketIndex;
        }

        @Nullable SingleWriterServiceImpl.SuccessCallback<T> getSuccessCallback() {
            return successCallback;
        }

        @Nullable SingleWriterServiceImpl.FailedCallback getFailedCallback() {
            return failedCallback;
        }
    }
}
