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

import com.codahale.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricRegistryLogger implements MetricRegistryListener {

    private static final Logger log = LoggerFactory.getLogger(MetricRegistryLogger.class);

    @Override
    public void onGaugeAdded(final String name, final Gauge<?> gauge) {
        log.trace("Metrics gauge [{}] added", name);
    }

    @Override
    public void onGaugeRemoved(final String name) {
        log.trace("Metrics gauge [{}] removed", name);
    }

    @Override
    public void onCounterAdded(final String name, final Counter counter) {
        log.trace("Metrics counter [{}] added", name);
    }

    @Override
    public void onCounterRemoved(final String name) {
        log.trace("Metrics counter [{}] removed", name);
    }

    @Override
    public void onHistogramAdded(final String name, final Histogram histogram) {
        log.trace("Metrics histogram [{}] added", name);
    }

    @Override
    public void onHistogramRemoved(final String name) {
        log.trace("Metrics histogram [{}] removed", name);
    }

    @Override
    public void onMeterAdded(final String name, final Meter meter) {
        log.trace("Metrics meter [{}] added", name);
    }

    @Override
    public void onMeterRemoved(final String name) {
        log.trace("Metrics meter [{}] removed", name);
    }

    @Override
    public void onTimerAdded(final String name, final Timer timer) {
        log.trace("Metrics timer [{}] added", name);
    }

    @Override
    public void onTimerRemoved(final String name) {
        log.trace("Metrics timer [{}] removed", name);
    }
}