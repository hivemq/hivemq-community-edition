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

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ChunkerTest {


    private int bucketCount;

    @Before
    public void setUp() throws Exception {
        bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(4);
    }

    @After
    public void tearDown() throws Exception {
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(bucketCount);

    }

    @Test
    public void calculatesMaxResults() {
        final Chunker chunker = new Chunker();

        chunker.getAllLocalChunk(new ChunkCursor(), 4, new Chunker.SingleWriterCall<String>() {
            @Override
            public ListenableFuture<@NotNull BucketChunkResult<Map<String, String>>> call(final int bucket, final @NotNull String lastKey, final int maxResults) {
                assertEquals(1, maxResults);
                return Futures.immediateFuture(new BucketChunkResult<>(Map.of(), true, "last", bucket));
            }
        });
    }

    @Test
    public void callsAllBuckets() throws Exception {
        final int[] counter = {0};
        final Chunker chunker = new Chunker();

        final MultipleChunkResult<Map<String, @NotNull String>> multi = chunker.getAllLocalChunk(new ChunkCursor(), 4, new Chunker.SingleWriterCall<String>() {
            @Override
            public ListenableFuture<@NotNull BucketChunkResult<Map<String, String>>> call(final int bucket, final @NotNull String lastKey, final int maxResults) {

                counter[0]++;
                return Futures.immediateFuture(new BucketChunkResult<>(Map.of(), false, "last", bucket));
            }
        }).get();

        final Map<Integer, BucketChunkResult<Map<String, @NotNull String>>> values = multi.getValues();
        assertEquals(4, values.size());
        final Set<Integer> buckets = values.keySet();
        assertTrue(buckets.contains(0));
        assertTrue(buckets.contains(1));
        assertTrue(buckets.contains(2));
        assertTrue(buckets.contains(3));
        assertFalse(values.get(0).isFinished());
        assertFalse(values.get(1).isFinished());
        assertFalse(values.get(2).isFinished());
        assertFalse(values.get(3).isFinished());
        assertEquals(4, counter[0]);
    }

    @Test
    public void doesNotCallFinishedBuckets() throws Exception {
        final int[] counter = {0};
        final Chunker chunker = new Chunker();

        final ChunkCursor cursor = new ChunkCursor(new HashMap<>(), ImmutableSet.of(1, 3));
        final MultipleChunkResult<Map<String, @NotNull String>> multi = chunker.getAllLocalChunk(cursor, 4, new Chunker.SingleWriterCall<String>() {
            @Override
            public ListenableFuture<@NotNull BucketChunkResult<Map<String, String>>> call(final int bucket, final @NotNull String lastKey, final int maxResults) {
                counter[0]++;
                return Futures.immediateFuture(new BucketChunkResult<>(Map.of(), false, "last", bucket));
            }
        }).get();

        final Map<Integer, BucketChunkResult<Map<String, @NotNull String>>> values = multi.getValues();
        assertEquals(4, values.size());
        final Set<Integer> buckets = values.keySet();
        assertTrue(buckets.contains(0));
        assertTrue(buckets.contains(1));
        assertTrue(buckets.contains(2));
        assertTrue(buckets.contains(3));
        assertFalse(values.get(0).isFinished());
        assertTrue(values.get(1).isFinished());
        assertFalse(values.get(2).isFinished());
        assertTrue(values.get(3).isFinished());
        assertEquals(2, counter[0]);
    }
}