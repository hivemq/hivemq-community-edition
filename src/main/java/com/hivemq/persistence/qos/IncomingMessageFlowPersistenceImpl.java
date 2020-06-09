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
package com.hivemq.persistence.qos;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.persistence.local.IncomingMessageFlowLocalPersistence;

import javax.inject.Inject;

/**
 * @author Dominik Obermaier
 */
@LazySingleton
public class IncomingMessageFlowPersistenceImpl implements IncomingMessageFlowPersistence {


    private final @NotNull IncomingMessageFlowLocalPersistence localPersistence;

    @Inject
    IncomingMessageFlowPersistenceImpl(final @NotNull IncomingMessageFlowLocalPersistence localPersistence) {
        this.localPersistence = localPersistence;
    }

    @Override
    public MessageWithID get(final @NotNull String client, final int messageId) {
        return localPersistence.get(client, messageId);
    }

    @Override
    public void addOrReplace(final @NotNull String client, final int messageId, final @NotNull MessageWithID message) {
        localPersistence.addOrReplace(client, messageId, message);
    }

    @Override
    public void remove(final @NotNull String client, final int messageId) {
        localPersistence.remove(client, messageId);
    }

    @Override
    public void delete(final @NotNull String client) {
        localPersistence.delete(client);
    }

    @Override
    public void closeDB() {
        localPersistence.closeDB();
    }
}
