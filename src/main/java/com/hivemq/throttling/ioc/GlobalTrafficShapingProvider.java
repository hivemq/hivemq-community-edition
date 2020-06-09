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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.throttling.GlobalTrafficShaperExecutorShutdownHook;
import com.hivemq.util.ThreadFactoryUtil;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * A provider which creates the Global Traffic Shaper for HiveMQ.
 *
 * @author Florian Limpoeck
 * @author Dominik Obermaier
 */
public class GlobalTrafficShapingProvider implements Provider<GlobalTrafficShapingHandler> {

    private static final Logger log = LoggerFactory.getLogger(GlobalTrafficShapingProvider.class);

    private final @NotNull ShutdownHooks registry;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;


    @Inject
    GlobalTrafficShapingProvider(final @NotNull ShutdownHooks registry,
                                 final @NotNull RestrictionsConfigurationService restrictionsConfigurationService) {

        this.registry = registry;
        this.restrictionsConfigurationService = restrictionsConfigurationService;

    }

    @Override
    public @NotNull GlobalTrafficShapingHandler get() {

        final ThreadFactory threadFactory = ThreadFactoryUtil.create("global-traffic-shaper-executor-%d");
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);

        final GlobalTrafficShaperExecutorShutdownHook shutdownHook = new GlobalTrafficShaperExecutorShutdownHook(scheduledExecutorService);
        registry.add(shutdownHook);

        final long incomingLimit = restrictionsConfigurationService.incomingLimit();
        log.debug("Throttling incoming traffic to {} B/s", incomingLimit);

        final long outgoinglimit = InternalConfigurations.OUTGOING_BANDWIDTH_THROTTLING_DEFAULT;
        log.debug("Throttling outgoing traffic to {} B/s", outgoinglimit);

        return new GlobalTrafficShapingHandler(scheduledExecutorService, outgoinglimit, incomingLimit, 1000L);
    }
}
