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
package com.hivemq.extensions.events.client.parameters;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Wrapper class for all ClientLifecycleEventListeners added by extensions.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientEventListeners {

    private final @NotNull Map<String, ClientLifecycleEventListener> pluginEventListenersMap;
    private final @NotNull ReadWriteLock readWriteLock;

    public ClientEventListeners(final @NotNull HiveMQExtensions hiveMQExtensions) {
        this.pluginEventListenersMap = new TreeMap<>(new ExtensionPriorityComparator(hiveMQExtensions));
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    public void put(final @NotNull String pluginId, final @NotNull ClientLifecycleEventListener eventListener) {
        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            pluginEventListenersMap.put(pluginId, eventListener);
        } finally {
            lock.unlock();
        }
    }

    public void removeForPlugin(final @NotNull IsolatedExtensionClassloader pluginClassLoader) {
        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            final List<String> keysToRemove = new ArrayList<>();
            for (final Map.Entry<String, ClientLifecycleEventListener> entry : pluginEventListenersMap.entrySet()) {
                if (entry.getValue().getClass().getClassLoader().equals(pluginClassLoader)) {
                    keysToRemove.add(entry.getKey());
                }
            }
            for (final String key : keysToRemove) {
                pluginEventListenersMap.remove(key);
            }
        } finally {
            lock.unlock();
        }

    }

    public @NotNull Map<String, ClientLifecycleEventListener> getPluginEventListenersMap() {
        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            return ImmutableMap.copyOf(pluginEventListenersMap);
        } finally {
            lock.unlock();
        }
    }
}
