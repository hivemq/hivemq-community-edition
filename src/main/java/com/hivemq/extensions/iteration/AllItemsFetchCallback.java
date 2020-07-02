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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * @author Georg Held
 */
public abstract class AllItemsFetchCallback<R, I> implements FetchCallback<R> {

    @Override
    public @NotNull ListenableFuture<ChunkResult<R>> fetchNextResults(final @Nullable ChunkCursor cursor) {

        final ListenableFuture<MultipleChunkResult<I>> persistenceFuture = persistenceCall(cursor != null ? cursor : new ChunkCursor());

        return Futures.transform(persistenceFuture, input -> {
            Preconditions.checkNotNull(input, "Chunk result cannot be null");
            return convertToChunkResult(input);
        }, MoreExecutors.directExecutor());
    }

    public @NotNull ChunkResult<R> convertToChunkResult(final @NotNull MultipleChunkResult<I> input) {
        final ImmutableList.Builder<R> results = ImmutableList.builder();
        final ImmutableMap.Builder<Integer, String> lastKeys = ImmutableMap.builder();
        final ImmutableSet.Builder<Integer> finishedBuckets = ImmutableSet.builder();

        boolean finished = true;
        final Map<Integer, BucketChunkResult<I>> values = input.getValues();

        for (final Map.Entry<Integer, BucketChunkResult<I>> bucketResult : values.entrySet()) {
            final BucketChunkResult<I> bucketChunkResult = bucketResult.getValue();

            if (bucketChunkResult.isFinished()) {
                finishedBuckets.add(bucketChunkResult.getBucketIndex());
            } else {
                finished = false;
            }

            if (bucketChunkResult.getLastKey() != null) {
                lastKeys.put(bucketChunkResult.getBucketIndex(), bucketChunkResult.getLastKey());
            }

            results.addAll(transform(bucketChunkResult.getValue()));
        }

        return new ChunkResult<>(results.build(), new ChunkCursor(lastKeys.build(), finishedBuckets.build()), finished);
    }

    protected abstract @NotNull ListenableFuture<MultipleChunkResult<I>> persistenceCall(final @NotNull ChunkCursor chunkCursor);

    protected abstract @NotNull Collection<R> transform(final @NotNull I i);
}
