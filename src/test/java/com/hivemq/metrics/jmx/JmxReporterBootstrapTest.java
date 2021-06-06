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
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Lukas Brandl
 */
public class JmxReporterBootstrapTest {

    @Test
    public void postConstructWhenEnabledThenReporterIsCreated() {
        final JmxReporterBootstrap jmxReporterBootstrap = new JmxReporterBootstrap(new MetricRegistry());
        jmxReporterBootstrap.postConstruct();
        assertNotNull(jmxReporterBootstrap.jmxReporter);
    }

    @Test
    public void objectNameWhenCounterMetricRequestedThenHasExpectedFormat() throws Exception {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final JmxReporterBootstrap jmxReporterBootstrap = new JmxReporterBootstrap(metricRegistry);
        jmxReporterBootstrap.postConstruct();

        metricRegistry.counter("my-counter").inc();

        final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName metricNameObject = new ObjectName("metrics:name=my-counter");
        final Object attribute = platformMBeanServer.getAttribute(metricNameObject, "Count");
        assertNotNull(attribute);

        final double metricValue = Double.parseDouble(attribute.toString());
        assertEquals(1, metricValue, 0);

        jmxReporterBootstrap.stop();
    }

    @Test
    public void objectNameWhenGaugeMetricRequestedThenHasExpectedFormat() throws Exception {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final JmxReporterBootstrap jmxReporterBootstrap = new JmxReporterBootstrap(metricRegistry);
        jmxReporterBootstrap.postConstruct();

        final AtomicInteger integer = new AtomicInteger(0);
        metricRegistry.gauge("my-gauge", () -> () -> integer.incrementAndGet());

        final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName metricNameObject = new ObjectName("metrics:name=my-gauge");
        final Object attribute = platformMBeanServer.getAttribute(metricNameObject, "Value");
        assertNotNull(attribute);

        final double metricValue = Double.parseDouble(attribute.toString());
        assertEquals(integer.get(), metricValue, 0);

        jmxReporterBootstrap.stop();
    }

    @Test
    public void objectNameWhenHistogramMetricRequestedThenHasExpectedFormat() throws Exception {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final JmxReporterBootstrap jmxReporterBootstrap = new JmxReporterBootstrap(metricRegistry);
        jmxReporterBootstrap.postConstruct();

        metricRegistry.histogram("my-histogram").update(1);

        final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName metricNameObject = new ObjectName("metrics:name=my-histogram");
        final Object attribute = platformMBeanServer.getAttribute(metricNameObject, "Mean");
        assertNotNull(attribute);

        final double metricValue = Double.parseDouble(attribute.toString());
        assertEquals(1, metricValue, 0);

        jmxReporterBootstrap.stop();
    }
}