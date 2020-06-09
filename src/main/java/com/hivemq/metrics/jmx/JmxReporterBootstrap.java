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
package com.hivemq.metrics.jmx;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.google.common.annotations.VisibleForTesting;
import com.hivemq.configuration.service.InternalConfigurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Lukas Brandl
 */
@Singleton
public class JmxReporterBootstrap {

    private static final Logger log = LoggerFactory.getLogger(JmxReporterBootstrap.class);

    private final MetricRegistry metricRegistry;

    private static final AtomicBoolean constructed = new AtomicBoolean(false);

    @VisibleForTesting
    JmxReporter jmxReporter;

    @Inject
    public JmxReporterBootstrap(final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;

    }

    @PostConstruct
    public void postConstruct() {
        if (!constructed.compareAndSet(false, true)) {
            return;
        }
        if (!InternalConfigurations.JMX_REPORTER_ENABLED) {
            return;
        }
        jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
        jmxReporter.start();
        log.debug("Started JMX Metrics Reporting.");
    }

    public void stop() {
        if (jmxReporter != null) {
            jmxReporter.stop();
        }
    }
}
