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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.statistics.entity.Statistic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT;
import static com.hivemq.configuration.service.RestrictionsConfigurationService.MAX_CONNECTIONS_DEFAULT;
import static com.hivemq.metrics.HiveMQMetrics.CONNECTIONS_OVERALL_CURRENT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class UsageStatisticsCollectorImplTest {

    @Mock
    HiveMQExtensions hiveMQExtensions;

    @Mock
    HiveMQExtension customExtension;

    @Mock
    HiveMQExtension officialExtension;

    private UsageStatisticsCollector collector;
    private MetricRegistry metricRegistry;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();

        final SystemInformationImpl systemInformation = new SystemInformationImpl();
        metricRegistry = new MetricRegistry();
        collector = new UsageStatisticsCollectorImpl(systemInformation, configurationService,
                new MetricsHolder(metricRegistry), new HivemqId(systemInformation), hiveMQExtensions);
        when(hiveMQExtensions.getEnabledHiveMQExtensions()).thenReturn(
                ImmutableMap.of("1", customExtension, "2", officialExtension));

        when(customExtension.getAuthor()).thenReturn("another company");
        when(officialExtension.getAuthor()).thenReturn("dc-square Gmbh");

    }

    @Test
    public void test_system_statistics() throws Exception {

        final String json = collector.getJson("test");
        System.out.println(json);
        final ObjectMapper objectMapper = new ObjectMapper();

        final Statistic statistic = objectMapper.reader().forType(Statistic.class).readValue(json);

        assertFalse(statistic.getCpu().isEmpty());
        assertTrue(statistic.getCpuSockets() > 0);
        assertTrue(statistic.getCpuPhysicalCores() > 0);
        assertTrue(statistic.getCpuTotalCores() > 0);
        assertFalse(statistic.getOs().isEmpty());
        assertFalse(statistic.getOsManufacturer().isEmpty());
        assertFalse(statistic.getOsVersion().isEmpty());
        assertTrue(statistic.getOsUptime() > 0);
        assertTrue(statistic.getOpenFileLimit() > 0);
        assertTrue(statistic.getDiskSize() > 0);
        assertTrue(statistic.getMemorySize() > 0);

    }

    @Test
    public void test_jvm_statistics() throws Exception {

        final String json = collector.getJson("test");
        System.out.println(json);
        final ObjectMapper objectMapper = new ObjectMapper();

        final Statistic statistic = objectMapper.reader().forType(Statistic.class).readValue(json);

        assertFalse(statistic.getJavaVendor().isEmpty());
        assertFalse(statistic.getJavaVersion().isEmpty());
        assertFalse(statistic.getJavaVersionDate().isEmpty());
        assertFalse(statistic.getJavaVirtualMachineName().isEmpty());
        assertFalse(statistic.getJavaRuntimeName().isEmpty());
        assertFalse(statistic.getSystemArchitecture().isEmpty());
    }

    @Test
    public void test_hivemq_statistics() throws Exception {
        final String json = collector.getJson("test");
        System.out.println(json);
        final ObjectMapper objectMapper = new ObjectMapper();

        final Statistic statistic = objectMapper.reader().forType(Statistic.class).readValue(json);

        assertEquals(36, statistic.getId().length());
        assertFalse(statistic.getHivemqVersion().isEmpty());
        assertTrue(statistic.getHivemqUptime() >= 0);
        assertEquals(1, statistic.getOfficialExtensions());
        assertEquals(1, statistic.getCustomExtensions());
    }

    @Test
    public void test_metric_statistics() throws Exception {

        metricRegistry.register(CONNECTIONS_OVERALL_CURRENT.name(), (Gauge<Number>) () -> 12);

        final String json = collector.getJson("test");
        System.out.println(json);
        final ObjectMapper objectMapper = new ObjectMapper();

        final Statistic statistic = objectMapper.reader().forType(Statistic.class).readValue(json);

        assertEquals(12, statistic.getConnectedClients());
    }

    @Test
    public void test_config_statistics() throws Exception {

        final String json = collector.getJson("test");
        System.out.println(json);
        final ObjectMapper objectMapper = new ObjectMapper();

        final Statistic statistic = objectMapper.reader().forType(Statistic.class).readValue(json);

        assertEquals(0, statistic.getTcpListeners());
        assertEquals(0, statistic.getTlsListeners());
        assertEquals(0, statistic.getWsListeners());
        assertEquals(0, statistic.getWssListeners());
        assertEquals(1000, statistic.getMaxQueue());
        assertEquals(65535, statistic.getMaxKeepalive());
        assertEquals(SESSION_EXPIRY_MAX, statistic.getSessionExpiry());
        assertEquals(MAX_EXPIRY_INTERVAL_DEFAULT, statistic.getMessageExpiry());
        assertEquals(MAX_CONNECTIONS_DEFAULT, statistic.getConnectionThrottling());
        assertEquals(0, statistic.getBandwithIncoming());
    }
}