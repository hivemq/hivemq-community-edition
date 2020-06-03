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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Georg Held
 */
@ThreadSafe
public class PublishPayloadMemoryLocalPersistence implements PublishPayloadLocalPersistence {

    private final @NotNull AtomicLong currentSize = new AtomicLong();
    private final @NotNull ConcurrentHashMap<Long, @NotNull byte[]> payloads = new ConcurrentHashMap<>();

    @Override
    public void init() {
        // noop
    }

    @Override
    public void put(final long id, final @NotNull byte[] payload) {
        currentSize.addAndGet(payload.length);
        payloads.put(id, payload);
    }

    @Override
    public @Nullable byte[] get(final long id) {
        return payloads.get(id);
    }

    @Override
    public void remove(final long id) {
        final byte[] payload = payloads.remove(id);
        if (payload != null) {
            currentSize.addAndGet(-payload.length);
        }
    }

    @Override
    public long getMaxId() {
        //always 0, as we do not have state after a restart
        return 0;
    }

    @Override
    public @NotNull ImmutableList<@NotNull Long> getAllIds() {
        return ImmutableList.copyOf(payloads.keySet());
    }

    @Override
    public void closeDB() {
        // noop
    }

    @Override
    public void iterate(final @NotNull Callback callback) {
        payloads.forEach(callback::call);
    }
}
