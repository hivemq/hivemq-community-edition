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
package com.hivemq.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A convenience class to specify constant names and types of the internal HiveMQ metrics
 *
 * @author Christoph Schäbel
 */
public class HiveMQMetric<T extends Metric> {

    private final String name;
    private final Class<? extends Metric> clazz;


    private HiveMQMetric(final String name, final Class<? extends Metric> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public static <T extends Metric> HiveMQMetric<T> valueOf(final String name, final Class<T> metricClass) {
        checkNotNull(name, "Name cannot be null");

        return new HiveMQMetric<>(name, metricClass);
    }

    public static HiveMQMetric<Gauge<Number>> gaugeValue(final String name) {
        checkNotNull(name, "Name cannot be null");

        return new HiveMQMetric<>(name, Gauge.class);
    }

    public String name() {
        return name;
    }

    public Class<? extends Metric> getClazz() {
        return clazz;
    }
}