package com.hivemq.metrics.jmx;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;

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
}