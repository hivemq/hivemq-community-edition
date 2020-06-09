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
package com.hivemq.persistence.ioc.provider.local;


import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.xodus.clientsession.ClientSessionXodusLocalPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * The provider which is responsible for creating and providing the
 * local ClientSession Store.
 *
 * @author Dominik Obermaier
 */
public class ClientSessionLocalProvider implements Provider<ClientSessionLocalPersistence> {

    private static final Logger log = LoggerFactory.getLogger(ClientSessionLocalProvider.class);

    private final Provider<ClientSessionXodusLocalPersistence> localFilePersistence;

    @Inject
    ClientSessionLocalProvider(final Provider<ClientSessionXodusLocalPersistence> localFilePersistence) {
        this.localFilePersistence = localFilePersistence;
    }

    @Override
    public ClientSessionLocalPersistence get() {
        log.trace("Using file based Client Session Persistence store");
        return localFilePersistence.get();
    }
}
