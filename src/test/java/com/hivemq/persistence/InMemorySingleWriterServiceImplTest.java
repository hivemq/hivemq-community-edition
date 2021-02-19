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
package com.hivemq.persistence;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Daniel Krüger
 */
public class InMemorySingleWriterServiceImplTest {

    private @NotNull InMemorySingleWriterImpl singleWriterServiceImpl;

    @Before
    public void setUp() throws Exception {
        InternalConfigurations.SINGLE_WRITER_THREAD_POOL_SIZE.set(4);
        InternalConfigurations.SINGLE_WRITER_CREDITS_PER_EXECUTION.set(200);
        InternalConfigurations.PERSISTENCE_SHUTDOWN_GRACE_PERIOD.set(200);
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(64);
        singleWriterServiceImpl = new InMemorySingleWriterImpl();
    }

    @After
    public void tearDown() {
        singleWriterServiceImpl.stop();
    }

    @Test
    public void test_valid_amount_of_queues() {
        assertEquals(1, singleWriterServiceImpl.validAmountOfQueues(1, 64));
        assertEquals(2, singleWriterServiceImpl.validAmountOfQueues(2, 64));
        assertEquals(4, singleWriterServiceImpl.validAmountOfQueues(4, 64));
        assertEquals(8, singleWriterServiceImpl.validAmountOfQueues(5, 64));
        assertEquals(8, singleWriterServiceImpl.validAmountOfQueues(8, 64));
        assertEquals(64, singleWriterServiceImpl.validAmountOfQueues(64, 64));
    }


}