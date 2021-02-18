package com.hivemq.persistence;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import io.netty.util.internal.shaded.org.jctools.queues.MpscUnboundedArrayQueue;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.persistence.SingleWriterServiceImpl.Task;


/**
 * @author Daniel Krüger
 */
public class InMemoryProducerQueuesImpl implements ProducerQueues {

    private static final int NO_SPECIFIC_BUCKET = -1;

    private final AtomicLong taskCount = new AtomicLong(0);
    private final int amountOfQueues;
    @VisibleForTesting
    final int bucketsPerQueue;

    @VisibleForTesting
    @NotNull
    final ImmutableList<Queue<TaskWithFuture>> queues;

    // Atomic booleans are more efficient than locks here, since we never actually wait for the lock.
    // Lock.tryLock() seams to park and unpark the thread each time :(
    private final @NotNull ImmutableList<AtomicBoolean> locks;
    private final @NotNull ImmutableList<AtomicLong> queueTaskCounter;
    private final @NotNull InMemorySingleWriterImpl singleWriterServiceImpl;
    private final @NotNull ImmutableList<ImmutableList<Integer>> queueBucketIndexes;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private @Nullable ListenableFuture<Void> closeFuture;
    private long shutdownStartTime = Long.MAX_VALUE; // Initialized as long max value, to ensure the the grace period condition is not met, when shutdown is true but the start time is net yet set.

    public InMemoryProducerQueuesImpl(final @NotNull InMemorySingleWriterImpl singleWriterService, final int amountOfQueues) {
        this.singleWriterServiceImpl = singleWriterService;

        final int bucketCount = singleWriterServiceImpl.getPersistenceBucketCount();
        this.amountOfQueues = amountOfQueues;
        bucketsPerQueue = bucketCount / amountOfQueues;

        final ImmutableList.Builder<Queue<TaskWithFuture>> queuesBuilder = ImmutableList.builder();
        for (int i = 0; i < amountOfQueues; i++) {
            queuesBuilder.add(new ConcurrentLinkedQueue<>());
        }
        queues = queuesBuilder.build();
        final ImmutableList.Builder<ImmutableList<Integer>> bucketIndexListBuilder = ImmutableList.builder();
        final ImmutableList.Builder<AtomicBoolean> locksBuilder = ImmutableList.builder();
        final ImmutableList.Builder<AtomicLong> counterBuilder = ImmutableList.builder();

        for (int i = 0; i < amountOfQueues; i++) {
            locksBuilder.add(new AtomicBoolean());
            counterBuilder.add(new AtomicLong(0));
            bucketIndexListBuilder.add(createBucketIndexes(i, bucketsPerQueue));
        }
        locks = locksBuilder.build();
        queueTaskCounter = counterBuilder.build();
        queueBucketIndexes = bucketIndexListBuilder.build();
    }

    @NotNull
    @VisibleForTesting
    ImmutableList<Integer> createBucketIndexes(final int queueIndex, final int bucketsPerQueue) {
        final ImmutableList.Builder<Integer> builder = ImmutableList.builder();
        for (int i = bucketsPerQueue * queueIndex; i < bucketsPerQueue * (queueIndex + 1); i++) {
            builder.add(i);
        }
        return builder.build();
    }

    @NotNull
    public <R> ListenableFuture<R> submit(@NotNull final String key, @NotNull final Task<R> task) {
        //noinspection ConstantConditions (futuer is never null if the callbacks are null)
        return submit(getBucket(key), task, null, null);
    }

    @NotNull
    public <R> ListenableFuture<R> submit(final int bucketIndex,
                                          @NotNull final Task<R> task) {
        //noinspection ConstantConditions (futuer is never null if the callbacks are null)
        return submit(bucketIndex, task, null, null);
    }

