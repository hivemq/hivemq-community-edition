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
package com.hivemq.persistence.local.memory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.hivemq.annotations.ExecuteInSingleWriter;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.local.xodus.PublishTopicTree;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.PublishUtil;
import com.hivemq.util.ThreadPreConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * @author Lukas Brandl
 */
@Singleton
public class RetainedMessageMemoryLocalPersistence implements RetainedMessageLocalPersistence {

    private static final Logger log = LoggerFactory.getLogger(RetainedMessageMemoryLocalPersistence.class);

    @VisibleForTesting
    final @NotNull AtomicLong currentMemorySize = new AtomicLong();

    @VisibleForTesting
    @NotNull
    final PublishTopicTree[] topicTrees;

    final private @NotNull Map<String, RetainedMessage>[] buckets;

    private final int bucketCount;

    @Inject
    public RetainedMessageMemoryLocalPersistence(@NotNull final MetricRegistry metricRegistry) {
        bucketCount = InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get();

        //noinspection unchecked
        buckets = new HashMap[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new HashMap<>();
        }
        topicTrees = new PublishTopicTree[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            topicTrees[i] = new PublishTopicTree();
        }

        metricRegistry.register(
                HiveMQMetrics.RETAINED_MESSAGES_MEMORY_PERSISTENCE_TOTAL_SIZE.name(),
                (Gauge<Long>) currentMemorySize::get);
    }

    @Override
    public long size() {
        int sum = 0;
        for (final Map<String, RetainedMessage> bucket : buckets) {
            sum += bucket.size();
        }
        return sum;
    }

    @ExecuteInSingleWriter
    @Override
    public void clear(final int bucketIndex) {
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);
        topicTrees[bucketIndex] = new PublishTopicTree();

        final Map<String, RetainedMessage> bucket = buckets[bucketIndex];
        for (final RetainedMessage retainedMessage : bucket.values()) {
            currentMemorySize.addAndGet(-retainedMessage.getEstimatedSizeInMemory());
        }
        bucket.clear();
    }

    @ExecuteInSingleWriter
    @Override
    public void remove(@NotNull final String topic, final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        topicTrees[bucketIndex].remove(topic);
        final Map<String, RetainedMessage> bucket = buckets[bucketIndex];
        final RetainedMessage retainedMessage = bucket.remove(topic);
        if (retainedMessage != null) {
            currentMemorySize.addAndGet(-retainedMessage.getEstimatedSizeInMemory());
        }
    }

    @ExecuteInSingleWriter
    @Override
    public @Nullable RetainedMessage get(@NotNull final String topic, final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, RetainedMessage> bucket = buckets[bucketIndex];
        final RetainedMessage retainedMessage = bucket.get(topic);
        if (retainedMessage == null) {
            return null;
        }

        if (PublishUtil.checkExpiry(retainedMessage.getTimestamp(), retainedMessage.getMessageExpiryInterval())) {
            return null;
        }
        final RetainedMessage copy = retainedMessage.copyWithoutPayload();
        return retainedMessage;
    }

    @ExecuteInSingleWriter
    @Override
    public void put(
            @NotNull final RetainedMessage retainedMessage, @NotNull final String topic, final int bucketIndex) {
        checkNotNull(topic, "Topic must not be null");
        checkNotNull(retainedMessage, "Retained message must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, RetainedMessage> bucket = buckets[bucketIndex];
        final RetainedMessage previousMessage = bucket.put(topic, retainedMessage);
        if (previousMessage != null) {
            currentMemorySize.addAndGet(-previousMessage.getEstimatedSizeInMemory());
        }
        currentMemorySize.addAndGet(retainedMessage.getEstimatedSizeInMemory());
        topicTrees[bucketIndex].add(topic);
    }

    @NotNull
    @ExecuteInSingleWriter
    @Override
    public Set<String> getAllTopics(@NotNull final String subscription, final int bucketIndex) {
        checkArgument(bucketIndex >= 0 && bucketIndex < bucketCount, "Bucket index out of range");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        return topicTrees[bucketIndex].get(subscription);
    }

    @ExecuteInSingleWriter
    @Override
    public void cleanUp(final int bucketIndex) {
        checkArgument(bucketIndex >= 0 && bucketIndex < bucketCount, "Bucket index out of range");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Map<String, RetainedMessage> bucket = buckets[bucketIndex];
        bucket.entrySet().removeIf(entry -> {
            if (entry == null) {
                return false;
            }
            final RetainedMessage retainedMessage = entry.getValue();
            final String topic = entry.getKey();
            if (PublishUtil.checkExpiry(retainedMessage.getTimestamp(), retainedMessage.getMessageExpiryInterval())) {
                currentMemorySize.addAndGet(-retainedMessage.getEstimatedSizeInMemory());
                topicTrees[bucketIndex].remove(topic);
                return true;
            }
            return false;
        });
    }

    // in contrast to the file persistence method we already have everything in memory. The sizing and pagination are ignored.
    @Override
    public @NotNull BucketChunkResult<Map<String, @NotNull RetainedMessage>> getAllRetainedMessagesChunk(final int bucketIndex,
                                                                                                         final @Nullable String ignored,
                                                                                                         final int alsoIgnored) {

        final ImmutableMap<String, RetainedMessage> collectedRetainedMessages = buckets[bucketIndex].entrySet()
                .stream()
                .map(entry -> {
                    final String topic = entry.getKey();
                    final RetainedMessage retainedMessage = entry.getValue();

                    // ignore messages with exceeded message expiry interval
                    if (PublishUtil.checkExpiry(retainedMessage.getTimestamp(), retainedMessage.getMessageExpiryInterval())) {
                        return null;
                    }

                    final Long payloadId = retainedMessage.getPublishId();
                    if (payloadId == null) {
                        log.warn("Could not dereference payload for retained message on topic \"{}\" as payload was null.", topic);
                        return null;
                    }

                    return new AbstractMap.SimpleEntry<>(topic, retainedMessage);

                })
                .filter(entry -> !Objects.isNull(entry))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        return new BucketChunkResult<>(collectedRetainedMessages, true, null, bucketIndex);
    }

    @Override
    public void iterate(@NotNull final ItemCallback callback) {
        throw new UnsupportedOperationException(
                "Iterate is only used for migrations which are not needed for memory persistences");
    }

    @Override
    public void bootstrapPayloads() {
        // noop
    }

    @Override
    public void closeDB(final int bucketIndex) {
        // noop
    }
}
