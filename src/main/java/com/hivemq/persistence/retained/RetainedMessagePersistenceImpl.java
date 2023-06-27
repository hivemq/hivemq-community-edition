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
package com.hivemq.persistence.retained;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.iteration.ChunkCursor;
import com.hivemq.extensions.iteration.Chunker;
import com.hivemq.extensions.iteration.MultipleChunkResult;
import com.hivemq.persistence.AbstractPersistence;
import com.hivemq.persistence.ProducerQueues;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.util.FutureUtils;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@LazySingleton
public class RetainedMessagePersistenceImpl extends AbstractPersistence implements RetainedMessagePersistence {

    private final @NotNull RetainedMessageLocalPersistence localPersistence;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull ProducerQueues singleWriter;
    private final @NotNull Chunker chunker;

    @Inject
    RetainedMessagePersistenceImpl(
            final @NotNull RetainedMessageLocalPersistence localPersistence,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull Chunker chunker) {

        this.localPersistence = localPersistence;
        this.payloadPersistence = payloadPersistence;

        singleWriter = singleWriterService.getRetainedMessageQueue();

        this.chunker = chunker;
    }

    @Override
    public @NotNull ListenableFuture<RetainedMessage> get(final @NotNull String topic) {
        try {
            checkNotNull(topic, "Topic must not be null");
            if (topic.contains("+") || topic.contains("#")) {
                throw new IllegalArgumentException(
                        "Topic contains wildcard characters. Call getWithWildcards method instead.");
            }
        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }

        return singleWriter.submit(topic, (bucketIndex) -> {
            final RetainedMessage retainedMessage = localPersistence.get(topic, bucketIndex);
            if (retainedMessage == null) {
                return null;
            }
            payloadPersistence.add(retainedMessage.getMessage(), retainedMessage.getPublishId());
            return retainedMessage;
        });
    }

    @Override
    public long size() {
        return localPersistence.size();
    }

    @Override
    public @NotNull ListenableFuture<Void> remove(final @NotNull String topic) {
        try {
            checkNotNull(topic, "Topic must not be null");

            return singleWriter.submit(topic, (bucketIndex) -> {
                localPersistence.remove(topic, bucketIndex);
                return null;
            });
        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }
    }

    @Override
    public @NotNull ListenableFuture<Void> persist(
            final @NotNull String topic, final @NotNull RetainedMessage retainedMessage) {
        try {
            checkNotNull(topic, "Topic must not be null");
            checkNotNull(retainedMessage, "Retained message must not be null");

            payloadPersistence.add(retainedMessage.getMessage(), retainedMessage.getPublishId());

            return singleWriter.submit(topic, (bucketIndex) -> {
                localPersistence.put(retainedMessage, topic, bucketIndex);
                return null;
            });

        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }
    }

    @Override
    public @NotNull ListenableFuture<Set<String>> getWithWildcards(final @NotNull String subscription) {
        try {
            checkNotNull(subscription, "Topic must not be null");
            if (!subscription.contains("+") && !subscription.contains("#")) {
                throw new IllegalArgumentException(
                        "Topic does not contain wildcard characters. Call get method instead.");
            }

            final List<ListenableFuture<Set<String>>> futures =
                    singleWriter.submitToAllBucketsParallel((bucketIndex) -> new HashSet<>(localPersistence.getAllTopics(
                            subscription,
                            bucketIndex)));
            return Futures.transform(Futures.allAsList(futures),
                    listOfSets -> listOfSets.stream().flatMap(Set::stream).collect(Collectors.toSet()),
                    MoreExecutors.directExecutor());
        } catch (final Throwable throwable) {
            return Futures.immediateFailedFuture(throwable);
        }
    }

    @Override
    public @NotNull ListenableFuture<Void> cleanUp(final int bucketIndex) {
        return singleWriter.submit(bucketIndex, (bucketIndex1) -> {
            localPersistence.cleanUp(bucketIndex1);
            return null;
        });
    }

    @Override
    public @NotNull ListenableFuture<Void> closeDB() {
        return closeDB(localPersistence, singleWriter);
    }

    @Override
    public @NotNull ListenableFuture<Void> clear() {
        final List<ListenableFuture<Void>> futureList = singleWriter.submitToAllBucketsParallel((bucketIndex) -> {
            localPersistence.clear(bucketIndex);
            return null;
        });
        return FutureUtils.voidFutureFromList(ImmutableList.copyOf(futureList));
    }

    @Override
    public @NotNull ListenableFuture<MultipleChunkResult<Map<String, @NotNull RetainedMessage>>> getAllLocalRetainedMessagesChunk(
            @NotNull ChunkCursor cursor) {
        return chunker.getAllLocalChunk(cursor,
                InternalConfigurations.PERSISTENCE_RETAINED_MESSAGES_MAX_CHUNK_MEMORY_BYTES,
                // Chunker.SingleWriterCall interface
                (bucket, lastKey, maxResults) ->
                        // actual single writer call
                        singleWriter.submit(bucket,
                                (bucketIndex) -> localPersistence.getAllRetainedMessagesChunk(bucketIndex,
                                        lastKey,
                                        maxResults)));
    }
}
