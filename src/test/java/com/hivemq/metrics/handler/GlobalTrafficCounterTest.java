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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.metrics.HiveMQMetrics;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Schäbel
 */
public class GlobalTrafficCounterTest {

    private GlobalTrafficCounter globalTrafficCounter;

    private MetricRegistry metricRegistry;

    @Before
    public void before() {

        metricRegistry = new MetricRegistry();
        globalTrafficCounter = new GlobalTrafficCounter(metricRegistry, Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    public void test_postConstruct() throws Exception {

        globalTrafficCounter.postConstruct();

        assertTrue(metricRegistry.getNames().contains(HiveMQMetrics.BYTES_READ_TOTAL.name()));
        assertTrue(metricRegistry.getNames().contains(HiveMQMetrics.BYTES_WRITE_TOTAL.name()));
    }

}