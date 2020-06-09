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
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertSame;

/**
 * @author Lukas Brandl
 */
public class JmxReporterBootstrapTest {

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_post_construct_twice() {
        final JmxReporterBootstrap jmxReporterBootstrap = new JmxReporterBootstrap(new MetricRegistry());
        jmxReporterBootstrap.postConstruct();
        final JmxReporter firstReporter = jmxReporterBootstrap.jmxReporter;
        jmxReporterBootstrap.postConstruct();
        final JmxReporter secondReporter = jmxReporterBootstrap.jmxReporter;
        assertSame(firstReporter, secondReporter);
    }
}