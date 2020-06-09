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
package com.hivemq.mqtt.message;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Striped;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.pool.MessageIDPool;
import com.hivemq.mqtt.message.pool.SequentialMessageIDPoolImpl;

import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Dominik Obermaier
 */
@Singleton
public class MessageIDPools {

    private final @NotNull Striped<ReadWriteLock> lock;
    private final @NotNull ConcurrentHashMap<String, MessageIDPool> producers = new ConcurrentHashMap<>();

    MessageIDPools() {
        //Do not instantiate manually
        lock = Striped.readWriteLock(InternalConfigurations.MESSAGE_ID_PRODUCER_LOCK_SIZE.get());
    }

    /**
     * Returns the {@link MessageIDPool} for a client. If there is no message id pool available for the client,
     * it will create anew message id pool for that client.
     *
     * @param client the client to return the message id pool for
     * @return a {@link MessageIDPool} implementation
     */
    @NotNull
    public MessageIDPool forClient(final @NotNull String client) {

        // A read lock is sufficient here, since we never override an entry with ConcurrentHashMap:putIfAbsent().
        final Lock readLock = this.lock.get(client).readLock();
        readLock.lock();
        try {

            MessageIDPool idProducer = producers.get(client);

            //The following code is an optimization so we don't have to create
            //a new object everytime. In worst case we create unnecessary
            //objects only if we have a race condition
            if (idProducer == null) {
                producers.putIfAbsent(client, new SequentialMessageIDPoolImpl());
                idProducer = producers.get(client);
            }

            return idProducer;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns the {@link MessageIDPool} for a client or null, if there is no message id pool available for the client.
     *
     * @param client the client to return the message id pool for
     * @return a {@link MessageIDPool} implementation
     */
    @Nullable
    public MessageIDPool forClientOrNull(final @NotNull String client) {

        // A read lock is sufficient here, since we never override an entry with ConcurrentHashMap:putIfAbsent().
        final Lock readLock = this.lock.get(client).readLock();
        readLock.lock();
        try {
            return producers.get(client);
        } finally {
            readLock.unlock();
        }
    }

    public void remove(final @NotNull String client) {
        final Lock writeLock = this.lock.get(client).writeLock();
        writeLock.lock();
        try {
            producers.remove(client);
        } finally {
            writeLock.unlock();
        }
    }

    @VisibleForTesting
    int size() {
        return producers.size();
    }
}
