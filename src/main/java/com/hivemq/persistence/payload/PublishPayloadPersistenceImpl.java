/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.persistence.ioc.annotation.PayloadPersistence;
import net.openhft.hashing.LongHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

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

    private final @NotNull LongHashFunction hashFunction;

    private final long removeSchedule;

    private final AtomicLong nextPayloadId = new AtomicLong(0);
    private final @NotNull BucketLock bucketLock;

    @NotNull Cache<Long, byte[]> payloadCache;
    final ConcurrentHashMap<Long, Long> lookupTable = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Long, AtomicLong> referenceCounter = new ConcurrentHashMap<>();
    final Queue<RemovablePayload> removablePayloads = new LinkedTransferQueue<>();
    final Set<Long> suspectedReferences = Collections.newSetFromMap(new ConcurrentHashMap<>());


    private @Nullable ListenableScheduledFuture<?> removeTaskFuture;

    @Inject
    PublishPayloadPersistenceImpl(final @NotNull PublishPayloadLocalPersistence localPersistence,
                                  final @NotNull @PayloadPersistence ListeningScheduledExecutorService scheduledExecutorService) {

        this.localPersistence = localPersistence;
        this.scheduledExecutorService = scheduledExecutorService;

        hashFunction = LongHashFunction.xx();

        payloadCache = CacheBuilder.newBuilder()
                .expireAfterAccess(InternalConfigurations.PAYLOAD_CACHE_DURATION.get(), TimeUnit.MILLISECONDS)
                .maximumSize(InternalConfigurations.PAYLOAD_CACHE_SIZE.get())
                .concurrencyLevel(InternalConfigurations.PAYLOAD_CACHE_CONCURRENCY_LEVEL.get())
                .removalListener(new PayloadCacheRemovalListener(hashFunction, lookupTable))
                .build();

        removeSchedule = InternalConfigurations.PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE.get();
        bucketLock = new BucketLock(InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.get());
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
            // Therefor all threads in the pool should be running simultaneously on high load.
            if (!scheduledExecutorService.isShutdown()) {
                removeTaskFuture = scheduledExecutorService.scheduleAtFixedRate(
                        new RemoveEntryTask(payloadCache, localPersistence, bucketLock, removablePayloads, removeDelay,
                                referenceCounter, taskSchedule), initialSchedule, taskSchedule, TimeUnit.MILLISECONDS);
            }
        }
        nextPayloadId.set(localPersistence.getMaxId() + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long add(@NotNull final byte[] payload, final long referenceCount) {
        checkNotNull(payload, "Payload must not be null");

        final long payloadHash = hashFunction.hashBytes(payload);

        final Long currentId = currentId(payloadHash, payload);
        if (currentId != null) {
            final Lock lock = bucketLock.get(Long.toString(currentId));
            lock.lock();
            try {
                // We can't lock before we have the id, therefor we have to check the cache again inside the lock.
                if (payloadCache.getIfPresent(currentId) != null) {
                    referenceCounter.get(currentId).addAndGet(referenceCount); //The counter can not be null if we lock correctly

                    // The payload is already existent in the persistence
                    return currentId;
                }
            } finally {
                lock.unlock();
            }
        }

        //The payload is not necessarily in the persistence yet
        final long payloadId = nextPayloadId.getAndIncrement();
        final Lock lock = bucketLock.get(Long.toString(payloadId));
        lock.lock();
        try {
            //we never overwrite in the map because the payloadId is guaranteed to be increasing on every call
            referenceCounter.put(payloadId, new AtomicLong(referenceCount));
            /*
            If there's already a hash in the lookup table, we can safely overwrite it
            because we are checking for byte equality before receiving the value. Overwriting
            an old value just means we don't profit from the optimization
            */
            lookupTable.put(payloadHash, payloadId);

            payloadCache.put(payloadId, payload);
            localPersistence.put(payloadId, payload);

            return payloadId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    //this method is not allowed to return null
    @Override
    public @NotNull byte[] get(final long id) {

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
    public @Nullable byte[] getPayloadOrNull(final long id) {
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
        lookupTable.put(hashFunction.hashBytes(payload), id);
        return payload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementReferenceCounterOnBootstrap(final long id) {
        // Since this method is only called during bootstrap, it is not performance critical.
        // Therefor locking is not an issue here.
        final Lock lock = bucketLock.get(Long.toString(id));
        lock.lock();
        try {
            final AtomicLong referenceCount = referenceCounter.get(id);
            if (referenceCount == null) {
                referenceCounter.put(id, new AtomicLong(1));
            } else {
                referenceCount.incrementAndGet();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementReferenceCounter(final long id) {
        final AtomicLong counter = referenceCounter.get(id);
        if (counter == null || counter.get() <= 0) {
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

        final long referenceCount = counter.decrementAndGet();

        if (referenceCount == 0) {
            removablePayloads.add(new RemovablePayload(id, System.currentTimeMillis()));
            //Note: We'll remove the AtomicLong from the reference counter in the cleanup
        }

    }

    @Override
    public void suspect(final long id) {
        suspectedReferences.add(id);
    }

    @Nullable
    private Long currentId(final long payloadHash, @NotNull final byte[] payload) {
        final Long existentId = lookupTable.get(payloadHash);

        if (existentId != null) {

            final byte[] existentPayload = payloadCache.getIfPresent(existentId);

            if (existentPayload != null) {
                final boolean equalPayload = Arrays.equals(existentPayload, payload);
                if (equalPayload) {
                    return existentId;
                }
            }

        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<Long> getAllIds() {
        return localPersistence.getAllIds();
    }

    @Override
    public void closeDB() {
        if (removeTaskFuture != null) {
            removeTaskFuture.cancel(true);
        }
        localPersistence.closeDB();
    }

    @NotNull
    @Override
    @VisibleForTesting
    public ImmutableMap<Long, AtomicLong> getReferenceCountersAsMap() {
        return ImmutableMap.copyOf(referenceCounter);
    }
}
