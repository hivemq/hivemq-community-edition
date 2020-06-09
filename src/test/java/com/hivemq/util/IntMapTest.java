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
package com.hivemq.util;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Lukas Brandl
 */
public class IntMapTest {

    @Test
    public void test_put_get_remove() throws Exception {
        final IntMap intMap = new IntMap();
        intMap.put(1, 1);
        intMap.put(2, 15);
        intMap.put(3, -15);
        assertEquals(1, intMap.get(1));
        assertEquals(15, intMap.get(2));
        assertEquals(-15, intMap.get(3));
        assertEquals(0, intMap.get(4));

        intMap.put(1, 3);
        assertEquals(3, intMap.get(1));
        intMap.put(1, 0);
        assertEquals(0, intMap.get(1));

        intMap.remove(3);
        assertEquals(0, intMap.get(3));
        intMap.remove(4);
        assertEquals(0, intMap.get(4));
    }

    @Test
    public void test_merge() throws Exception {
        final IntMap intMap1 = new IntMap();
        intMap1.put(1, 1);
        intMap1.put(2, 15);

        final IntMap intMap2 = new IntMap();
        intMap2.put(1, 2);
        intMap2.put(2, -1);
        intMap2.put(3, 10);

        intMap1.mergeMax(intMap2);

        assertEquals(2, intMap1.get(1));
        assertEquals(15, intMap1.get(2));
        assertEquals(10, intMap1.get(3));
    }

    @Test
    public void test_initialize() throws Exception {
        final IntMap intMap = new IntMap(3);
        assertEquals(3, intMap.size());
        intMap.putUnsafe(3, 1, 0);
        intMap.putUnsafe(2, 2, 1);
        intMap.putUnsafe(5, 3, 2);

        assertEquals(3, intMap.size());
        assertEquals(1, intMap.get(3));
        assertEquals(2, intMap.get(2));
        assertEquals(3, intMap.get(5));
    }

    @Test
    public void test_increment() throws Exception {
        final IntMap intMap = new IntMap();
        intMap.put(1, 1);
        intMap.increment(1);
        intMap.increment(2);

        assertEquals(2, intMap.get(1));
        assertEquals(1, intMap.get(2));
    }

    @Test
    public void test_iterate() throws Exception {
        final IntMap intMap = new IntMap();
        intMap.put(1, 1);
        intMap.put(2, 15);
        intMap.put(3, -15);

        final Iterator<IntMap.IntMapEntry> iterator = intMap.iterator();
        final IntMap.IntMapEntry entry1 = iterator.next();
        assertEquals(1, entry1.getKey());
        assertEquals(1, entry1.getValue());

        final IntMap.IntMapEntry entry2 = iterator.next();
        assertEquals(2, entry2.getKey());
        assertEquals(15, entry2.getValue());

        final IntMap.IntMapEntry entry3 = iterator.next();
        assertEquals(3, entry3.getKey());
        assertEquals(-15, entry3.getValue());

        assertEquals(false, iterator.hasNext());
        assertNull(iterator.next());
    }

    @Test
    public void test_copy() throws Exception {
        final IntMap intMap = new IntMap();
        intMap.put(1, 1);
        intMap.put(2, 15);
        intMap.put(3, -15);

        final IntMap copy = new IntMap(intMap);
        assertEquals(1, copy.get(1));
        assertEquals(15, copy.get(2));
        assertEquals(-15, copy.get(3));
        assertEquals(0, copy.get(4));
    }
}