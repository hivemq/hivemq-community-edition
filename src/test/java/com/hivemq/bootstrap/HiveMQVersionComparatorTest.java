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

package com.hivemq.bootstrap;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Christoph Schäbel
 */
public class HiveMQVersionComparatorTest {

    private HiveMQVersionComparator comparator;

    @Before
    public void before() {
        comparator = new HiveMQVersionComparator();
    }

    @Test
    public void test_compare_equals() throws Exception {
        assertEquals(0, comparator.compare("1.2.3", "1.2.3"));
        assertEquals(0, comparator.compare("1.2.0", "1.2.0"));
        assertEquals(0, comparator.compare("121.0.0", "121.0.0"));
    }

    @Test
    public void test_compare_less_than() throws Exception {
        assertEquals(-1, comparator.compare("1.2.2", "1.2.3"));
        assertEquals(-1, comparator.compare("1.2.0", "1.3.0"));
        assertEquals(-1, comparator.compare("1.0.0", "2.0.0"));
    }

    @Test
    public void test_compare_greater_than() throws Exception {
        assertEquals(1, comparator.compare("1.2.4", "1.2.3"));
        assertEquals(1, comparator.compare("1.3.0", "1.2.0"));
        assertEquals(1, comparator.compare("21.0.0", "1.2.3"));
    }

}