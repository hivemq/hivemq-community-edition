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
package com.hivemq.persistence.payload;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import org.eclipse.collections.api.tuple.primitive.LongIntPair;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Most methods are NOT thread-safe and the exclusive access on the bucket must be secured by the caller
 * The reason is that the caller (primarily PublishPayloadPersistence) calls often multiple methods and the lock
 * must cover all sequential calls to methods
 */
@NotThreadSafe
public class PayloadReferenceCounterRegistryImpl implements PayloadReferenceCounterRegistry {

    private final @NotNull BucketLock bucketLock;
    private final @NotNull LongIntHashMap @NotNull [] buckets;

    PayloadReferenceCounterRegistryImpl(final @NotNull BucketLock bucketLock) {
        this.bucketLock = bucketLock;
        this.buckets = new LongIntHashMap[bucketLock.getBucketCount()];
        for (int i = 0; i < buckets.length; i++) {
            this.buckets[i] = new LongIntHashMap();
        }
    }

    @Override
    public int get(final long payloadId) {
        final LongIntHashMap map = buckets[bucketIndexForPayloadId(payloadId)];
        return map.getIfAbsent(payloadId, UNKNOWN_PAYLOAD);
    }

    @Override
    public int getAndIncrement(final long payloadId) {
        final LongIntHashMap map = buckets[bucketIndexForPayloadId(payloadId)];
        final int previousValue = map.getIfAbsent(payloadId, UNKNOWN_PAYLOAD);
        if (previousValue == UNKNOWN_PAYLOAD) {
            map.put(payloadId, 1);
        } else {
            map.put(payloadId, previousValue + 1);
        }
        return previousValue;
    }

    @Override
    public int decrementAndGet(final long payloadId) {
        final int bucketIndex = bucketIndexForPayloadId(payloadId);
        final LongIntHashMap map = buckets[bucketIndex];
        final int currentValue = map.getIfAbsent(payloadId, UNKNOWN_PAYLOAD);
        if (currentValue == UNKNOWN_PAYLOAD) {
            return UNKNOWN_PAYLOAD;
        }
        if (currentValue == 0) {
            return REF_COUNT_ALREADY_ZERO;
        }
        final int newValue = currentValue - 1;
        map.put(payloadId, newValue);
        return newValue;
    }

    @Override
    public void delete(final long payloadId) {
        final LongIntHashMap map = buckets[bucketIndexForPayloadId(payloadId)];
        if (map == null) {
            return;
        }
        map.remove(payloadId);
    }

    @ThreadSafe
    @Override
    public @NotNull ImmutableMap<Long, Integer> getAll() {
        final ImmutableMap.Builder<Long, Integer> builder = ImmutableMap.builder();
        for (int i = 0; i < buckets.length; i++) {
            final int bucketIndex = i;
            bucketLock.accessBucket(bucketIndex, () -> {
                for (final LongIntPair longIntPair : buckets[bucketIndex].keyValuesView()) {
                    builder.put(longIntPair.getOne(), longIntPair.getTwo());
                }
            });
        }
        return builder.build();
    }

    @ThreadSafe
    @Override
    public int size() {
        final AtomicInteger sum = new AtomicInteger();
        for (int i = 0; i < buckets.length; i++) {
            final int bucketIndex = i;
            bucketLock.accessBucket(bucketIndex, () -> sum.addAndGet(buckets[bucketIndex].size()));
        }
        return sum.get();
    }

    private int bucketIndexForPayloadId(final long payloadId) {
        return BucketUtils.getBucket(Long.toString(payloadId), buckets.length);
    }
}
