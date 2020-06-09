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

import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import net.openhft.hashing.LongHashFunction;

import java.util.concurrent.ConcurrentHashMap;

class PayloadCacheRemovalListener implements RemovalListener<Long, byte[]> {

    private final LongHashFunction hashFunction;
    private final ConcurrentHashMap<Long, Long> lookupTable;

    PayloadCacheRemovalListener(final LongHashFunction hashFunction, final ConcurrentHashMap<Long, Long> lookupTable) {
        this.hashFunction = hashFunction;
        this.lookupTable = lookupTable;
    }

    @Override
    public void onRemoval(final RemovalNotification<Long, byte[]> notification) {
        if (notification.getValue() != null) {
            // It is not necessary to lock here.
            // There is no issue if we read an id from the lookup table, that is already removed form the cache.
            // In this case we just handle the payload as a new entry.
            final long hash = hashFunction.hashBytes(notification.getValue());
            lookupTable.remove(hash);
        }
    }
}