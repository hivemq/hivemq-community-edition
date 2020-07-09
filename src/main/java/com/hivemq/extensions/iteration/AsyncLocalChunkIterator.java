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
package com.hivemq.extensions.iteration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Christoph Schäbel
 */
public class AsyncLocalChunkIterator<V> implements AsyncIterator<V> {

    private final @NotNull FetchCallback<V> fetchCallback;
    private final @NotNull ResultBuffer<V> resultBuffer;
    private final @NotNull ItemCallback<V> itemCallback;
    private final @NotNull ExecutorService executorService;

    private final CompletableFuture<Void> finishedFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> fetchFuture = new CompletableFuture<>();

    private final AtomicBoolean iterating = new AtomicBoolean(false);
    private final AtomicBoolean aborted = new AtomicBoolean(false);

    private final Lock lock = new ReentrantLock();

    AsyncLocalChunkIterator(@NotNull final FetchCallback<V> fetchCallback,
                            @NotNull final ItemCallback<V> itemCallback,
                            @NotNull final ExecutorService executorService) {
        this.fetchCallback = fetchCallback;
        this.resultBuffer = new ResultBuffer<>((cursor, resultBuffer) -> {
            fetchNextChunk(cursor);
        });
        this.itemCallback = itemCallback;
        this.executorService = executorService;
    }

    @Override
    public void fetchAndIterate() {
        fetchNextChunk(null);
    }

    private void fetchNextChunk(@Nullable final ChunkCursor cursor) {
        final ListenableFuture<ChunkResult<V>> singleFuture = fetchCallback.fetchNextResults(cursor);
        Futures.addCallback(
                singleFuture, new ChunkResultFutureCallback<V>(resultBuffer, this, lock), executorService);
    }

    private synchronized void triggerIteration() {

        if (!iterating.compareAndSet(false, true)) {
            //protect from late arrivals
            return;
        }

        if (aborted.get()) {
            return;
        }

        // read from buffer
        final Collection<V> items = resultBuffer.getNextChunk();

        if (items == null) {
            if (fetchFuture.isDone()) {
                //no results available and all done
                resultBuffer.clean();
                finishedFuture.complete(null);
            }
            iterating.set(false);
            return;
        }

        callCallback(items);
    }

    private synchronized void callCallback(@NotNull final Collection<V> items) {
        if (aborted.get()) {
            return;
        }
        final ListenableFuture<Boolean> itemFuture = itemCallback.onItems(items);
        Futures.addCallback(itemFuture, new ChunkCallback<V>(this, resultBuffer, lock), executorService);
    }

    @NotNull
    public CompletableFuture<Void> getFinishedFuture() {
        return finishedFuture;
    }

    @NotNull
    @VisibleForTesting
    CompletableFuture<Void> getFetchFuture() {
        return fetchFuture;
    }

    private void abortExceptionally(@NotNull final Throwable t) {
        aborted.set(true);
        resultBuffer.clean();
        fetchFuture.completeExceptionally(t);
        finishedFuture.completeExceptionally(t);
    }

    private static class ChunkResultFutureCallback<V> implements FutureCallback<ChunkResult<V>> {

        private final @NotNull ResultBuffer<V> resultBuffer;
        private final @NotNull AsyncLocalChunkIterator<V> asyncLocalChunkIterator;
        private final @NotNull Lock lock;

        ChunkResultFutureCallback(@NotNull final ResultBuffer<V> resultBuffer,
                                  @NotNull final AsyncLocalChunkIterator<V> asyncLocalChunkIterator,
                                  @NotNull final Lock lock) {
            this.resultBuffer = resultBuffer;
            this.asyncLocalChunkIterator = asyncLocalChunkIterator;
            this.lock = lock;
        }

        @Override
        public void onSuccess(final ChunkResult<V> result) {

            if (asyncLocalChunkIterator.aborted.get()) {
                //the current iteration has been aborted, ignore results
                return;
            }

            if (result == null) {
                asyncLocalChunkIterator.abortExceptionally(new NullPointerException("chunk result cannot be null"));
                return;
            }

            lock.lock();
            try {
                //add chunk to buffer
                if (!result.getResults().isEmpty()) {
                    resultBuffer.addChunk(result);
                }

                if (result.isFinished()) {
                    asyncLocalChunkIterator.getFetchFuture().complete(null);
                }
            } finally {
                lock.unlock();
            }

            asyncLocalChunkIterator.triggerIteration();

        }

        @Override
        public void onFailure(@NotNull final Throwable t) {
            asyncLocalChunkIterator.abortExceptionally(t);
        }
    }

    private static class ChunkCallback<V> implements FutureCallback<Boolean> {

        private final @NotNull AsyncLocalChunkIterator<V> asyncLocalChunkIterator;
        private final @NotNull ResultBuffer<V> resultBuffer;
        private final @NotNull Lock lock;

        private ChunkCallback(@NotNull final AsyncLocalChunkIterator<V> asyncLocalChunkIterator,
                              @NotNull final ResultBuffer<V> resultBuffer,
                              @NotNull final Lock lock) {
            this.asyncLocalChunkIterator = asyncLocalChunkIterator;
            this.resultBuffer = resultBuffer;
            this.lock = lock;
        }

        @Override
        public void onSuccess(@Nullable final Boolean result) {
            if (result == null) {
                asyncLocalChunkIterator.abortExceptionally(new NullPointerException("callback result cannot be null"));
                return;
            }

            if (!result) {
                //iteration aborted
                asyncLocalChunkIterator.aborted.set(true);
                resultBuffer.clean();
                asyncLocalChunkIterator.getFinishedFuture().complete(null);
                return;
            }

            lock.lock();
            try {
                // read from buffer
                final Collection<V> items = resultBuffer.getNextChunk();

                if (items == null) {
                    asyncLocalChunkIterator.iterating.set(false);
                    //no results available anymore, but all chunks done
                    if (asyncLocalChunkIterator.getFetchFuture().isDone()) {
                        resultBuffer.clean();
                        asyncLocalChunkIterator.getFinishedFuture().complete(null);
                    }
                    return;
                }
                asyncLocalChunkIterator.callCallback(items);

            } finally {
                lock.unlock();
            }

        }

        @Override
        public void onFailure(@NotNull final Throwable t) {
            asyncLocalChunkIterator.abortExceptionally(t);
        }
    }
}
