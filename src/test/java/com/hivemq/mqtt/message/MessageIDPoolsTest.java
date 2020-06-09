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
package com.hivemq.mqtt.message;


import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Lukas Brandl
 */
public class MessageIDPoolsTest {

    MessageIDPools messageIDPools;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalConfigurations.MESSAGE_ID_PRODUCER_LOCK_SIZE.set(8);
        messageIDPools = new MessageIDPools();
    }

    @Test
    public void test_add_and_remove_producer() throws Exception {
        messageIDPools.forClient("client");
        assertEquals(1, messageIDPools.size());
        messageIDPools.forClient("client");
        assertEquals(1, messageIDPools.size());
        messageIDPools.remove("client");
        assertEquals(0, messageIDPools.size());
    }

    @Test
    public void test_for_client_or_null() throws Exception {
        assertNull(messageIDPools.forClientOrNull("client"));
        final MessageIDPool messageIDPool = messageIDPools.forClient("client");
        assertEquals(messageIDPool, messageIDPools.forClientOrNull("client"));
    }
}