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
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class AllItemsFetchCallbackTest {

    @Test
    public void createsCursor() {
        final AllItemsFetchCallback<Object, Object> allItemsFetchCallback = new AllItemsFetchCallback<>() {

            @Override
            protected @NotNull ListenableFuture<MultipleChunkResult<Object>> persistenceCall(final @NotNull ChunkCursor chunkCursor) {

                assertNotNull(chunkCursor);
                return SettableFuture.create();
            }

            @Override
            protected @NotNull Collection<Object> transform(@NotNull Object o) {
                return Collections.EMPTY_LIST;
            }
        };

        allItemsFetchCallback.fetchNextResults(null);
    }

    @Test
    public void partiallyFinished() throws ExecutionException, InterruptedException {

        final AllItemsFetchCallback<String, String> allItemsFetchCallback = new AllItemsFetchCallback<>() {
            @Override
            protected @NotNull ListenableFuture<MultipleChunkResult<String>> persistenceCall(final @NotNull ChunkCursor chunkCursor) {

                final BucketChunkResult<String> one = new BucketChunkResult<>("1", true, "last", 1);
                final BucketChunkResult<String> two = new BucketChunkResult<>("2", false, "two", 2);

                return Futures.immediateFuture(new MultipleChunkResult<>(Map.of(1, one, 2, two)));
            }

            @Override
            protected @NotNull Collection<String> transform(final @NotNull String s) {
                return ImmutableList.of(s);
            }
        };

        final ChunkResult<String> result = allItemsFetchCallback.fetchNextResults(new ChunkCursor()).get();
        assertFalse(result.isFinished());
        assertTrue(result.getCursor().getFinishedBuckets().contains(1));
        assertEquals(1, result.getCursor().getFinishedBuckets().size());
        assertEquals("last", result.getCursor().getLastKeys().get(1));
        assertEquals("two", result.getCursor().getLastKeys().get(2));
    }

    @Test
    public void continues() throws ExecutionException, InterruptedException {

        final AllItemsFetchCallback<String, String> allItemsFetchCallback = new AllItemsFetchCallback<>() {
            @Override
            protected @NotNull ListenableFuture<MultipleChunkResult<String>> persistenceCall(final @NotNull ChunkCursor chunkCursor) {

                final BucketChunkResult<String> one = new BucketChunkResult<>("1", false, "last", 1);
                final BucketChunkResult<String> two = new BucketChunkResult<>("2", false, "two", 2);

                return Futures.immediateFuture(new MultipleChunkResult<>(Map.of(1, one, 2, two)));
            }

            @Override
            protected @NotNull Collection<String> transform(final @NotNull String s) {
                return ImmutableList.of(s);
            }
        };

        final ChunkResult<String> result = allItemsFetchCallback.fetchNextResults(new ChunkCursor()).get();
        assertFalse(result.isFinished());
        assertTrue(result.getCursor().getFinishedBuckets().isEmpty());
        assertEquals("last", result.getCursor().getLastKeys().get(1));
        assertEquals("two", result.getCursor().getLastKeys().get(2));
    }

    @Test
    public void finished() throws ExecutionException, InterruptedException {

        final AllItemsFetchCallback<String, String> allItemsFetchCallback = new AllItemsFetchCallback<>() {
            @Override
            protected @NotNull ListenableFuture<MultipleChunkResult<String>> persistenceCall(final @NotNull ChunkCursor chunkCursor) {

                final BucketChunkResult<String> one = new BucketChunkResult<>("1", true, "last", 1);
                final BucketChunkResult<String> two = new BucketChunkResult<>("2", true, "two", 2);

                return Futures.immediateFuture(new MultipleChunkResult<>(Map.of(1, one, 2, two)));
            }

            @Override
            protected @NotNull Collection<String> transform(final @NotNull String s) {
                return ImmutableList.of(s);
            }
        };

        final ChunkResult<String> result = allItemsFetchCallback.fetchNextResults(new ChunkCursor()).get();
        assertTrue(result.isFinished());
        assertTrue(result.getCursor().getFinishedBuckets().contains(1));
        assertTrue(result.getCursor().getFinishedBuckets().contains(2));
        assertEquals(2, result.getCursor().getFinishedBuckets().size());
        assertEquals("last", result.getCursor().getLastKeys().get(1));
        assertEquals("two", result.getCursor().getLastKeys().get(2));
    }
}