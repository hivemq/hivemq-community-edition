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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author Christoph Schäbel
 */
public class UsageStatistics {

    private static final Logger log = LoggerFactory.getLogger(UsageStatistics.class);

    private final @NotNull UsageStatisticsCollector statisticsCollector;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull UsageStatisticsSender statisticsSender;
    private final @NotNull ScheduledExecutorService scheduledExecutorService;
    private final @NotNull FullConfigurationService configurationService;

    @Inject
    public UsageStatistics(final @NotNull UsageStatisticsCollector statisticsCollector,
                           final @NotNull SystemInformation systemInformation,
                           final @NotNull UsageStatisticsSender statisticsSender,
                           final @NotNull FullConfigurationService configurationService) {
        this.statisticsCollector = statisticsCollector;
        this.systemInformation = systemInformation;
        this.statisticsSender = statisticsSender;

        this.configurationService = configurationService;

        final ThreadFactory threadFactory = ThreadFactoryUtil.create("usage-statistics-%d");
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    public void start() {

        if (!configurationService.usageStatisticsConfiguration().isEnabled()) {
            return;
        }

        final String hiveMQVersion = systemInformation.getHiveMQVersion();

        //Do not capture statistics for snapshots
        if (hiveMQVersion.equals("Development Snapshot") || hiveMQVersion.endsWith("SNAPSHOT")) {
            return;
        }

        //schedule first task
        scheduledExecutorService.execute(new SendStatisticsTask(statisticsSender, statisticsCollector,
                scheduledExecutorService, "startup"));

    }

    private static class SendStatisticsTask implements Runnable {

        private final @NotNull UsageStatisticsSender statisticsSender;
        private final @NotNull UsageStatisticsCollector statisticsCollector;
        private final @NotNull ScheduledExecutorService scheduledExecutorService;
        private final @NotNull String statisticType;


        private SendStatisticsTask(final @NotNull UsageStatisticsSender statisticsSender,
                                   final @NotNull UsageStatisticsCollector statisticsCollector,
                                   final @NotNull ScheduledExecutorService scheduledExecutorService,
                                   final @NotNull String statisticType) {
            this.statisticsSender = statisticsSender;
            this.statisticsCollector = statisticsCollector;
            this.scheduledExecutorService = scheduledExecutorService;
            this.statisticType = statisticType;

        }

        @Override
        public void run() {
            try {
                statisticsSender.sendStatistics(statisticsCollector.getJson(statisticType));
            } catch (final Exception e) {
                log.debug("Not able to send anonymous user statistics, reason: {}", e.getMessage());
                log.trace("original exception", e);
            } finally {
                //reschedule
                scheduledExecutorService.schedule(new SendStatisticsTask(statisticsSender, statisticsCollector,
                        scheduledExecutorService, "runtime"), InternalConfigurations.STATISTICS_SEND_EVERY_MINUTES, TimeUnit.MINUTES);
            }
        }
    }
}
