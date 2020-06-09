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
package util;

import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import org.apache.commons.lang3.RandomStringUtils;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Georg Held
 */
public class TestBucketUtil {


    public static String getId(final int desiredBucket, final int bucketSize) {
        checkArgument(0 <= desiredBucket && desiredBucket < bucketSize);
        String id = RandomStringUtils.randomAlphanumeric(15);
        while (BucketUtils.getBucket(id, bucketSize) != desiredBucket) {
            id = RandomStringUtils.randomAlphanumeric(15);
        }
        return id;
    }
}
