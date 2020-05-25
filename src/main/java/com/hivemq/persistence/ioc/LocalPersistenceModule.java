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

import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.mqtt.topic.tree.TopicTreeImpl;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.ChannelPersistenceImpl;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl;
import com.hivemq.persistence.clientqueue.ClientQueueXodusLocalPersistence;
import com.hivemq.persistence.clientsession.*;
import com.hivemq.persistence.ioc.provider.local.ClientSessionLocalProvider;
import com.hivemq.persistence.ioc.provider.local.ClientSessionSubscriptionLocalProvider;
import com.hivemq.persistence.ioc.provider.local.IncomingMessageFlowPersistenceLocalProvider;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.IncomingMessageFlowLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.clientsession.ClientSessionSubscriptionXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.clientsession.ClientSessionXodusLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistenceImpl;
import com.hivemq.persistence.payload.PublishPayloadXodusLocalPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistenceImpl;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistenceProvider;

import javax.inject.Singleton;

/**
 * @author Dominik Obermaier
 */
class LocalPersistenceModule extends SingletonModule<Class<LocalPersistenceModule>> {

    private final @NotNull Injector persistenceInjector;
    private final @NotNull PersistenceType payloadPersistenceType;
    private final @NotNull PersistenceType retainedPersistenceType;

    public LocalPersistenceModule(@NotNull final Injector persistenceInjector) {
        super(LocalPersistenceModule.class);
        this.persistenceInjector = persistenceInjector;
        this.payloadPersistenceType = InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.get();
        this.retainedPersistenceType = InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get();
    }

    @Override
    protected void configure() {

        /* Local */
        if (payloadPersistenceType == PersistenceType.FILE) {
            bindLocalPersistence(
                    PublishPayloadLocalPersistence.class, PublishPayloadXodusLocalPersistence.class, null);
        }
        if (retainedPersistenceType == PersistenceType.FILE) {
            bindLocalPersistence(
                    RetainedMessageLocalPersistence.class, RetainedMessageXodusLocalPersistence.class, null);
        }

        if (payloadPersistenceType == PersistenceType.FILE_NATIVE ||
                retainedPersistenceType == PersistenceType.FILE_NATIVE) {
            install(new LocalPersistenceRocksDBModule(persistenceInjector));
        }

        bindLocalPersistence(
                ClientSessionLocalPersistence.class, ClientSessionXodusLocalPersistence.class,
                ClientSessionLocalProvider.class);
        bindLocalPersistence(
                ClientSessionSubscriptionLocalPersistence.class, ClientSessionSubscriptionXodusLocalPersistence.class,
                ClientSessionSubscriptionLocalProvider.class);
        bindLocalPersistence(ClientQueueLocalPersistence.class, ClientQueueXodusLocalPersistence.class, null);

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
        bind(PublishPayloadPersistence.class).toInstance(
                persistenceInjector.getInstance(PublishPayloadPersistence.class));
        bind(PublishPayloadPersistenceImpl.class).toInstance(
                persistenceInjector.getInstance(PublishPayloadPersistenceImpl.class));

        /* Startup */
        bind(PersistenceStartup.class).toInstance(persistenceInjector.getInstance(PersistenceStartup.class));

    }

    private void bindLocalPersistence(
            final @NotNull Class localPersistenceClass, final @NotNull Class filePersistenceClass,
            final @Nullable Class localPersistenceProviderClass) {

        final Object instance = persistenceInjector.getInstance(filePersistenceClass);
        if (instance != null) {
            bind(filePersistenceClass).toInstance(instance);
            bind(localPersistenceClass).toInstance(instance);
        } else {
            if (localPersistenceProviderClass != null) {
                bind(localPersistenceClass).toProvider(localPersistenceProviderClass).in(Singleton.class);
            }
        }
    }
}
