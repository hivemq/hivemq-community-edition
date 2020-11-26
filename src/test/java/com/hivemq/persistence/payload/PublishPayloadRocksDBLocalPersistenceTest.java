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

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 */
@SuppressWarnings("NullabilityAnnotations")
public class PublishPayloadRocksDBLocalPersistenceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    LocalPersistenceFileUtil localPersistenceFileUtil;

    private PublishPayloadRocksDBLocalPersistence persistence;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(8);
        when(localPersistenceFileUtil.getVersionedLocalPersistenceFolder(anyString(), anyString())).thenReturn(temporaryFolder.newFolder());
        InternalConfigurations.PAYLOAD_PERSISTENCE_MEMTABLE_SIZE_PORTION.set(1024);
        InternalConfigurations.PAYLOAD_PERSISTENCE_BLOCK_CACHE_SIZE_PORTION.set(16);

        persistence = new PublishPayloadRocksDBLocalPersistence(localPersistenceFileUtil, new PersistenceStartup());
        persistence.start();
    }

    @After
    public void cleanUp() {
        InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.set(64);
        persistence.closeDB();
    }

    @Test
    public void test_add_get_payload() throws Exception {

        final byte[] payload1 = "payload".getBytes();
        final byte[] payload2 = "payload".getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload2);

        final byte[] result1 = persistence.get(0L);
        final byte[] result2 = persistence.get(1L);

        assertEquals(true, Arrays.equals(result1, payload1));
        assertEquals(true, Arrays.equals(result2, payload2));
    }


    @Test
    public void test_add_remove_get_payload() throws Exception {

        final byte[] payload1 = "payload".getBytes();
        final byte[] payload2 = "payload".getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload2);

        persistence.remove(1L);

        final byte[] result1 = persistence.get(0L);
        final byte[] result2 = persistence.get(1L);

        assertEquals(true, Arrays.equals(result1, payload1));
        assertNull(result2);
    }

    @Test
    public void test_add_get_big_payload() throws Exception {

        final byte[] payload1 = "payload".getBytes();
        final byte[] payload2 = RandomStringUtils.random(10 * 1024 * 1024 + 100, true, true).getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload2);

        final byte[] result1 = persistence.get(0L);
        final byte[] result2 = persistence.get(1L);

        assertEquals(true, Arrays.equals(result1, payload1));
        assertEquals(true, Arrays.equals(result2, payload2));
    }

    @Test
    public void test_add_remove_get_big_payload() throws Exception {

        final byte[] payload1 = "payload".getBytes();
        final byte[] payload2 = RandomStringUtils.random(10 * 1024 * 1024 + 100, true, true).getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload2);

        persistence.remove(1L);

        final byte[] result1 = persistence.get(0L);
        final byte[] result2 = persistence.get(1L);

        assertEquals(true, Arrays.equals(result1, payload1));
        assertNull(result2);
    }

    @Test
    public void test_get_all_ids() throws Exception {

        final byte[] payload1 = "payload".getBytes();

        persistence.put(0L, payload1);
        persistence.put(1L, payload1);
        persistence.put(2L, payload1);

        persistence.remove(1L);

        final List<Long> allIds = persistence.getAllIds();
        assertEquals(2, allIds.size());
        assertEquals(false, allIds.contains(1L));
    }


    @Test(timeout = 10_000)
    public void put_bigPayloads_memtableFlushed() {
        final long memtableSize = persistence.getMemtableSize();
        long bytesPuttedIn = 0L;
        final byte[] payload1 = "payload".getBytes();
        while (bytesPuttedIn < memtableSize) {
            for (final long memTableSize : persistence.getRocksdbToMemTableSize()) {
                //skip the empty entries
                if (memTableSize == 0) {
                    continue;
                }
                assertEquals(bytesPuttedIn, memTableSize);
            }
            persistence.put(0L, payload1);
            bytesPuttedIn += payload1.length;
        }
        //after flush memTable must be empty (all -  because the others were empty already)
        for (final long memTableSize : persistence.getRocksdbToMemTableSize()) {
            assertEquals(0L, memTableSize);
        }
    }

}