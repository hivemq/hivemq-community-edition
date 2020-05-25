/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.executor.task;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.common.annotations.GuardedBy;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extensions.ioc.annotation.PluginTaskQueue;
import com.hivemq.util.Exceptions;
import com.hivemq.util.ThreadFactoryUtil;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * There is one Thread that constantly iterates multiple queues if they have any tasks.
 * If the queues have tasks it takes the first one from the queue. If the first task has the "async" feature enabled,
 * then the tasks stays in the queue and the Thread continues on to the next queue.
 * When the async task taken by the Thread is not marked as done it is ignored, if it is done the post-functions are
 * executed and the Thread moves on to the next queue.
 *
 * @author Christoph Schäbel
 */
@ThreadSafe
public class PluginTaskExecutor {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(ThreadFactoryUtil.create("extension-task-executor-%d"));
    private final AtomicBoolean running = new AtomicBoolean(true);

    @GuardedBy("stripedLock")
    private final ConcurrentMap<String, Queue<PluginTaskExecution>> taskQueues = new ConcurrentHashMap<>();

    @NotNull
    private final AtomicLong counterAllQueues;

    /**
     * The semaphore is used to make the thread wait if no more tasks are available.
     * The thread is therefore *not* busy-waiting if the queues are empty.
     */
    private final Semaphore semaphore = new Semaphore(0);

    /**
     * This striped lock is used to prevent concurrency issues when the queues are removed and added
     */
    private final Striped<Lock> stripedLock = Striped.lock(100);

    @Inject
    public PluginTaskExecutor(@NotNull @PluginTaskQueue final AtomicLong counterAllQueues) {
        this.counterAllQueues = counterAllQueues;
    }

    @VisibleForTesting
    @PostConstruct
    public void postConstruct() {
        executorService.submit(new PluginTaskExecutorRunnable());
    }

    public void stop() {
        running.set(false);
        executorService.shutdownNow();
    }

    public void handlePluginTaskExecution(@NotNull final PluginTaskExecution pluginTaskExecution) {

        if (!running.get()) {
            throw new RejectedExecutionException("Extension Task executor is already stopped");
        }

        counterAllQueues.getAndIncrement();

        final String identifier = pluginTaskExecution.getPluginContext().getIdentifier();

        final Lock lock = stripedLock.get(identifier);

        try {
            lock.lock();
            final Queue<PluginTaskExecution> queueForId = taskQueues.computeIfAbsent(identifier, new CreateQueueIfNotPresent());
            queueForId.add(pluginTaskExecution);
        } finally {
            lock.unlock();
        }
        semaphore.release();
    }

    private static class CreateQueueIfNotPresent implements Function<String, Queue<PluginTaskExecution>> {
        @NotNull
        @Override
        public Queue<PluginTaskExecution> apply(@NotNull final String id) {
            return new ConcurrentLinkedQueue<>();
        }
    }

    private class PluginTaskExecutorRunnable implements Runnable {

        @Override
        public void run() {
            try {
                //only run if a task is present
                semaphore.acquire();

                while (running.get()) {

                    boolean taskExecuted = false;
                    final int availablePermitsBeforeLoop = semaphore.availablePermits();

                    for (final Map.Entry<String, Queue<PluginTaskExecution>> taskQueueEntry : taskQueues.entrySet()) {
                        final Queue<PluginTaskExecution> queue = taskQueueEntry.getValue();
                        final String key = taskQueueEntry.getKey();


                        if (queue.isEmpty()) {
                            if (possiblyCleanupEmptyQueue(key)) {
                                continue;
                            }
                        }

                        final PluginTaskExecution task = queue.peek();

                        if (task == null) {
                            continue;
                        }

                        if (task.isAsync()) {
                            if (task.isDone()) {
                                //if the task is async and already done, then excute the post functions
                                // and clean the task
                                executeDoneTask(task);
                                queue.remove();
                                counterAllQueues.decrementAndGet();
                                taskExecuted = true;

                                //async task is done, only run if another task is available
                                semaphore.acquire();
                            }
                            continue;
                        }

                        try {
                            taskExecuted = true;
                            executeTask(task);

                            if (!task.isAsync()) {
                                queue.remove();
                                counterAllQueues.decrementAndGet();
                            }

                        } catch (final Throwable t) {
                            queue.remove();
                            counterAllQueues.decrementAndGet();
                            Exceptions.rethrowError("Exception at extension task", t);
                        } finally {
                            //only continue to the next queue if a task is present
                            semaphore.acquire();
                        }
                    }


                    if (!taskExecuted) {
                        //if the for-loop did run through all queues and could not execute at least one task
                        // we wait for a change in the semaphore. We wait for a change in the semaphore by requesting
                        // all available permits and giving them all back afterwards, therefore not decreasing
                        // the overall count. We have to use the count of available permits before the loop, otherwise
                        // we could miss a new task addition and wait here for a higher count than we actually want.
                        semaphore.acquire(availablePermitsBeforeLoop + 1);
                        semaphore.release(availablePermitsBeforeLoop + 1);
                    }
                }

            } catch (final InterruptedException ignored) {
                //ignore, finally already takes care of rescheduling
            } catch (final Throwable t) {
                Exceptions.rethrowError("Exception at PluginTaskExecutor", t);
            } finally {
                if (running.get()) {
                    executorService.submit(this);
                }
            }
        }

