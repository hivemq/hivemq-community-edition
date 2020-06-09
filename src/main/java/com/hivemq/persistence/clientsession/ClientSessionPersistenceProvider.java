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
package com.hivemq.persistence.clientsession;

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Christoph Schäbel
 */
@LazySingleton
public class ClientSessionPersistenceProvider implements Provider<ClientSessionPersistence> {

    private final Provider<ClientSessionPersistenceImpl> provider;

    @Inject
    public ClientSessionPersistenceProvider(final Provider<ClientSessionPersistenceImpl> provider) {
        this.provider = provider;
    }

    @Override
    public ClientSessionPersistence get() {
        return provider.get();
    }
}
