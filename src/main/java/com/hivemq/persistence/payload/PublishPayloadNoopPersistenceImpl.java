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
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Daniel Krüger
 */
@LazySingleton
public class PublishPayloadNoopPersistenceImpl implements PublishPayloadPersistence {

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
    public boolean add(@NotNull final byte[] payload, final long referenceCount, final long payloadId) {
        return false;
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

}