    @Nullable
    public <R> ListenableFuture<R> submit(final int bucketIndex,
                                          @NotNull final Task<R> task,
                                          @Nullable final SingleWriterService.SuccessCallback<R> successCallback,
                                          @Nullable final SingleWriterService.FailedCallback failedCallback) {
        if (shutdown.get() && System.currentTimeMillis() - shutdownStartTime > singleWriterServiceImpl.getShutdownGracePeriod()) {
            return SettableFuture.create(); // Future will never return since we are shutting down.
        }
        final int queueIndex = bucketIndex / bucketsPerQueue;
        final SettableFuture<R> resultFuture;
        if (successCallback == null) {
            resultFuture = SettableFuture.create();
        } else {
            resultFuture = null;
        }

        final MpscUnboundedArrayQueue<Runnable> queue = singleWriterServiceImpl.queues[queueIndex];
        final AtomicInteger wip = singleWriterServiceImpl.wips[queueIndex];
        queue.offer(() -> {
            try {
                final R result = task.doTask(bucketIndex, queueBucketIndexes.get(queueIndex), queueIndex);
                if (resultFuture != null) {
                    resultFuture.set(result);
                } else {
                    if (successCallback != null) {
                        successCallback.afterTask(result);
                    }
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


    @NotNull
    public <R> List<ListenableFuture<R>> submitToAllQueues(final @NotNull Task<R> task) {
        return submitToAllQueues(task, false);
    }

    @NotNull
    public <R> ListenableFuture<List<R>> submitToAllQueuesAsList(final @NotNull Task<R> task) {
        return Futures.allAsList(submitToAllQueues(task, false));
    }

    @NotNull
    private <R> ListenableFuture<List<R>> submitToAllQueuesAsList(final @NotNull Task<R> task, final boolean ignoreShutdown) {
        return Futures.allAsList(submitToAllQueues(task, ignoreShutdown));
    }

    @NotNull
    private <R> List<ListenableFuture<R>> submitToAllQueues(final @NotNull Task<R> task, final boolean ignoreShutdown) {
        if (!ignoreShutdown && shutdown.get() && System.currentTimeMillis() - shutdownStartTime > singleWriterServiceImpl.getShutdownGracePeriod()) {
            return Collections.singletonList(SettableFuture.create()); // Future will never return since we are shutting down.
        }
        final ImmutableList.Builder<ListenableFuture<R>> builder = ImmutableList.builder();
        final List<ListenableFuture<R>> futures = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            final ListenableFuture<R> future = submit(i, task);
            futures.add(future);
        }
        return futures;
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
                final Queue<TaskWithFuture> queue = queues.get(queueIndex);
                int creditCount = 0;
                while (creditCount < singleWriterServiceImpl.getCreditsPerExecution()) {
                    final TaskWithFuture taskWithFuture = queue.poll();
                    if (taskWithFuture == null) {
                        return;
                    }
                    creditCount++;
                    try {
                        final Object result = taskWithFuture.getTask().doTask(taskWithFuture.getBucketIndex(), taskWithFuture.getQueueBuckets(), queueIndex);
                        if (taskWithFuture.getFuture() != null) {
                            taskWithFuture.getFuture().set(result);
                        } else {
                            if (taskWithFuture.getSuccessCallback() != null) {
                                singleWriterServiceImpl.getCallbackExecutors()[queueIndex].submit(() -> taskWithFuture.getSuccessCallback().afterTask(result));
                            }
                        }
                    } catch (final Exception e) {
                        if (taskWithFuture.getFuture() != null) {
                            taskWithFuture.getFuture().setException(e);
                        } else {
                            if (taskWithFuture.getFailedCallback() != null) {
                                singleWriterServiceImpl.getCallbackExecutors()[queueIndex].submit(() -> taskWithFuture.getFailedCallback().afterTask(e));
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
        final SettableFuture<Void> settableFuture = SettableFuture.create();
        settableFuture.set(null);
        return settableFuture;
    }

    @NotNull
    public AtomicLong getTaskCount() {
        return taskCount;
    }

    @VisibleForTesting
    static class TaskWithFuture<T> {
        @Nullable
        private final SettableFuture<T> future;
        @NotNull
        private final SingleWriterService.Task task;
        private final int bucketIndex;
        @NotNull
        private final ImmutableList<Integer> queueBuckets;
        @Nullable
        private final SingleWriterServiceImpl.SuccessCallback<T> successCallback;
        @Nullable
        private final SingleWriterServiceImpl.FailedCallback failedCallback;

        private TaskWithFuture(@Nullable final SettableFuture<T> future,
                               @NotNull final SingleWriterService.Task task,
                               final int bucketIndex,
                               @NotNull final ImmutableList<Integer> queueBuckets,
                               @Nullable final SingleWriterServiceImpl.SuccessCallback<T> successCallback,
                               @Nullable final SingleWriterServiceImpl.FailedCallback failedCallback) {
            this.future = future;
            this.task = task;
            this.bucketIndex = bucketIndex;
            this.queueBuckets = queueBuckets;
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

        @NotNull
        public ImmutableList<Integer> getQueueBuckets() {
            return queueBuckets;
        }

        @Nullable
        SingleWriterServiceImpl.SuccessCallback<T> getSuccessCallback() {
            return successCallback;
        }

        @Nullable
        SingleWriterServiceImpl.FailedCallback getFailedCallback() {
            return failedCallback;
        }
    }
}
