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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import org.eclipse.collections.api.tuple.primitive.LongIntPair;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import oshi.annotation.concurrent.NotThreadSafe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Daniel Krüger
 * <p>
 * Most methods are NOT thread-safe and the exclusive access on the bucket must be secured by the caller
 * The reason is that the caller (primarly PublishPayloadPersistence) calls often multiple methods and the lock must cover all sequential calls to methods
 */
public class PayloadReferenceCounterRegistryImpl implements PayloadReferenceCounterRegistry {

    private final int numberBuckets;
    private final @NotNull BucketLock bucketLock;
    private final @NotNull LongIntHashMap @NotNull [] buckets;

    PayloadReferenceCounterRegistryImpl(final @NotNull BucketLock bucketLock) {
        this.numberBuckets = bucketLock.getBucketCount();
        this.bucketLock = bucketLock;
        this.buckets = new LongIntHashMap[numberBuckets];
        for (int i = 0; i < numberBuckets; i++) {
            this.buckets[i] = new LongIntHashMap();
        }
    }

    @NotThreadSafe
    public @Nullable Integer get(final long payloadId) {
        final LongIntHashMap map = buckets[calcBucket(payloadId)];
        if (map.containsKey(payloadId)) {
            return map.get(payloadId);
        } else {
            return null;
        }
    }

    @NotThreadSafe
    public int add(final long payloadId, final int referenceCount) {
        final LongIntHashMap map = buckets[calcBucket(payloadId)];
        final int currentCount = map.get(payloadId);
        map.put(payloadId, currentCount + referenceCount);
        return currentCount + referenceCount;
    }

    @NotThreadSafe
    public int put(final long payloadId, final int referenceCount) {
        final LongIntHashMap map = buckets[calcBucket(payloadId)];
        map.put(payloadId, referenceCount);
        return referenceCount;
    }

    @NotThreadSafe
    public int increment(final long payloadId) {
        return add(payloadId, 1);
    }

    @NotThreadSafe
    public int decrement(final long payloadId) {
        final int bucketIndex = calcBucket(payloadId);
        final LongIntHashMap map = buckets[bucketIndex];

        final int i = map.get(payloadId);
        if (i == 0) {
            // return a negative value, but dont set it in the registry
            return -1;
        } else {
            map.put(payloadId, i - 1);
            return i - 1;
        }

    }

    /**
     * This method is thread safe
     *
     * @return a map that contains all entries
     */
    @ThreadSafe
    public synchronized @NotNull Map<Long, Integer> getAll() {
        final ConcurrentHashMap<Long, Integer> completeMap = new ConcurrentHashMap<>();
        for (int i = 0; i < numberBuckets; i++) {
            final int finalI = i;
            bucketLock.accessBucket(finalI, () -> {
                for (LongIntPair longIntPair : buckets[finalI].keyValuesView()) {
                    completeMap.put(longIntPair.getOne(), longIntPair.getTwo());
                }
            });
        }
        return completeMap;
    }

    @ThreadSafe
    public int size() {
        final AtomicInteger sum = new AtomicInteger();
        for (int i = 0; i < numberBuckets; i++) {
            final int finalI = i;
            bucketLock.accessBucket(finalI, () ->
                    sum.addAndGet(buckets[finalI].size())
            );
        }
        return sum.get();
    }


    @NotThreadSafe
    public void remove(final long payloadId) {
        final LongIntHashMap map = buckets[calcBucket(payloadId)];
        if (map == null) {
            return;
        }
        map.remove(payloadId);
    }


    private int calcBucket(final @NotNull long pubCounter) {
        return BucketUtils.getBucket(Long.toString(pubCounter), numberBuckets);
    }
}
