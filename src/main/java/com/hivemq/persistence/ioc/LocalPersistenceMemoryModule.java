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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.memory.ClientQueueMemoryLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionMemoryLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionSubscriptionMemoryLocalPersistence;
import com.hivemq.persistence.local.memory.RetainedMessageMemoryLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;

import javax.inject.Singleton;

/**
 * @author Florian Limpöck
 */
class LocalPersistenceMemoryModule extends SingletonModule<Class<LocalPersistenceMemoryModule>> {

    private final @Nullable Injector injector;

    public LocalPersistenceMemoryModule(@Nullable final Injector injector) {
        super(LocalPersistenceMemoryModule.class);
        this.injector = injector;
    }

    @Override
    protected void configure() {


        bindLocalPersistence(RetainedMessageLocalPersistence.class,
                RetainedMessageMemoryLocalPersistence.class);

        bindLocalPersistence(ClientSessionSubscriptionLocalPersistence.class,
                ClientSessionSubscriptionMemoryLocalPersistence.class);

        bindLocalPersistence(ClientSessionLocalPersistence.class,
                ClientSessionMemoryLocalPersistence.class);

        bindLocalPersistence(ClientQueueLocalPersistence.class,
                ClientQueueMemoryLocalPersistence.class);

    }

    private void bindLocalPersistence(final @NotNull Class localPersistenceClass,
                                      final @NotNull Class localPersistenceImplClass) {

        final Object instance = injector == null ? null : injector.getInstance(localPersistenceImplClass);
        if (instance != null) {
            bind(localPersistenceImplClass).toInstance(instance);
            bind(localPersistenceClass).toInstance(instance);
        } else {
            bind(localPersistenceClass).to(localPersistenceImplClass).in(Singleton.class);
        }
    }
}
