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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.ioc.annotation.PayloadPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.persistence.payload.PayloadReferenceCounterRegistryImpl.REF_COUNT_ALREADY_ZERO;
import static com.hivemq.persistence.payload.PayloadReferenceCounterRegistryImpl.UNKNOWN_PAYLOAD;

@LazySingleton
public class PublishPayloadPersistenceImpl implements PublishPayloadPersistence {

    private static final @NotNull Logger log = LoggerFactory.getLogger(PublishPayloadPersistenceImpl.class);

    private final @NotNull PublishPayloadLocalPersistence localPersistence;
    private final @NotNull ListeningScheduledExecutorService scheduledExecutorService;
    private final @NotNull BucketLock bucketLock;
    private final @NotNull PayloadReferenceCounterRegistry payloadReferenceCounterRegistry;
    private final @NotNull RemovablePayloads[] removablePayloads;

    @Inject
    PublishPayloadPersistenceImpl(
            final @NotNull PublishPayloadLocalPersistence localPersistence,
            final @NotNull @PayloadPersistence ListeningScheduledExecutorService scheduledExecutorService) {

        this.localPersistence = localPersistence;
        this.scheduledExecutorService = scheduledExecutorService;

        final int bucketCount = InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.get();
        bucketLock = new BucketLock(bucketCount);
        payloadReferenceCounterRegistry = new PayloadReferenceCounterRegistryImpl(bucketLock);

        removablePayloads = new RemovablePayloads[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            removablePayloads[i] = new RemovablePayloads(i, new LinkedList<>());
        }
    }

    // The payload persistence has to be initialized after the other persistence bootstraps are finished.
    @Override
    public void init() {
        final int cleanupThreadCount = InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_THREADS.get();
        final long removeSchedule = InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE_MSEC.get();

        final RemovablePayloads[][] bucketResponsibilities =
                partitionBucketResponsibilities(removablePayloads, cleanupThreadCount);

        for (int i = 0; i < cleanupThreadCount; i++) {

            final RemovablePayloads[] responsibleBuckets = bucketResponsibilities[i];

            if (responsibleBuckets.length > 0 && !scheduledExecutorService.isShutdown()) {
                scheduledExecutorService.scheduleWithFixedDelay(new RemoveEntryTask(bucketLock,
                        payloadReferenceCounterRegistry,
                        localPersistence,
                        responsibleBuckets), removeSchedule, removeSchedule, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void add(final byte @NotNull [] payload, final long id) {
        checkNotNull(payload, "Payload must not be null");
        bucketLock.accessBucketByPayloadId(id, (bucketIndex) -> {
            if (payloadReferenceCounterRegistry.getAndIncrement(id) == UNKNOWN_PAYLOAD) {
                localPersistence.put(id, payload);
            }
        });
    }

    @Override
    public byte @NotNull [] get(final long id) {
        final byte[] payload = getPayloadOrNull(id);
        if (payload == null) {
            throw new PayloadPersistenceException(id);
        }
        return payload;
    }

    @Override
    public byte @Nullable [] getPayloadOrNull(final long id) {
        return localPersistence.get(id);
    }

    @Override
    public void incrementReferenceCounterOnBootstrap(final long id) {
        bucketLock.accessBucketByPayloadId(id, (bucketIndex) -> payloadReferenceCounterRegistry.getAndIncrement(id));
    }

    @Override
    public void decrementReferenceCounter(final long id) {
        bucketLock.accessBucketByPayloadId(id, (bucketIndex) -> {
            final int result = payloadReferenceCounterRegistry.decrementAndGet(id);
            if (result == UNKNOWN_PAYLOAD || result == REF_COUNT_ALREADY_ZERO) {
                log.warn("Tried to decrement a payload reference counter ({}) that was already zero.", id);
                if (InternalConfigurations.LOG_REFERENCE_COUNTING_STACKTRACE_AS_WARNING) {
                    if (log.isWarnEnabled()) {
                        for (int i = 0; i < Thread.currentThread().getStackTrace().length; i++) {
                            log.warn(Thread.currentThread().getStackTrace()[i].toString());
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        for (int i = 0; i < Thread.currentThread().getStackTrace().length; i++) {
                            log.debug(Thread.currentThread().getStackTrace()[i].toString());
                        }
                    }
                }
            } else if (result == 0) {
                //Note: We'll remove the entry async in the cleanup job.
                removablePayloads[bucketIndex].getQueue().add(id);
            }
        });
    }

    @Override
    public void closeDB() {
        localPersistence.closeDB();
    }

    @VisibleForTesting
    public @NotNull ImmutableMap<Long, Integer> getReferenceCountersAsMap() {
        return ImmutableMap.copyOf(payloadReferenceCounterRegistry.getAll());
    }

    @VisibleForTesting
    static @NotNull RemovablePayloads @NotNull [] @NotNull [] partitionBucketResponsibilities(
            final @NotNull RemovablePayloads[] removablePayloads, final int threads) {

        final RemovablePayloads[][] responsibilities = new RemovablePayloads[threads][];

        final int buckets = removablePayloads.length;
        final int bucketsPerThread = buckets / threads;
        // There can be remaining buckets depending on the number of CPU cores available.
        final int remainingBuckets = buckets % threads;

        for (int i = 0; i < threads; i++) {

            if (i < remainingBuckets) {
                responsibilities[i] = new RemovablePayloads[bucketsPerThread + 1];
                // add one of the remaining buckets to the last index
                responsibilities[i][bucketsPerThread] = removablePayloads[(buckets - 1 /* last index */) - i];
            } else {
                responsibilities[i] = new RemovablePayloads[bucketsPerThread];
            }

            final int startIndex = i * bucketsPerThread;

            System.arraycopy(removablePayloads, startIndex, responsibilities[i], 0, bucketsPerThread);
        }
        return responsibilities;
    }
}
