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
package com.hivemq.throttling.ioc;

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.throttling.GlobalTrafficShaperExecutorShutdownHook;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class GlobalTrafficShapingProviderTest {

    @Mock
    ShutdownHooks shutdownHooks;

    @Mock
    RestrictionsConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        when(configurationService.incomingLimit()).thenReturn(20L);
    }

    @Test
    public void test_shutdown_hook_added() {


        final GlobalTrafficShapingProvider globalTrafficShapingProvider = new GlobalTrafficShapingProvider(shutdownHooks, configurationService);

        globalTrafficShapingProvider.get();

        verify(shutdownHooks).add(any(GlobalTrafficShaperExecutorShutdownHook.class));
    }


}