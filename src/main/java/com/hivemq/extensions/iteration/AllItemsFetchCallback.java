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
