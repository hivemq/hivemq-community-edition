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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Lukas Brandl
 */
public class BucketTest {

    private final @NotNull Environment environment = mock();
    private final @NotNull Store store = mock();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void close() throws Exception {
        final Bucket bucket = new Bucket(environment, store);
        assertTrue(bucket.close());
        assertFalse(bucket.close());
        assertFalse(bucket.close());
    }
}
