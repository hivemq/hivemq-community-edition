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
package com.hivemq.persistence.local.xodus.bucket;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Dominik Obermaier
 */
public class BucketUtilsTest {

    @Test
    public void test_buckets_between_0_and_bucketsize() throws Exception {
        for (int i = 0; i < 10000; i++) {
            final int bucket = BucketUtils.getBucket("" + i, 3);
            assertTrue(bucket >= 0);
            assertTrue(bucket < 3);

        }
    }

    @Test
    public void test_integer_min_value() throws Exception {

        //DESIGNING WORKHOUSES generates Integer.MIN_VALUE :)
        final int bucket = BucketUtils.getBucket("DESIGNING WORKHOUSES", 5);

        assertTrue(bucket >= 0);
    }

}