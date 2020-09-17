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

package com.hivemq.extensions.services.interceptor;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
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
 * @author Lukas Brandl
 * @author Florian Limpöck
 */
public class InterceptorsImpl implements Interceptors {

    @NotNull
    private final Map<@NotNull String, @NotNull ConnectInboundInterceptorProvider> connectInboundInterceptorProviderMap;

    @NotNull
    private final Map<@NotNull String, @NotNull ConnackOutboundInterceptorProvider>
            connackOutboundInterceptorProviderMap;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final ReadWriteLock readWriteLock;

    @Inject
    public InterceptorsImpl(@NotNull final HiveMQExtensions hiveMQExtensions) {
        this.hiveMQExtensions = hiveMQExtensions;
        final ExtensionPriorityComparator extensionPriorityComparator = new ExtensionPriorityComparator(hiveMQExtensions);
        this.connectInboundInterceptorProviderMap = new TreeMap<>(extensionPriorityComparator);
        this.connackOutboundInterceptorProviderMap = new TreeMap<>(extensionPriorityComparator);
        this.readWriteLock = new ReentrantReadWriteLock();
        hiveMQExtensions.addAfterExtensionStopCallback(hiveMQExtension -> {
            final ClassLoader pluginClassloader = hiveMQExtension.getExtensionClassloader();
            if (pluginClassloader != null) {
                removeInterceptors(hiveMQExtension.getId());
            }
        });

    }

    @Override
    public void addConnectInboundInterceptorProvider(@NotNull final ConnectInboundInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {

            final ClassLoader pluginClassloader = provider.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {
                connectInboundInterceptorProviderMap.put(plugin.getId(), provider);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    @NotNull
    public ImmutableMap<String, ConnectInboundInterceptorProvider> connectInboundInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();

        readLock.lock();
        try {
            return ImmutableMap.copyOf(connectInboundInterceptorProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void addConnackOutboundInterceptorProvider(final @NotNull ConnackOutboundInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {

            final ClassLoader pluginClassloader = provider.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {
                connackOutboundInterceptorProviderMap.put(plugin.getId(), provider);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public @NotNull ImmutableMap<String, ConnackOutboundInterceptorProvider> connackOutboundInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();

        readLock.lock();
        try {
            return ImmutableMap.copyOf(connackOutboundInterceptorProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    private void removeInterceptors(@NotNull final String pluginId) {

        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();
        try {
            connectInboundInterceptorProviderMap.remove(pluginId);
            connackOutboundInterceptorProviderMap.remove(pluginId);
        } finally {
            writeLock.unlock();
        }
    }
}
