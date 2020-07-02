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
package com.hivemq.extensions.services.executor;

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class GlobalManagedExtensionExecutorServiceTest {

    private GlobalManagedExtensionExecutorService managedPluginExecutorService;

    @Mock
    ShutdownHooks shutdownHooks;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        InternalConfigurations.MANAGED_PLUGIN_THREAD_POOL_KEEP_ALIVE_SECONDS.set(60);
        InternalConfigurations.MANAGED_PLUGIN_THREAD_POOL_SIZE.set(4);

        managedPluginExecutorService = new GlobalManagedExtensionExecutorService(shutdownHooks);

    }

    @Test
    public void test_post_construct() {
        managedPluginExecutorService.postConstruct();
        assertEquals(4, managedPluginExecutorService.getCorePoolSize());
        assertEquals(60, managedPluginExecutorService.getKeepAliveSeconds());
        assertEquals(0, managedPluginExecutorService.getCurrentPoolSize());
        assertEquals(Integer.MAX_VALUE, managedPluginExecutorService.getMaxPoolSize());
    }
}