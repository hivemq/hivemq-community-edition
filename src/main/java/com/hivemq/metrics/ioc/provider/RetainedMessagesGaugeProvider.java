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
package com.hivemq.metrics.ioc.provider;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.metrics.gauges.RetainedMessagesGauge;
import com.hivemq.persistence.retained.RetainedMessagePersistence;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class RetainedMessagesGaugeProvider implements Provider<RetainedMessagesGauge> {

    private final RetainedMessagePersistence retainedMessagePersistence;
    private final MetricRegistry metricRegistry;

    @Inject
    public RetainedMessagesGaugeProvider(
            final RetainedMessagePersistence retainedMessagePersistence, final MetricRegistry metricRegistry) {
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.metricRegistry = metricRegistry;
    }


    @Override
    @Singleton
    public RetainedMessagesGauge get() {
        final RetainedMessagesGauge gauge = new RetainedMessagesGauge(retainedMessagePersistence);
        metricRegistry.register(HiveMQMetrics.RETAINED_MESSAGES_CURRENT.name(), gauge);
        return gauge;
    }
}
