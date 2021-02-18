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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.topic.tree.TopicTreeStartup;
import com.hivemq.persistence.*;
import com.hivemq.persistence.ioc.annotation.PayloadPersistence;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.ioc.provider.local.PayloadPersistenceScheduledExecutorProvider;
import com.hivemq.persistence.ioc.provider.local.PersistenceExecutorProvider;
import com.hivemq.persistence.ioc.provider.local.PersistenceScheduledExecutorProvider;
import com.hivemq.persistence.util.AbstractFutureUtils;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.persistence.util.FutureUtilsImpl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Dominik Obermaier
 */
public class PersistenceModule extends SingletonModule<Class<PersistenceModule>> {

    private final @NotNull Injector persistenceInjector;
    private final PersistenceConfigurationService persistenceConfigurationService;

    public PersistenceModule(
            final @NotNull Injector persistenceInjector,
            final PersistenceConfigurationService persistenceConfigurationService) {
        super(PersistenceModule.class);
        this.persistenceInjector = persistenceInjector;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @Override
    protected void configure() {

        install(new LocalPersistenceModule(persistenceInjector, persistenceConfigurationService));

        if (persistenceConfigurationService.getMode() == PersistenceConfigurationService.PersistenceMode.IN_MEMORY) {
            bind(SingleWriterService.class).to(InMemorySingleWriterImpl.class);
        } else {
            bind(SingleWriterService.class).to(SingleWriterServiceImpl.class);
        }

        bind(ShutdownHooks.class).toInstance(persistenceInjector.getInstance(ShutdownHooks.class));

        bind(PersistenceShutdownHookInstaller.class).asEagerSingleton();

        bind(ExecutorService.class).annotatedWith(Persistence.class)
                .toProvider(PersistenceExecutorProvider.class)
                .in(LazySingleton.class);
        bind(ListeningExecutorService.class).annotatedWith(Persistence.class)
                .toProvider(PersistenceExecutorProvider.class)
                .in(LazySingleton.class);

        bind(ScheduledExecutorService.class).annotatedWith(Persistence.class)
                .toProvider(PersistenceScheduledExecutorProvider.class)
                .in(LazySingleton.class);
        bind(ListeningScheduledExecutorService.class).annotatedWith(Persistence.class)
                .toProvider(PersistenceScheduledExecutorProvider.class)
                .in(LazySingleton.class);

        bindIfAbsent(ListeningScheduledExecutorService.class,
                PayloadPersistenceScheduledExecutorProvider.class,
                PayloadPersistence.class);

        bind(TopicTreeStartup.class).asEagerSingleton();

        bind(CleanUpService.class).asEagerSingleton();

        bind(AbstractFutureUtils.class).to(FutureUtilsImpl.class).asEagerSingleton();
        requestStaticInjection(FutureUtils.class);
    }

    private <T> void bindIfAbsent(final Class type, final Class provider, final Class annotation) {

        final Object instance = persistenceInjector.getInstance(Key.get(type, annotation));
        if (instance != null) {
            bind(type).annotatedWith(annotation).toInstance(instance);
        } else {
            bind(type).annotatedWith(annotation).toProvider(provider).in(LazySingleton.class);
        }
    }
}
