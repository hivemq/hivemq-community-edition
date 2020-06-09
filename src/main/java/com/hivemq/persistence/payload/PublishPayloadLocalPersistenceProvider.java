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
package com.hivemq.persistence.payload;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.migration.meta.PersistenceType;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class PublishPayloadLocalPersistenceProvider implements Provider<PublishPayloadLocalPersistence> {

    private final @NotNull Provider<PublishPayloadRocksDBLocalPersistence> rocksDBProvider;
    private final @NotNull Provider<PublishPayloadXodusLocalPersistence> xodusProvider;
    private final @NotNull PersistenceType persistenceType;


    @Inject
    public PublishPayloadLocalPersistenceProvider(final @NotNull Provider<PublishPayloadRocksDBLocalPersistence> rocksDBProvider,
                                                   final @NotNull Provider<PublishPayloadXodusLocalPersistence> xodusProvider) {
        this.rocksDBProvider = rocksDBProvider;
        this.xodusProvider = xodusProvider;
        this.persistenceType = InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.get();
    }

    @NotNull
    @Override
    public PublishPayloadLocalPersistence get() {
        if(persistenceType == PersistenceType.FILE_NATIVE) {
            return rocksDBProvider.get();
        } else {
            return xodusProvider.get();
        }
    }
}