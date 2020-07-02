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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings({"NullabilityAnnotations"})
public class AsyncLocalChunkIteratorTest {

    private AsyncLocalChunkIterator<String> asyncIterator;
    private TestFetchCallback fetchCallback;
    private TestItemCallback itemCallback;
    private ExecutorService executorService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        executorService = Executors.newFixedThreadPool(2);
        fetchCallback = new TestFetchCallback(executorService);
        itemCallback = new TestItemCallback();
        asyncIterator = new AsyncLocalChunkIterator<>(fetchCallback, itemCallback, executorService);
    }


    @Test(timeout = 15_000, expected = RuntimeException.class)
    public void test_fetch_failed() throws Throwable {

        fetchCallback.setException(new RuntimeException("test-exception"));

        asyncIterator.fetchAndIterate();

        try {
            asyncIterator.getFetchFuture().get();
        } catch (final Exception e) {
            throw e.getCause();
        }
    }

    @Test(timeout = 15_000, expected = RuntimeException.class)
    public void test_failed() throws Throwable {

        fetchCallback.setException(new RuntimeException("test-exception"));

        asyncIterator.fetchAndIterate();

        try {
            asyncIterator.getFinishedFuture().get();
        } catch (final Exception e) {
            throw e.getCause();
        }
    }

    @Test(timeout = 15_000, expected = NullPointerException.class)
    public void test_fetch_result_null() throws Throwable {

        asyncIterator = new AsyncLocalChunkIterator<>((cursor) -> Futures.immediateFuture(null), itemCallback, executorService);

        asyncIterator.fetchAndIterate();

        try {
            asyncIterator.getFinishedFuture().get();
        } catch (final Exception e) {
            throw e.getCause();
        }
    }

    @Test(timeout = 15_000)
    public void test_iterate_multiple() throws Exception {

        fetchCallback.setChunks(new ArrayDeque<>(List.of(List.of("a1", "b", "c1"), List.of("a2", "b", "d", "e"))));

        asyncIterator.fetchAndIterate();

        asyncIterator.getFinishedFuture().get();

        assertEquals(7, itemCallback.getItems().size());
        assertEquals(itemCallback.getItems(), ImmutableList.of("a1", "b", "c1", "a2", "b", "d", "e"));
    }

    @Test(timeout = 15_000)
    public void test_iterate_emtpy_result() throws Exception {

        fetchCallback.setChunks(new ArrayDeque<>(List.of(List.of())));

        asyncIterator.fetchAndIterate();

        asyncIterator.getFetchFuture().get();
        asyncIterator.getFinishedFuture().get();

        assertEquals(0, itemCallback.getItems().size());
    }

    @Test(timeout = 15_000)
    public void test_abort_on_first_item() throws Exception {

        fetchCallback.setChunks(new ArrayDeque<>(List.of(List.of("a1", "b", "c1"), List.of("d", "e"))));

        itemCallback.setAbort(true);

        asyncIterator.fetchAndIterate();

        asyncIterator.getFinishedFuture().get();

        assertTrue("Should only contain max 3 item, was " + itemCallback.getItems().size() + " items", itemCallback.getItems().size() <= 3);
    }

    private static class TestItemCallback implements AsyncIterator.ItemCallback<String> {

        final List<String> items = Collections.synchronizedList(new ArrayList<>());

        private boolean abort = false;

        @Override
        @NotNull
        public ListenableFuture<Boolean> onItems(@NotNull final Collection<String> items) {
            this.items.addAll(items);
            return Futures.immediateFuture(!abort);
        }

        public List<String> getItems() {
            return items;
        }

        public void setAbort(final boolean abort) {
            this.abort = abort;
        }
    }

    private static class TestFetchCallback implements FetchCallback<String> {

        private final AtomicReference<Queue<Collection<String>>> chunks = new AtomicReference<>();
        private final AtomicBoolean block = new AtomicBoolean(false);
        private final CountDownLatch blockingLatch = new CountDownLatch(2);
        private final ExecutorService executorService;

        private Exception exception = null;

        private TestFetchCallback(final ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public @NotNull ListenableFuture<ChunkResult<String>> fetchNextResults(@Nullable final ChunkCursor cursor) {

            if (exception != null) {
                return Futures.immediateFailedFuture(exception);
            }

            final SettableFuture<ChunkResult<String>> resultFuture = SettableFuture.create();
            executorService.submit(() -> {

                final Queue<Collection<String>> chunkQueue = chunks.get();
                if (block.get()) {
                    blockingLatch.countDown();
                    while (block.get()) {
                        try {
                            Thread.sleep(50);
                        } catch (final InterruptedException e) {
                            break;
                        }
                    }
                }
                final Collection<String> nextChunk = chunkQueue.poll();
                if (nextChunk == null) {
                    resultFuture.set(new ChunkResult<>(List.of(), cursor, true));
                } else {
                    resultFuture.set(new ChunkResult<>(nextChunk, cursor, chunkQueue.peek() == null));
                }
            });

            return resultFuture;
        }

        public void setChunks(final Queue<Collection<String>> chunks) {
            this.chunks.set(chunks);
        }

        public void setBlockDuringFetch(final boolean block) {
            this.block.set(block);
        }

        public CountDownLatch getBlockingLatch() {
            return blockingLatch;
        }

        public void setException(final Exception exception) {
            this.exception = exception;
        }
    }
}
