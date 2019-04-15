/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.metrics.handler;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import com.hivemq.metrics.HiveMQMetrics;
import io.netty.channel.ChannelHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
@ChannelHandler.Sharable
@Singleton
public class GlobalTrafficCounter extends GlobalTrafficShapingHandler {

    public static final int PRECISION_SECONDS = 1;
    private final MetricRegistry metricRegistry;

    public GlobalTrafficCounter(final MetricRegistry metricRegistry,
                                final ScheduledExecutorService scheduledExecutorService) {
        super(scheduledExecutorService, TimeUnit.SECONDS.toMillis(PRECISION_SECONDS));

        this.metricRegistry = metricRegistry;
    }

    @PostConstruct
    public void postConstruct() {

        for (final Map.Entry<String, Gauge<Long>> entry : gauges().entrySet()) {
            metricRegistry.register(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, Gauge<Long>> gauges() {
        final Map<String, Gauge<Long>> gauges = Maps.newHashMap();

        final io.netty.handler.traffic.TrafficCounter tc = this.trafficCounter();

        gauges.put(HiveMQMetrics.BYTES_READ_TOTAL.name(), () -> tc.cumulativeReadBytes());

        gauges.put(HiveMQMetrics.BYTES_WRITE_TOTAL.name(), () -> tc.cumulativeWrittenBytes());

        return gauges;
    }

}
