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
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @author Lukas Brandl
 */
@Singleton
public class JmxReporterBootstrap {

    private static final Logger log = LoggerFactory.getLogger(JmxReporterBootstrap.class);

    private final MetricRegistry metricRegistry;

    @VisibleForTesting
    JmxReporter jmxReporter;

    @Inject
    public JmxReporterBootstrap(final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @PostConstruct
    public void postConstruct() {
        if (!InternalConfigurations.JMX_REPORTER_ENABLED.get()) {
            return;
        }
        /*
         * The default object name factory in Dropwizard Metrics for JMX reporter was updated and changes the format.
         * We need to support the old format as our customers are using them for their integrations.
         * <p>
         * - Old name format, that we support:  "metrics:name=kafka-extension.total.success.count"
         * - New name format:                   "metrics:name=kafka-extension.total.success.count,type=counters"
         * <p>
         * The behavior was changed in this commit: https://github.com/dropwizard/metrics/pull/1310/files
         * The code below was copied also from this commit.
         */
        jmxReporter = JmxReporter.forRegistry(metricRegistry).createsObjectNamesWith((type, domain, name) -> {
            try {
                ObjectName objectName = new ObjectName(domain, "name", name);
                if (objectName.isPattern()) {
                    objectName = new ObjectName(domain, "name", ObjectName.quote(name));
                }
                return objectName;
            } catch (final MalformedObjectNameException e) {
                try {
                    return new ObjectName(domain, "name", ObjectName.quote(name));
                } catch (final MalformedObjectNameException e1) {
                    log.warn("Unable to register {} {}", type, name, e1);
                    throw new RuntimeException(e1);
                }
            }
        }).build();
        jmxReporter.start();
        log.debug("Started JMX Metrics Reporting.");
    }

    public void stop() {
        if (jmxReporter != null) {
            jmxReporter.stop();
        }
    }
}
