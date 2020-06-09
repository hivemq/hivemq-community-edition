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

import com.google.common.cache.Cache;
import com.hivemq.util.Exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

/**
 * @author Lukas Brandl
 */
public class RemoveEntryTask implements Runnable {

    private final Cache<Long, byte[]> payloadCache;
    private final PublishPayloadLocalPersistence localPersistence;
    private final BucketLock bucketLock;
    private final Queue<RemovablePayload> removablePayloads;
    private final long removeDelay;
    private final ConcurrentHashMap<Long, AtomicLong> referenceCounter;
    private final long taskMaxDuration;

    public RemoveEntryTask(final Cache<Long, byte[]> payloadCache,
                           final PublishPayloadLocalPersistence localPersistence,
                           final BucketLock bucketLock,
                           final Queue<RemovablePayload> removablePayloads,
                           final long removeDelay,
                           final ConcurrentHashMap<Long, AtomicLong> referenceCounter,
                           final long taskMaxDuration) {

        this.payloadCache = payloadCache;
        this.localPersistence = localPersistence;
        this.bucketLock = bucketLock;
        this.removablePayloads = removablePayloads;
        this.removeDelay = removeDelay;
        this.referenceCounter = referenceCounter;
        this.taskMaxDuration = taskMaxDuration;
    }

    @Override
    public void run() {
        try {
            final List<RemovablePayload> notRemovedPayloads = new ArrayList<>();
            RemovablePayload removablePayload = removablePayloads.poll();
            final long startTime = System.currentTimeMillis();
            while (removablePayload != null) {
                if (System.currentTimeMillis() - removablePayload.getTimestamp() > removeDelay) {
                    final Lock lock = bucketLock.get(Long.toString(removablePayload.getId()));
                    lock.lock();
                    final long payloadId = removablePayload.getId();
                    try {
                        final AtomicLong referenceCount = referenceCounter.get(payloadId);
                        if (referenceCount == null) {
                            //The reference count can be null, if it was marked as removable twice.
                            //Which is possible if a payload marked as removable and we receive the same payload again and mark it as removable again,
                            //before the cleanup is able to remove the payload.
                            removablePayload = removablePayloads.poll();
                            continue;
                        }
                        if (referenceCount.get() == 0) {
                            payloadCache.invalidate(payloadId);
                            localPersistence.remove(payloadId);
                            referenceCounter.remove(payloadId);
                        }

                    } finally {
                        lock.unlock();
                    }
                } else {
                    notRemovedPayloads.add(removablePayload);
                }
                if (System.currentTimeMillis() > startTime + taskMaxDuration) {
                    break;
                }
                removablePayload = removablePayloads.poll();
            }
            removablePayloads.addAll(notRemovedPayloads);
        } catch (final Throwable t) {
            Exceptions.rethrowError("Exception during payload cleanup. ", t);
        }
    }
}
