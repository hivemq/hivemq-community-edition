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
import com.hivemq.util.Exceptions;

import java.util.Queue;

class RemoveEntryTask implements Runnable {

    private final @NotNull PublishPayloadLocalPersistence localPersistence;
    private final @NotNull BucketLock bucketLock;
    private final @NotNull RemovablePayloads @NotNull [] responsibleBuckets;
    private final @NotNull PayloadReferenceCounterRegistry payloadReferenceCounterRegistry;

    RemoveEntryTask(
            final @NotNull BucketLock bucketLock,
            final @NotNull PayloadReferenceCounterRegistry payloadReferenceCounterRegistry,
            final @NotNull PublishPayloadLocalPersistence localPersistence,
            final @NotNull RemovablePayloads @NotNull [] responsibleBuckets) {
        this.localPersistence = localPersistence;
        this.bucketLock = bucketLock;
        this.responsibleBuckets = responsibleBuckets;
        this.payloadReferenceCounterRegistry = payloadReferenceCounterRegistry;
    }

    @Override
    public void run() {
        try {
            // Cleanup our buckets for which we are responsible.
            for (final RemovablePayloads responsibleBucket : responsibleBuckets) {

                if (responsibleBucket.getQueue().isEmpty()) {
                    continue;
                }

                bucketLock.accessBucket(responsibleBucket.getBucketIndex(),
                        (index) -> cleanupQueueCompletely(responsibleBucket));
            }
        } catch (final Throwable t) {
            Exceptions.rethrowError("Exception during payload cleanup. ", t);
        }
    }

    /**
     * Delete all payloads from the current queue as long as no payload replication is in progress.
     *
     * @param removablePayloads The queue to drain the publishes from.
     */
    private void cleanupQueueCompletely(final @NotNull RemovablePayloads removablePayloads) {

        final Queue<Long> removablePayloadQueue = removablePayloads.getQueue();

        Long payloadId;
        while ((payloadId = removablePayloadQueue.poll()) != null) {

            final int referenceCount = payloadReferenceCounterRegistry.get(payloadId);
            // The reference count can be UNKNOWN_PAYLOAD, if it was marked as removable twice.
            // This is possible if a payload is marked as removable, and we receive the same payload again
            // and mark it as removable again before the cleanup is able to remove the payload.
            if (referenceCount == 0) {
                localPersistence.remove(payloadId);
                payloadReferenceCounterRegistry.delete(payloadId);
            }
        }
    }
}
