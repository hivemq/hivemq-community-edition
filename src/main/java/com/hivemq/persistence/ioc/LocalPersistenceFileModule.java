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
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.clientqueue.ClientQueueXodusLocalPersistence;
import com.hivemq.persistence.ioc.provider.local.ClientSessionLocalProvider;
import com.hivemq.persistence.ioc.provider.local.ClientSessionSubscriptionLocalProvider;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.clientsession.ClientSessionSubscriptionXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.clientsession.ClientSessionXodusLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadXodusLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;

import javax.inject.Singleton;

/**
 * @author Dominik Obermaier
 */
class LocalPersistenceFileModule extends SingletonModule<Class<LocalPersistenceFileModule>> {

    private final @NotNull Injector persistenceInjector;
    private final @NotNull PersistenceType payloadPersistenceType;
    private final @NotNull PersistenceType retainedPersistenceType;

    public LocalPersistenceFileModule(@NotNull final Injector persistenceInjector) {
        super(LocalPersistenceFileModule.class);
        this.persistenceInjector = persistenceInjector;
        this.payloadPersistenceType = InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.get();
        this.retainedPersistenceType = InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get();
    }

    @Override
    protected void configure() {

        /* Local */
        if (payloadPersistenceType == PersistenceType.FILE) {
            bindLocalPersistence(PublishPayloadLocalPersistence.class,
                    PublishPayloadXodusLocalPersistence.class,
                    null);
        }
        if (retainedPersistenceType == PersistenceType.FILE) {
            bindLocalPersistence(RetainedMessageLocalPersistence.class,
                    RetainedMessageXodusLocalPersistence.class,
                    null);
        }

        if (payloadPersistenceType == PersistenceType.FILE_NATIVE ||
                retainedPersistenceType == PersistenceType.FILE_NATIVE) {
            install(new LocalPersistenceRocksDBModule(persistenceInjector));
        }

        bindLocalPersistence(ClientSessionLocalPersistence.class,
                ClientSessionXodusLocalPersistence.class,
                ClientSessionLocalProvider.class);

        bindLocalPersistence(ClientSessionSubscriptionLocalPersistence.class,
                ClientSessionSubscriptionXodusLocalPersistence.class,
                ClientSessionSubscriptionLocalProvider.class);

        bindLocalPersistence(ClientQueueLocalPersistence.class,
                ClientQueueXodusLocalPersistence.class,
                null);
    }

    private void bindLocalPersistence(
            final @NotNull Class localPersistenceClass,
            final @NotNull Class localPersistenceImplClass,
            final @Nullable Class localPersistenceProviderClass) {

        final Object instance = persistenceInjector.getInstance(localPersistenceImplClass);
        if (instance != null) {
            bind(localPersistenceImplClass).toInstance(instance);
            bind(localPersistenceClass).toInstance(instance);
        } else {
            if (localPersistenceProviderClass != null) {
                bind(localPersistenceClass).toProvider(localPersistenceProviderClass).in(Singleton.class);
            } else {
                bind(localPersistenceClass).to(localPersistenceImplClass).in(Singleton.class);
            }
        }
    }
}