        /**
         * Tries to clean an empty queue if it is really empty
         */
        private boolean possiblyCleanupEmptyQueue(@NotNull final String key) {
            //cleanup empty queues immediately
            //only acquire the lock if the queue might be empty, if we don't see the queue as empty
            // although it is empty it will be cleaned up by a later run
            // the lock is required to prevent the threads which are adding tasks from adding entries
            // while the queue is removed and cleaned up
            final Lock lock = stripedLock.get(key);
            try {
                lock.lock();
                final Queue<PluginTaskExecution> possiblyEmptyQueue = taskQueues.get(key);
                if (possiblyEmptyQueue.isEmpty()) {
                    taskQueues.remove(key);
                    return true;
                }
            } finally {
                lock.unlock();
            }
            return false;
        }

        private void executeDoneTask(@NotNull final PluginTaskExecution task) {

            try {
                final PluginTaskOutput outputObject = task.getOutputObject();
                if (outputObject == null) {
                    return;
                }

                final PluginTaskContext pluginContext = task.getPluginContext();
                if (pluginContext instanceof PluginTaskPost) {
                    final PluginTaskPost pluginPost = (PluginTaskPost) pluginContext;
                    //noinspection unchecked: generics extends a PluginTaskOutput
                    pluginPost.pluginPost(outputObject);
                }
                if (outputObject.isAsync()) {
                    outputObject.resetAsyncStatus();
                }
            } catch (final Throwable t) {
                Exceptions.rethrowError("Exception at extension post", t);
            }
        }

        private void executeTask(@NotNull final PluginTaskExecution task) {

            final PluginTaskOutput output = runTask(task);

            //noinspection unchecked: generics extends a PluginTaskOutput
            task.setOutputObject(output);

            if (output.isAsync()) {
                //handle async result

                task.markAsAsync();

                final ListenableFuture<Boolean> asyncFuture = output.getAsyncFuture();

                Preconditions.checkNotNull(asyncFuture, "Async future cannot be null for an async task");

                Futures.addCallback(asyncFuture, new FutureCallback<Boolean>() {
                    @Override
                    public void onSuccess(@Nullable final Boolean result) {
                        //mark the task as done and increment the semaphore to make sure the thread runs
                        task.markAsDone();
                        semaphore.release();
                    }

                    @Override
                    public void onFailure(@NotNull final Throwable t) {
                        Exceptions.rethrowError("Exception at PluginTaskExecutor", t);
                        task.markAsDone();
                        semaphore.release();
                    }
                    //the queue executor cannot be passed here, because it is spinning or blocked all the time
                    // therefore a new task might never be executed.
                    //A direct executor is the choice here, because it can run the callback even if resume is called
                    // in the same thread as async without (does not add a new task to the executor service)
                }, MoreExecutors.directExecutor());
            } else {
                //directly execute result function
                task.markAsDone();

                executeDoneTask(task);
            }
        }

        @NotNull
        private PluginTaskOutput runTask(@NotNull final PluginTaskExecution task) {
            final Thread thread = Thread.currentThread();
            final ClassLoader contextClassLoader = thread.getContextClassLoader();
            try {

                final PluginTask pluginTask = task.getPluginTask();
                thread.setContextClassLoader(pluginTask.getPluginClassLoader());
                final PluginTaskOutput output;
                if (pluginTask instanceof PluginInOutTask) {
                    output = runInOutTask(task, (PluginInOutTask) pluginTask);
                } else if (pluginTask instanceof PluginInTask) {
                    output = runInTask(task, (PluginInTask) pluginTask);
                } else if (pluginTask instanceof PluginOutTask) {
                    output = runOutTask(task, (PluginOutTask) pluginTask);
                } else {
                    throw new IllegalArgumentException("Unknown task type for extension task queue");
                }
                return output;
            } finally {
                thread.setContextClassLoader(contextClassLoader);
            }
        }

        @NotNull
        private PluginTaskOutput runOutTask(@NotNull final PluginTaskExecution task, final PluginOutTask pluginTask) {
            //noinspection unchecked: cast is safe because accept has generics that extend PluginTaskOutput
            return (PluginTaskOutput) pluginTask.apply(task.getOutputObject());
        }

        @NotNull
        private PluginTaskOutput runInTask(@NotNull final PluginTaskExecution task, @NotNull final PluginInTask pluginTask) {
            //noinspection unchecked: cast is safe because accept has generics that extend PluginTaskOutput
            pluginTask.accept(task.getInputObject());
            return DefaultPluginTaskOutput.getInstance();
        }

        @NotNull
        private PluginTaskOutput runInOutTask(@NotNull final PluginTaskExecution task, final PluginInOutTask pluginTask) {
            //noinspection unchecked: cast is safe because apply has generics that extend PluginTaskOutput
            return (PluginTaskOutput) pluginTask.apply(task.getInputObject(), task.getOutputObject());
        }
    }
}
