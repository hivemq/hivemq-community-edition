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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * @author Lukas Brandl
 */
class RemoveEntryTask implements Runnable {

    private final @NotNull PublishPayloadLocalPersistence localPersistence;
    private final @NotNull BucketLock bucketLock;
    private final @NotNull Queue<RemovablePayload> removablePayloads;
    private final long removeDelay;
    private final @NotNull PayloadReferenceCounterRegistry payloadReferenceCounterRegistry;
    private final long taskMaxDuration;

    RemoveEntryTask(
            final @NotNull PublishPayloadLocalPersistence localPersistence,
            final @NotNull BucketLock bucketLock,
            final @NotNull Queue<RemovablePayload> removablePayloads,
            final long removeDelay,
            final @NotNull PayloadReferenceCounterRegistry payloadReferenceCounterRegistry,
            final long taskMaxDuration) {
        this.localPersistence = localPersistence;
        this.bucketLock = bucketLock;
        this.removablePayloads = removablePayloads;
        this.removeDelay = removeDelay;
        this.payloadReferenceCounterRegistry = payloadReferenceCounterRegistry;
        this.taskMaxDuration = taskMaxDuration;
    }

    @Override
    public void run() {
        try {
            final List<RemovablePayload> notRemovedPayloads = new ArrayList<>();
            RemovablePayload removablePayload = removablePayloads.poll();
            final long startTime = System.currentTimeMillis();
            while (removablePayload != null) {
                if (System.currentTimeMillis() - removablePayload.getTimestamp() > removeDelay &&
                        removablePayload.inProgress.compareAndSet(false, true)) {
                    final long payloadId = removablePayload.getId();
                    bucketLock.accessBucketByPayloadId(removablePayload.getId(), () -> {
                        final int referenceCount = payloadReferenceCounterRegistry.get(payloadId);
                        // The reference count can be UNKNOWN_PAYLOAD, if it was marked as removable twice.
                        // This is possible if a payload is marked as removable, and we receive the same payload again
                        // and mark it as removable again before the cleanup is able to remove the payload.
                        if (referenceCount == 0) {
                            localPersistence.remove(payloadId);
                            payloadReferenceCounterRegistry.remove(payloadId);
                        }
                    });
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
