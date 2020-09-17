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

package com.hivemq.extensions.services.initializer;

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;

import javax.inject.Inject;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class contains every initializer set by any extension.
 * <p>
 * get, add and remove share the same {@link ReadWriteLock} which makes it ThreadSafe.
 * <p>
 * the clientInitializerMap is sorted by extension priority.
 * <p>
 * Highest priority comes first.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
@LazySingleton
@ThreadSafe
public class InitializersImpl implements Initializers {

    @NotNull
    private final Map<@NotNull String, @NotNull ClientInitializer> clientInitializerMap;

    @NotNull
    private final ReadWriteLock readWriteLock;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @Inject
    public InitializersImpl(@NotNull final HiveMQExtensions hiveMQExtensions) {
        this.hiveMQExtensions = hiveMQExtensions;
        this.clientInitializerMap = new TreeMap<>(new ExtensionPriorityComparator(hiveMQExtensions));
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    @Override
    public void addClientInitializer(@NotNull final ClientInitializer initializer) {

        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {

            final ClassLoader pluginClassloader = initializer.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {

                clientInitializerMap.put(plugin.getId(), initializer);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    @NotNull
    public Map<@NotNull String, @NotNull ClientInitializer> getClientInitializerMap() {

        final Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            return clientInitializerMap;
        } finally {
            readLock.unlock();
        }
    }
}
