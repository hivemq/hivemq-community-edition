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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.statistics.collectors.CloudPlatform;
import com.hivemq.statistics.collectors.ContainerEnvironment;
import com.hivemq.statistics.entity.Statistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.SortedMap;

import static com.hivemq.configuration.service.InternalConfigurations.SYSTEM_METRICS_ENABLED;
import static com.hivemq.metrics.HiveMQMetrics.CONNECTIONS_OVERALL_CURRENT;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class UsageStatisticsCollectorImpl implements UsageStatisticsCollector {

    private static final Logger log = LoggerFactory.getLogger(UsageStatisticsCollectorImpl.class);


    private final @NotNull SystemInformation systemInformation;
    private final @NotNull FullConfigurationService fullConfigurationService;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull HivemqId hivemqId;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    @Inject
    public UsageStatisticsCollectorImpl(final @NotNull SystemInformation systemInformation,
                                        final @NotNull FullConfigurationService fullConfigurationService,
                                        final @NotNull MetricsHolder metricsHolder,
                                        final @NotNull HivemqId hivemqId,
                                        final @NotNull HiveMQExtensions hiveMQExtensions) {

        this.systemInformation = systemInformation;
        this.fullConfigurationService = fullConfigurationService;
        this.metricsHolder = metricsHolder;
        this.hivemqId = hivemqId;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    public @NotNull String getJson(final @NotNull String statisticsType) throws Exception {

        final Statistic statistic = collectStatistics();
        statistic.setStatisticType(statisticsType);

        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writer().writeValueAsString(statistic);
    }

    private @NotNull Statistic collectStatistics() {

        final Statistic statistic = new Statistic();

        collectHiveMQInformation(statistic);

        try {
            collectSystemStatistics(statistic);
        } catch (final Throwable t) {
            log.debug("Not able to fetch system information for anonymous statistics, reason: {}", t.getMessage());
            log.trace("original exception", t);
        }
        collectConfigStatistics(statistic);
        collectMetricStatistics(statistic);
        collectJvmStatistics(statistic);
        collectEnvironmentStatistics(statistic);

        return statistic;
    }

    private void collectEnvironmentStatistics(final @NotNull Statistic statistic) {
        statistic.setCloudPlatform(new CloudPlatform().getCloudPlatform());
        statistic.setContainer(new ContainerEnvironment().getContainerEnvironment());
    }

    private void collectHiveMQInformation(final @NotNull Statistic statistic) {

        statistic.setId(hivemqId.getHivemqId());
        statistic.setHivemqVersion(systemInformation.getHiveMQVersion());
        statistic.setHivemqUptime((System.currentTimeMillis() - systemInformation.getRunningSince()) / 1000);

        int official = 0;
        int custom = 0;

        final Collection<HiveMQExtension> extensions = hiveMQExtensions.getEnabledHiveMQExtensions().values();

        for (final HiveMQExtension extension : extensions) {
            if (extension.getAuthor() != null && extension.getAuthor().contains("dc-square")) {
                official++;
            } else {
                custom++;
            }
        }

        statistic.setOfficialExtensions(official);
        statistic.setCustomExtensions(custom);
    }

    private void collectJvmStatistics(final @NotNull Statistic statistic) {

        statistic.setJavaVendor(System.getProperty("java.vm.vendor"));
        statistic.setJavaVendorVersion(System.getProperty("java.vendor.version"));
        statistic.setJavaVersion(System.getProperty("java.version"));
        statistic.setJavaVersionDate(System.getProperty("java.version.date"));
        statistic.setJavaVirtualMachineName(System.getProperty("java.vm.name"));
        statistic.setJavaRuntimeName(System.getProperty("java.runtime.name"));
        statistic.setSystemArchitecture(System.getProperty("os.arch"));
    }

    private void collectMetricStatistics(final @NotNull Statistic statistic) {

        final Number connectedClients = getGaugeValue(CONNECTIONS_OVERALL_CURRENT.name());

        statistic.setConnectedClients(connectedClients == null ? 0 : connectedClients.longValue());
    }

    private void collectConfigStatistics(final @NotNull Statistic statistic) {

        final ListenerConfigurationService listenerConfiguration = fullConfigurationService.listenerConfiguration();

        statistic.setTcpListeners(listenerConfiguration.getTcpListeners().size());
        statistic.setTlsListeners(listenerConfiguration.getTlsTcpListeners().size());
        statistic.setWsListeners(listenerConfiguration.getWebsocketListeners().size());
        statistic.setWssListeners(listenerConfiguration.getTlsWebsocketListeners().size());

        final MqttConfigurationService mqttConfigurationService = fullConfigurationService.mqttConfiguration();
        statistic.setMaxQueue(mqttConfigurationService.maxQueuedMessages());

        statistic.setMaxKeepalive(mqttConfigurationService.keepAliveMax());
        statistic.setSessionExpiry(mqttConfigurationService.maxSessionExpiryInterval());
        statistic.setMessageExpiry(mqttConfigurationService.maxMessageExpiryInterval());
        statistic.setConnectionThrottling(fullConfigurationService.restrictionsConfiguration().maxConnections());
        statistic.setBandwithIncoming(fullConfigurationService.restrictionsConfiguration().incomingLimit());
    }

    private @Nullable <T> T getGaugeValue(final String metricName) {
        try {
            final SortedMap<String, Gauge> gauges = metricsHolder.getMetricRegistry().getGauges((name, metric) -> metricName.equals(name));
            if (gauges.isEmpty()) {
                return null;
            }

            //we expect a single result here
            final Gauge gauge = gauges.values().iterator().next();

            final T value = (T) gauge.getValue();
            return value;
        } catch (final Exception e) {
            return null;
        }
    }

    private void collectSystemStatistics(final @NotNull Statistic statistic) {

        if (!SYSTEM_METRICS_ENABLED) {
            return;
        }

        final OperatingSystem operatingSystem;
        final HardwareAbstractionLayer hardware;
        try {
            final SystemInfo systemInfo = new SystemInfo();
            operatingSystem = systemInfo.getOperatingSystem();
            hardware = systemInfo.getHardware();
        } catch (final UnsupportedOperationException e) {
            log.debug("system metrics are not supported, ignoring extended system information");
            log.trace("original exception", e);
            return;
        }

        statistic.setOsManufacturer(operatingSystem.getManufacturer());
        statistic.setOs(operatingSystem.getFamily());
        final OperatingSystem.OSVersionInfo version = operatingSystem.getVersionInfo();
        statistic.setOsVersion(version.getVersion());
        statistic.setOpenFileLimit(operatingSystem.getFileSystem().getMaxFileDescriptors());

        //disk space in MB
        long totalDiskSpace = 0;
        for (final OSFileStore osFileStore : operatingSystem.getFileSystem().getFileStores()) {
            totalDiskSpace += (osFileStore.getTotalSpace() / 1024 / 1024);
        }
        statistic.setDiskSize(totalDiskSpace);

        final CentralProcessor processor = hardware.getProcessor();
        statistic.setCpu(processor.toString());
        statistic.setCpuSockets(processor.getPhysicalPackageCount());
        statistic.setCpuPhysicalCores(processor.getPhysicalProcessorCount());
        statistic.setCpuTotalCores(processor.getLogicalProcessorCount());

        statistic.setOsUptime(operatingSystem.getSystemUptime());
        statistic.setMemorySize(hardware.getMemory().getTotal() / 1024 / 1024);
    }
}
