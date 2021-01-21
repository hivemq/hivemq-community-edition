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
package com.hivemq.metrics.ioc;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.MetricsShutdownHook;
import com.hivemq.metrics.gauges.OpenConnectionsGauge;
import com.hivemq.metrics.gauges.RetainedMessagesGauge;
import com.hivemq.metrics.gauges.SessionsGauge;
import com.hivemq.metrics.ioc.provider.OpenConnectionsGaugeProvider;
import com.hivemq.metrics.ioc.provider.RetainedMessagesGaugeProvider;
import com.hivemq.metrics.ioc.provider.SessionsGaugeProvider;
import com.hivemq.metrics.jmx.JmxReporterBootstrap;

/**
 * The guice module which is responsible for all statistics
 *
 * @author Dominik Obermaier
 */
public class MetricsModule extends SingletonModule<Class<MetricsModule>> {

    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull Injector persistenceInjector;

    public MetricsModule(final @NotNull MetricRegistry metricRegistry, final @NotNull Injector persistenceInjector) {
        super(MetricsModule.class);
        this.metricRegistry = metricRegistry;
        this.persistenceInjector = persistenceInjector;
    }

    @Override
    protected void configure() {

        bind(MetricRegistry.class).toInstance(metricRegistry);

        //These providers are needed to force real eager initialization and instant registration of the metrics
        //because the metrics need to be available when OnBrokerStart callbacks get called.
        bind(MetricsHolder.class).toInstance(persistenceInjector.getInstance(MetricsHolder.class));
        bind(SessionsGauge.class).toProvider(SessionsGaugeProvider.class).asEagerSingleton();
        bind(OpenConnectionsGauge.class).toProvider(OpenConnectionsGaugeProvider.class).asEagerSingleton();
        bind(RetainedMessagesGauge.class).toProvider(RetainedMessagesGaugeProvider.class).asEagerSingleton();
        bind(JmxReporterBootstrap.class).asEagerSingleton();
        bind(MetricsShutdownHook.class).asEagerSingleton();
    }

}
