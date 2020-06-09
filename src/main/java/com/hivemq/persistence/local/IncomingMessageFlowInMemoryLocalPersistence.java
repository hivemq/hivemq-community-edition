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
package com.hivemq.persistence.local;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.mqtt.message.MessageWithID;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is an in-memory on-heap implementation of the
 * incoming message flow persistence.
 * <p>
 * Implementation note: No Locking is used since this implementation assumes the following:
 * <p>
 * - The same thread is used for each client id
 *
 * @author Dominik Obermaier
 */
@LazySingleton
public class IncomingMessageFlowInMemoryLocalPersistence implements IncomingMessageFlowLocalPersistence {


    private final ConcurrentHashMap<MessageFlowKey, MessageWithID> backingMap = new ConcurrentHashMap<>();


    @Override
    public void closeDB() {
    }

    @Override
    @Nullable
    public MessageWithID get(@NotNull final String client, final int messageId) {
        return backingMap.get(new MessageFlowKey(client, messageId));
    }

    @Override
    public void addOrReplace(@NotNull final String client, final int messageId, @NotNull final MessageWithID message) {
        backingMap.put(new MessageFlowKey(client, messageId), message);
    }

    @Override
    public void remove(@NotNull final String client, final int messageId) {
        backingMap.remove(new MessageFlowKey(client, messageId));
    }

    @Override
    public void delete(@NotNull final String client) {

        /* dobermai: This is a dangerous operation since that delete is not atomic.
        It shouldn't be a problem, though, since if delete is called, no adds / removes
        are expected for the same client key at the same time due to other threads interfering.

        Since the Netty layer gives us this guarantee,
        this should work for us. In case this guarantee is removed in the future,
        then we have a problem here and we need to lock the whole map while doing this expensive operation!

        Another problem here is that the removal has a complexity of O(n), which isn't really good for large maps.
        Fortunately we don't block and don't lock, so this shouldn't be much of a problem. However, the calling
        thread will be blocked. In case we have very large maps, it may be a good idea to execute this in a
        separate executor or we do parallel iteration.*/
        final Set<MessageFlowKey> keys = backingMap.keySet();
        for (final MessageFlowKey messageFlowKey : keys) {
            if (messageFlowKey.getClientId().equals(client)) {
                keys.remove(messageFlowKey);
            }
        }
    }


    @Immutable
    static class MessageFlowKey {

        private final String clientId;

        //We only need the message ID for hashing, so we don't provide
        //any access to it
        private final int messageId;

        public MessageFlowKey(@NotNull final String clientId, final int messageId) {
            this.clientId = clientId;
            this.messageId = messageId;
        }

        public String getClientId() {
            return clientId;
        }


        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final MessageFlowKey that = (MessageFlowKey) o;

            if (messageId != that.messageId) return false;
            return clientId.equals(that.clientId);

        }

        @Override
        public int hashCode() {
            int result = clientId.hashCode();
            result = 31 * result + messageId;
            return result;
        }
    }
}
