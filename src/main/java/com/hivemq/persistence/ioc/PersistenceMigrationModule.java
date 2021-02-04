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
package com.hivemq.persistence.ioc;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.ioc.provider.MetricsHolderProvider;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.dropping.MessageDroppedServiceProvider;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.PersistenceStartupShutdownHookInstaller;
import com.hivemq.persistence.ioc.annotation.PayloadPersistence;
import com.hivemq.persistence.ioc.provider.local.PayloadPersistenceScheduledExecutorProvider;
import com.hivemq.persistence.payload.PublishPayloadNoopPersistenceImpl;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistenceImpl;

import javax.inject.Singleton;

/**
 * @author Florian Limpöck
 */
public class PersistenceMigrationModule extends SingletonModule<Class<PersistenceMigrationModule>> {

    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull PersistenceConfigurationService persistenceConfigurationService;

    public PersistenceMigrationModule(
            @NotNull final MetricRegistry metricRegistry,
            @NotNull final PersistenceConfigurationService persistenceConfigurationService) {
        super(PersistenceMigrationModule.class);
        this.metricRegistry = metricRegistry;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @Override
    protected void configure() {

        bind(ShutdownHooks.class).asEagerSingleton();
        bind(PersistenceStartup.class).asEagerSingleton();
        bind(PersistenceStartupShutdownHookInstaller.class).asEagerSingleton();

        if (persistenceConfigurationService.getMode() == PersistenceConfigurationService.PersistenceMode.FILE) {
            install(new PersistenceMigrationFileModule());
        } else {
            install(new LocalPersistenceMemoryModule(null));
        }

        if (persistenceConfigurationService.getMode() == PersistenceConfigurationService.PersistenceMode.IN_MEMORY) {
            bind(PublishPayloadPersistence.class).to(PublishPayloadNoopPersistenceImpl.class);
        } else {
            bind(PublishPayloadPersistence.class).to(PublishPayloadPersistenceImpl.class);
        }

        bind(MetricRegistry.class).toInstance(metricRegistry);
        bind(MetricsHolder.class).toProvider(MetricsHolderProvider.class).asEagerSingleton();

        bind(ListeningScheduledExecutorService.class).annotatedWith(PayloadPersistence.class)
                .toProvider(PayloadPersistenceScheduledExecutorProvider.class)
                .in(LazySingleton.class);

        bind(MessageDroppedService.class).toProvider(MessageDroppedServiceProvider.class).in(Singleton.class);

    }
}
