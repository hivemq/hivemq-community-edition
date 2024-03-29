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
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadRocksDBLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;

import javax.inject.Singleton;

/**
 * @author Georg Held
 */
class LocalPersistenceRocksDBModule extends SingletonModule<Class<LocalPersistenceRocksDBModule>> {

    private final Injector persistenceInjector;
    private final PersistenceType payloadPersistenceType;
    private final PersistenceType retainedPersistenceType;

    LocalPersistenceRocksDBModule(@NotNull final Injector persistenceInjector) {
        super(LocalPersistenceRocksDBModule.class);
        this.persistenceInjector = persistenceInjector;
        this.payloadPersistenceType = InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.get();
        this.retainedPersistenceType = InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get();
    }

    @Override
    protected void configure() {
        if (retainedPersistenceType == PersistenceType.FILE_NATIVE) {
            bindLocalPersistence(RetainedMessageLocalPersistence.class,
                    RetainedMessageRocksDBLocalPersistence.class,
                    null);
        }

        if (payloadPersistenceType == PersistenceType.FILE_NATIVE) {
            bindLocalPersistence(PublishPayloadLocalPersistence.class,
                    PublishPayloadRocksDBLocalPersistence.class,
                    null);
        }
    }

    private void bindLocalPersistence(
            final @NotNull Class localPersistenceClass,
            final @NotNull Class filePersistenceClass,
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
