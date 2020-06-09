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
package com.hivemq.statistics;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.FullConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Christoph Schäbel
 */
public class UsageStatisticsTest {

    @Mock
    private UsageStatisticsCollector collector;

    @Mock
    private UsageStatisticsSender sender;

    @Mock
    private SystemInformation systemInformation;

    private UsageStatistics usageStatistics;
    private FullConfigurationService configurationService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();

        usageStatistics = new UsageStatistics(collector, systemInformation, sender, configurationService);

    }

    @Test
    public void test_usage_statistics_disabled() throws Exception {

        when(systemInformation.getHiveMQVersion()).thenReturn("4.5.6");
        configurationService.usageStatisticsConfiguration().setEnabled(false);

        usageStatistics.start();

        verify(collector, never()).getJson(anyString());

    }

    @Test
    public void test_usage_statistics_enabled_snapshot() throws Exception {

        when(systemInformation.getHiveMQVersion()).thenReturn("4.5.6-SNAPSHOT");
        configurationService.usageStatisticsConfiguration().setEnabled(true);

        usageStatistics.start();

        verify(collector, never()).getJson(anyString());
    }

    @Test
    public void test_usage_statistics_enabled_dev_snapshot() throws Exception {

        when(systemInformation.getHiveMQVersion()).thenReturn("Development Snapshot");
        configurationService.usageStatisticsConfiguration().setEnabled(true);

        usageStatistics.start();

        verify(collector, never()).getJson(anyString());
    }

    @Test
    public void test_usage_statistics_enabled() throws Exception {

        when(systemInformation.getHiveMQVersion()).thenReturn("4.5.6");
        configurationService.usageStatisticsConfiguration().setEnabled(true);

        usageStatistics.start();

        //wait for scheduled jobs to run
        Thread.sleep(100);

        verify(collector, times(1)).getJson(eq("startup"));

    }

}