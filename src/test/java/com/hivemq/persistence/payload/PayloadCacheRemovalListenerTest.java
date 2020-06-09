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
import com.google.common.cache.CacheBuilder;
import net.openhft.hashing.LongHashFunction;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;

/**
 * @author Lukas Brandl
 */
public class PayloadCacheRemovalListenerTest {

    private final LongHashFunction hashFunction = LongHashFunction.xx();
    private final ConcurrentHashMap<Long, Long> lookupTable = new ConcurrentHashMap<>();
    private Cache<Long, byte[]> payloadCache;

    @Before
    public void setUp() throws Exception {
        payloadCache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .maximumSize(1)
                .removalListener(new PayloadCacheRemovalListener(hashFunction, lookupTable))
                .build();
    }

    @Test
    public void test_remove_from_lookup_on_remove() throws Exception {
        final byte[] payload = "payload".getBytes();
        final long payloadHash = hashFunction.hashBytes(payload);
        lookupTable.put(payloadHash, 1L);
        payloadCache.put(1L, payload);
        payloadCache.invalidate(1L);
        assertEquals(0, lookupTable.size());
    }

    @Test
    public void test_remove_from_lookup_on_over_flow() throws Exception {
        final byte[] payload1 = "payload1".getBytes();
        final byte[] payload2 = "payload2".getBytes();
        final long payloadHash1 = hashFunction.hashBytes(payload1);
        final long payloadHash2 = hashFunction.hashBytes(payload2);
        lookupTable.put(payloadHash1, 1L);
        lookupTable.put(payloadHash2, 2L);
        payloadCache.put(1L, payload1);
        payloadCache.put(2L, payload2);
        assertEquals(1, lookupTable.size());
    }
}