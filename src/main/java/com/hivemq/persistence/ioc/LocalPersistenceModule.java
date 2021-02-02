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

import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.mqtt.topic.tree.TopicTreeImpl;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.ChannelPersistenceImpl;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl;
import com.hivemq.persistence.clientsession.*;
import com.hivemq.persistence.ioc.provider.local.IncomingMessageFlowPersistenceLocalProvider;
import com.hivemq.persistence.local.IncomingMessageFlowLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadNoopPersistenceImpl;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistenceImpl;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistenceImpl;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistenceProvider;

import javax.inject.Singleton;

/**
 * @author Dominik Obermaier
 */
class LocalPersistenceModule extends SingletonModule<Class<LocalPersistenceModule>> {

    private final @NotNull Injector persistenceInjector;
    private final @NotNull PersistenceConfigurationService persistenceConfigurationService;

    public LocalPersistenceModule(
            @NotNull final Injector persistenceInjector,
            @NotNull final PersistenceConfigurationService persistenceConfigurationService) {
        super(LocalPersistenceModule.class);
        this.persistenceInjector = persistenceInjector;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @Override
    protected void configure() {

        /* Local */
        if (persistenceConfigurationService.getMode() == PersistenceConfigurationService.PersistenceMode.FILE) {
            install(new LocalPersistenceFileModule(persistenceInjector));
        } else {
            install(new LocalPersistenceMemoryModule(persistenceInjector));
        }

        /* Retained Message */
        bind(RetainedMessagePersistence.class).toProvider(RetainedMessagePersistenceProvider.class)
                .in(LazySingleton.class);

        /* Channel */
        bind(ChannelPersistence.class).to(ChannelPersistenceImpl.class).in(Singleton.class);

        /* Client Session */
        bind(ClientSessionPersistence.class).toProvider(ClientSessionPersistenceProvider.class).in(LazySingleton.class);

        /* Client Session Sub */
        bind(SharedSubscriptionService.class).to(SharedSubscriptionServiceImpl.class).in(LazySingleton.class);
        bind(ClientSessionSubscriptionPersistence.class).toProvider(ClientSessionSubscriptionPersistenceProvider.class)
                .in(LazySingleton.class);

        /* Topic Tree */
        bind(LocalTopicTree.class).to(TopicTreeImpl.class).in(Singleton.class);

        /* QoS Handling */
        bind(IncomingMessageFlowPersistence.class).to(IncomingMessageFlowPersistenceImpl.class);
        bind(IncomingMessageFlowLocalPersistence.class).toProvider(IncomingMessageFlowPersistenceLocalProvider.class)
                .in(LazySingleton.class);

        /* Client Queue */
        bind(ClientQueuePersistence.class).to(ClientQueuePersistenceImpl.class).in(LazySingleton.class);

        /* Payload Persistence */
        if (persistenceConfigurationService.getMode() == PersistenceConfigurationService.PersistenceMode.IN_MEMORY) {
            bind(PublishPayloadPersistence.class).toInstance(persistenceInjector.getInstance(PublishPayloadNoopPersistenceImpl.class));
        } else {
            bind(PublishPayloadPersistence.class).toInstance(persistenceInjector.getInstance(PublishPayloadPersistence.class));
            bind(PublishPayloadPersistenceImpl.class).toInstance(persistenceInjector.getInstance(
                    PublishPayloadPersistenceImpl.class));
        }

        /* Startup */
        bind(PersistenceStartup.class).toInstance(persistenceInjector.getInstance(PersistenceStartup.class));
    }
}
