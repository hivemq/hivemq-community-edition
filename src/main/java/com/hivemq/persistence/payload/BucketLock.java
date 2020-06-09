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

import com.hivemq.persistence.local.xodus.bucket.BucketUtils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Lukas Brandl
 */
public class BucketLock {

    private final Lock[] locks;

    public BucketLock(final int bucketCount) {
        locks = new Lock[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public Lock get(final String key) {
        return locks[BucketUtils.getBucket(key, locks.length)];
    }
}
