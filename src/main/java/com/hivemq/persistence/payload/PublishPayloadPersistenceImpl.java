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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.ioc.annotation.PayloadPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Lukas Brandl
 */
@LazySingleton
public class PublishPayloadPersistenceImpl implements PublishPayloadPersistence {

    @VisibleForTesting
    static final Logger log = LoggerFactory.getLogger(PublishPayloadPersistenceImpl.class);


    private final @NotNull PublishPayloadLocalPersistence localPersistence;
    private final @NotNull ListeningScheduledExecutorService scheduledExecutorService;

    private final long removeSchedule;

    private final @NotNull BucketLock bucketLock;

    @NotNull Cache<Long, byte[]> payloadCache;
    final Queue<RemovablePayload> removablePayloads = new LinkedTransferQueue<>();
    private final @NotNull PayloadReferenceCounterRegistry payloadReferenceCounterRegistry;


    private @Nullable ListenableScheduledFuture<?> removeTaskFuture;
    private final int bucketLockCount;


    @Inject
    PublishPayloadPersistenceImpl(final @NotNull PublishPayloadLocalPersistence localPersistence,
                                  final @NotNull @PayloadPersistence ListeningScheduledExecutorService scheduledExecutorService) {

        this.localPersistence = localPersistence;
        this.scheduledExecutorService = scheduledExecutorService;

        payloadCache = CacheBuilder.newBuilder()
                .expireAfterAccess(InternalConfigurations.PAYLOAD_CACHE_DURATION.get(), TimeUnit.MILLISECONDS)
                .maximumSize(InternalConfigurations.PAYLOAD_CACHE_SIZE.get())
                .concurrencyLevel(InternalConfigurations.PAYLOAD_CACHE_CONCURRENCY_LEVEL.get())
                .build();
        removeSchedule =  InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE.get();
        bucketLockCount = InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.get();
        bucketLock = new BucketLock(bucketLockCount);
        payloadReferenceCounterRegistry = new PayloadReferenceCounterRegistryImpl(bucketLock);
    }

    // The payload persistence has to be initialized after the other persistence bootstraps are finished.
    @Override
    public void init() {
        final long removeDelay = InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_DELAY.get();
        final int cleanupThreadCount = InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_THREADS.get();
        final long taskSchedule = removeSchedule * cleanupThreadCount;
        for (int i = 0; i < cleanupThreadCount; i++) {
            final long initialSchedule = removeSchedule * i;
            // We schedule an amount of tasks equal to the amount of clean up threads. The rate is a configured value multiplied by the thread count.
            // Therefore all threads in the pool should be running simultaneously on high load.
            if (!scheduledExecutorService.isShutdown()) {
                removeTaskFuture = scheduledExecutorService.scheduleAtFixedRate(
                        new RemoveEntryTask(payloadCache, localPersistence, bucketLock, removablePayloads, removeDelay,
                                payloadReferenceCounterRegistry, taskSchedule), initialSchedule, taskSchedule, TimeUnit.MILLISECONDS);
            }
        }
    }


    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public boolean add(@NotNull final byte[] payload, final long referenceCount, final @NotNull long payloadId) {
        checkNotNull(payload, "Payload must not be null");
        bucketLock.accessBucketByPaloadId(payloadId, () -> {
            final Integer counter = payloadReferenceCounterRegistry.get(payloadId);
            if (payloadCache.getIfPresent(payloadId) != null && counter != null) {
                payloadReferenceCounterRegistry.add(payloadId, (int) referenceCount);
            } else {
                if (counter == null) {
                    payloadReferenceCounterRegistry.put(payloadId, (int) referenceCount);
                } else {
                    payloadReferenceCounterRegistry.add(payloadId, (int) referenceCount);
                }
                payloadCache.put(payloadId, payload);
                localPersistence.put(payloadId, payload);
            }
        });
        return true;
    }

    /**
     * {@inheritDoc}
     */
    //this method is not allowed to return null
    @Override
    public byte @NotNull [] get(final long id) {

        final byte[] payload = getPayloadOrNull(id);

        if (payload == null) {
            throw new PayloadPersistenceException(id);
        }
        return payload;
    }

    /**
     * {@inheritDoc}
     */
    //this method is allowed to return null
    @Override
    public byte @Nullable [] getPayloadOrNull(final long id) {
        final byte[] cachedPayload = payloadCache.getIfPresent(id);
        // We don't need to lock here.
        // In case of a lost update issue, we would just overwrite the cache entry with the same payload.
        if (cachedPayload != null) {
            return cachedPayload;
        }
        final byte[] payload = localPersistence.get(id);
        if (payload == null) {
            return null;
        }
        /*
            We have the guarantee that there is no other entry with the same id because the id is monotonically
            increasing due to the AtomicLong nature. In worst case we do the same put N times instead of only once,
            this doesn't do any harm.
         */
        payloadCache.put(id, payload);
        /*
            In worst case we overwrite a newer value in the lookup table which means we kill
            the optimization. No harm is done in this case since we "just" lose performance.
         */
        return payload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementReferenceCounterOnBootstrap(final long payloadId) {
        // Since this method is only called during bootstrap, it is not performance critical.
        // Therefore locking is not an issue here.
        bucketLock.accessBucketByPaloadId(payloadId, () -> {
                payloadReferenceCounterRegistry.increment(payloadId);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementReferenceCounter(final long id) {
        bucketLock.accessBucketByPaloadId(id, () -> {
            final Integer counter = payloadReferenceCounterRegistry.get(id);
            if (counter == null || counter <= 0) {
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
                return;
            }

            final long referenceCount = payloadReferenceCounterRegistry.decrement(id);

            if (referenceCount == 0) {
                removablePayloads.add(new RemovablePayload(id, System.currentTimeMillis()));
                //Note: We'll remove the reference counter entry  in the cleanup
            }
        });
    }




    @Override
    public void closeDB() {
        if (removeTaskFuture != null) {
        }
        localPersistence.closeDB();
    }

    @Override
    @VisibleForTesting
    public @NotNull ImmutableMap<Long, Integer> getReferenceCountersAsMap() {
        return ImmutableMap.copyOf(payloadReferenceCounterRegistry.getAll());
    }

    public static long createId() {
        return PUBLISH.PUBLISH_COUNTER.getAndIncrement();
    }

    public PayloadReferenceCounterRegistry getPayloadReferenceCounterRegistry() {
        return payloadReferenceCounterRegistry;
    }
}
