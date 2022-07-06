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
package com.hivemq.extensions.services;


import com.hivemq.configuration.service.InternalConfigurations;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Lukas Brandl
 */
public class PluginServiceRateLimitServiceTest {

    private PluginServiceRateLimitService pluginServiceRateLimitService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        pluginServiceRateLimitService = new PluginServiceRateLimitService();
    }

    @Test
    public void test_no_limit() {
        assertFalse(pluginServiceRateLimitService.rateLimitExceeded());
    }

    @Test
    public void test_limit() {
        InternalConfigurations.EXTENSION_SERVICE_CALL_RATE_LIMIT_PER_SEC.set(10);

        pluginServiceRateLimitService = new PluginServiceRateLimitService();

        //use up the current second
        for (int i = 0; i < 10; i++) {
            assertFalse(pluginServiceRateLimitService.rateLimitExceeded());
        }
        //use up the 10s reserve
        for (int i = 0; i < 10; i++) {
            assertFalse(pluginServiceRateLimitService.rateLimitExceeded());
        }
        assertTrue(pluginServiceRateLimitService.rateLimitExceeded());
    }

    @Test
    public void test_limit_not_exceeded() {
        InternalConfigurations.EXTENSION_SERVICE_CALL_RATE_LIMIT_PER_SEC.set(2);

        pluginServiceRateLimitService = new PluginServiceRateLimitService();

        //use up the first second
        assertFalse(pluginServiceRateLimitService.rateLimitExceeded());

        //use up the reserve
        assertFalse(pluginServiceRateLimitService.rateLimitExceeded());
    }

}