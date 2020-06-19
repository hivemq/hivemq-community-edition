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
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.ioc.annotation.PayloadPersistence;
import com.hivemq.persistence.ioc.provider.local.ClientSessionLocalProvider;
import com.hivemq.persistence.ioc.provider.local.ClientSessionSubscriptionLocalProvider;
import com.hivemq.persistence.ioc.provider.local.PayloadPersistenceScheduledExecutorProvider;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionMemoryLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionSubscriptionMemoryLocalPersistence;
import com.hivemq.persistence.local.memory.ClientQueueMemoryLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionSubscriptionMemoryLocalPersistence;
import com.hivemq.persistence.local.memory.RetainedMessageMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistenceImpl;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;

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
            bind(ClientSessionLocalPersistence.class).toProvider(ClientSessionLocalProvider.class).in(Singleton.class);
            bind(ClientSessionSubscriptionLocalPersistence.class).toProvider(ClientSessionSubscriptionLocalProvider.class)
                    .in(Singleton.class);
            bind(ClientQueueLocalPersistence.class).to(ClientQueueXodusLocalPersistence.class).in(Singleton.class);
        } else {
            bind(PublishPayloadLocalPersistence.class).to(PublishPayloadMemoryLocalPersistence.class)
                    .in(Singleton.class);
            bind(RetainedMessageLocalPersistence.class).to(RetainedMessageMemoryLocalPersistence.class)
                    .in(Singleton.class);
            bind(ClientSessionLocalPersistence.class).to(ClientSessionMemoryLocalPersistence.class)
                    .in(Singleton.class);
            bind(ClientSessionSubscriptionLocalPersistence.class).to(ClientSessionSubscriptionMemoryLocalPersistence.class)
                    .in(Singleton.class);
            bind(ClientQueueLocalPersistence.class).to(ClientQueueMemoryLocalPersistence.class)
                    .in(Singleton.class);
        }

        bind(PublishPayloadPersistence.class).to(PublishPayloadPersistenceImpl.class).in(Singleton.class);

        bind(MetricRegistry.class).toInstance(metricRegistry);
        bind(MetricsHolder.class).toProvider(MetricsHolderProvider.class).asEagerSingleton();

        bind(ListeningScheduledExecutorService.class).annotatedWith(PayloadPersistence.class)
                .toProvider(PayloadPersistenceScheduledExecutorProvider.class)
                .in(LazySingleton.class);

        bind(MessageDroppedService.class).toProvider(MessageDroppedServiceProvider.class).in(Singleton.class);

    }
}
