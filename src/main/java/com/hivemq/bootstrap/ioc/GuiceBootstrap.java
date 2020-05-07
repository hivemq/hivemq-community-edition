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

package com.hivemq.bootstrap.ioc;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingletonModule;
import com.hivemq.bootstrap.netty.ioc.NettyModule;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.ioc.ConfigurationModule;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.diagnostic.DiagnosticModule;
import com.hivemq.extensions.ioc.ExtensionModule;
import com.hivemq.lifecycle.LifecycleModule;
import com.hivemq.metrics.ioc.MetricsModule;
import com.hivemq.mqtt.ioc.MQTTHandlerModule;
import com.hivemq.mqtt.ioc.MQTTServiceModule;
import com.hivemq.persistence.ioc.FilePersistenceModule;
import com.hivemq.persistence.ioc.PersistenceModule;
import com.hivemq.security.ioc.SecurityModule;
import com.hivemq.statistics.UsageStatisticsModule;
import com.hivemq.throttling.ioc.ThrottlingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstraps Guice. Before the Guice Bootstrap happens, no
 * Dependency Injection is available.
 *
 * @author Dominik Obermaier
 */
public class GuiceBootstrap {

    private static final Logger log = LoggerFactory.getLogger(GuiceBootstrap.class);

    @Nullable
    public static Injector bootstrapInjector(final @NotNull SystemInformation systemInformation,
                                             final @NotNull MetricRegistry metricRegistry,
                                             final @NotNull HivemqId hiveMQId,
                                             final @NotNull FullConfigurationService fullConfigurationService,
                                             final @NotNull Injector persistenceInjector) {

        if (!Boolean.parseBoolean(System.getProperty("diagnosticMode"))) {
            log.trace("Turning Guice stack traces off");
            //System.setProperty("guice_include_stack_traces", "OFF");
        }

        final ImmutableList.Builder<AbstractModule> modules = ImmutableList.builder();

        return getInjector(systemInformation,
                metricRegistry,
                hiveMQId,
                fullConfigurationService,
                modules,
                persistenceInjector);
    }

    @Nullable
    @VisibleForTesting
    private static Injector getInjector(final @NotNull SystemInformation systemInformation,
                                        final @NotNull MetricRegistry metricRegistry,
                                        final @NotNull HivemqId hiveMQId,
                                        final @NotNull FullConfigurationService fullConfigurationService,
                                        final @NotNull ImmutableList.Builder<AbstractModule> modules,
                                        final @NotNull Injector persistenceInjector) {

        modules.add(new SystemInformationModule(systemInformation),
                /* For supporting lazy singletons */
                new LazySingletonModule(),
                /* Adds lifecycle methods like @PostConstruct */
                LifecycleModule.get(),
                /* Binds the configuration service */
                new ConfigurationModule(fullConfigurationService, hiveMQId),
                /* Binds netty specific classes */
                new NettyModule(), new HiveMQMainModule(),
                /* Binds MQTT handler specific classes */
                new MQTTHandlerModule(persistenceInjector),
                /* Binds the persistence */
                new PersistenceModule(persistenceInjector),
                /* Binds statistics */
                new MetricsModule(metricRegistry, persistenceInjector),
                /* Binds throttling specific classes */
                new ThrottlingModule(),
                /* Binds Services for publish distribution */
                new MQTTServiceModule(),
                /* Binds Diagnostics */
                new DiagnosticModule(),
                /* Binds SSL functionality*/
                new SecurityModule(),
                /* Bind Statistics specific classes */
                new UsageStatisticsModule(),
                /* Binds the Extension System */
                new ExtensionModule());

        try {
            return Guice.createInjector(Stage.PRODUCTION, modules.build());
        } catch (final Exception e) {
            log.error("Initializing Guice aborted", e);
            if (log.isDebugEnabled()) {
                log.debug("Original Exception: ", e);
            }
            return null;
        }
    }

    public static @NotNull Injector persistenceInjector(final @NotNull SystemInformation systemInformation,
                                                        final @NotNull MetricRegistry metricRegistry,
                                                        final @NotNull HivemqId hiveMQId,
                                                        final @NotNull FullConfigurationService configService) {

        final ImmutableList.Builder<AbstractModule> modules = ImmutableList.builder();

        modules.add(new SystemInformationModule(systemInformation), new ConfigurationModule(configService, hiveMQId),
                new LazySingletonModule(), LifecycleModule.get(), new FilePersistenceModule(metricRegistry));

        return Guice.createInjector(Stage.PRODUCTION, modules.build());
    }
}
