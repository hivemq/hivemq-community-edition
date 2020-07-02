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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Georg Held
 */
@Singleton
public class Chunker {

    private final int bucketCount;

    @Inject
    public Chunker() {
        bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();
    }

    public <T> @NotNull ListenableFuture<MultipleChunkResult<Map<String, @NotNull T>>> getAllLocalChunk(final @NotNull ChunkCursor cursor,
                                                                                                        final int maxChunkSize,
                                                                                                        final @NotNull SingleWriterCall<T> singleWriterCall) {
        try {
            checkNotNull(cursor, "Cursor must not be null");
            checkNotNull(singleWriterCall, "Single writer call must not be null");


            final ImmutableList.Builder<ListenableFuture<@NotNull BucketChunkResult<Map<String, T>>>> builder = ImmutableList.builder();

            final int maxResults = maxChunkSize / (bucketCount - cursor.getFinishedBuckets().size());
            for (int i = 0; i < bucketCount; i++) {
                //skip already finished buckets
                if (!cursor.getFinishedBuckets().contains(i)) {
                    final String lastKey = cursor.getLastKeys().get(i);
                    builder.add(singleWriterCall.call(i, lastKey, maxResults));
                }
            }


            return Futures.transform(Futures.allAsList(builder.build()), allBucketsResult -> {
                Preconditions.checkNotNull(allBucketsResult, "Iteration result from all buckets cannot be null");

                final ImmutableMap.Builder<Integer, BucketChunkResult<Map<String, T>>> resultBuilder = ImmutableMap.builder();
                for (final BucketChunkResult<Map<String, T>> bucketResult : allBucketsResult) {
                    resultBuilder.put(bucketResult.getBucketIndex(), bucketResult);
                }

                for (final Integer finishedBucketId : cursor.getFinishedBuckets()) {
                    resultBuilder.put(finishedBucketId, new BucketChunkResult<>(Map.of(), true, cursor.getLastKeys().get(finishedBucketId), finishedBucketId));
                }

                return new MultipleChunkResult<>(resultBuilder.build());

            }, MoreExecutors.directExecutor());

        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }
    }

    public interface SingleWriterCall<T> {
        ListenableFuture<@NotNull BucketChunkResult<Map<String, T>>> call(final int bucket, final @NotNull String lastKey, final int maxResults);
    }
}
