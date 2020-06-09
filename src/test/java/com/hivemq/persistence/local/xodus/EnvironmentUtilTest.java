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
package com.hivemq.persistence.local.xodus;

import com.hivemq.configuration.service.InternalConfigurations;
import jetbrains.exodus.env.EnvironmentConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 */
public class EnvironmentUtilTest {

    private EnvironmentUtil environmentUtil;

    @Before
    public void setUp() throws Exception {
        environmentUtil = new EnvironmentUtil();
    }

    @Test(expected = NullPointerException.class)
    public void test_create_environment_config_name_null() throws Exception {
        environmentUtil.createEnvironmentConfig(null);
    }

    @Test
    public void test_create_environment_config() throws Exception {

        final EnvironmentConfig envConfig = environmentUtil.createEnvironmentConfig("name");

        assertEquals(false, envConfig.isManagementEnabled());
        assertEquals(false, envConfig.getGcRenameFiles());
        assertEquals(60000, envConfig.getGcFilesDeletionDelay());
        assertEquals(30000, envConfig.getGcRunPeriod());
        assertEquals(1, envConfig.getGcFilesInterval());
        assertEquals(2, envConfig.getGcFileMinAge());
        assertEquals(1000, envConfig.getLogSyncPeriod());
        assertEquals(false, envConfig.getLogDurableWrite());
        assertEquals(25, envConfig.getMemoryUsagePercentage());
        assertEquals(InternalConfigurations.XODUS_LOG_CACHE_USE_NIO, envConfig.getLogCacheUseNio());
    }

}