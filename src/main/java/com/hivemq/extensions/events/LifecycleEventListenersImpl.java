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
package com.hivemq.extensions.events;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @since 4.0.0
 */
@Singleton
public class LifecycleEventListenersImpl implements LifecycleEventListeners {

    @NotNull
    private final Map<@NotNull String, @NotNull ClientLifecycleEventListenerProvider> clientLifecycleEventListenerProviderMap;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final ReadWriteLock readWriteLock;

    @Inject
    public LifecycleEventListenersImpl(@NotNull final HiveMQExtensions hiveMQExtensions) {
        this.hiveMQExtensions = hiveMQExtensions;
        readWriteLock = new ReentrantReadWriteLock();
        clientLifecycleEventListenerProviderMap = new TreeMap<>(new ExtensionPriorityComparator(hiveMQExtensions));
    }

    @Override
    public void addClientLifecycleEventListenerProvider(final @NotNull ClientLifecycleEventListenerProvider provider) {

        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {

            final ClassLoader pluginClassloader = provider.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {

                clientLifecycleEventListenerProviderMap.put(plugin.getId(), provider);

            }

        } finally {
            writeLock.unlock();
        }

    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull ClientLifecycleEventListenerProvider> getClientLifecycleEventListenerProviderMap() {
        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            return ImmutableMap.copyOf(clientLifecycleEventListenerProviderMap);
        } finally {
            lock.unlock();
        }
    }
}
