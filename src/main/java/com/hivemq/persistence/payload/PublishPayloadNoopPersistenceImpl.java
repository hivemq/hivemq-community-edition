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
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Daniel Krüger
 */
@LazySingleton
public class PublishPayloadNoopPersistenceImpl implements PublishPayloadPersistence {

    @VisibleForTesting
    static final Logger log = LoggerFactory.getLogger(PublishPayloadNoopPersistenceImpl.class);

    @Inject
    public PublishPayloadNoopPersistenceImpl() {
    }

    @Override
    public void init() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(@NotNull final byte[] payload, final long referenceCount, final long payloadId) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte @NotNull [] get(final long id) {
        throw new UnsupportedOperationException("With in-memory payloads must not be gotten.");
    }

    /**
     * {@inheritDoc}
     */
    //this method is allowed to return null
    @Override
    public byte @Nullable [] getPayloadOrNull(final long id) {
        throw new UnsupportedOperationException("With in-memory payloads must not be gotten.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementReferenceCounterOnBootstrap(final long payloadId) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementReferenceCounter(final long id) {
        //NOOP
    }


    @Override
    public void closeDB() {
        //NOOP
    }

    @NotNull
    @Override
    @VisibleForTesting
    public ImmutableMap<Long, AtomicLong> getReferenceCountersAsMap() {
        throw new UnsupportedOperationException("getAllIds iys not supported for in-memory persistence");
    }


    public static long createId() {
        return PUBLISH.PUBLISH_COUNTER.getAndIncrement();
    }

    @FunctionalInterface
    private interface BucketAccessCallback {
        void call();
    }
}
