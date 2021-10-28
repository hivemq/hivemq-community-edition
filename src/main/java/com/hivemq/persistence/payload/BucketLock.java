package com.hivemq.persistence.payload;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Lukas Brandl
 */
public class BucketLock {

    private final Lock @NotNull [] locks;
    private final int bucketCount;

    public BucketLock(final int bucketCount) {
        locks = new Lock[bucketCount];
        this.bucketCount = bucketCount;
        for (int i = 0; i < bucketCount; i++) {
            locks[i] = new ReentrantLock();
        }
    }


    public int getBucketCount() {
        return bucketCount;
    }


    public void accessBucketByPaloadId(final @NotNull long payloadId, final @NotNull BucketAccessCallback callback) {
        checkNotNull(payloadId);
        final int index = BucketUtils.getBucket(Long.toString(payloadId), locks.length);
        accessBucket(index, callback);
    }

    public void accessBucket(final int index, final @NotNull BucketAccessCallback callback) {
        final Lock lock = locks[index];
        lock.lock();
        try {
            callback.call();
        } finally {
            lock.unlock();
        }
    }

    @FunctionalInterface
    interface BucketAccessCallback {
        void call();
    }
}
