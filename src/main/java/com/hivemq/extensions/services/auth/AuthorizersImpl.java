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
package com.hivemq.extensions.services.auth;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
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
public class AuthorizersImpl implements Authorizers {

    @NotNull
    private final Map<@NotNull String, @NotNull AuthorizerProvider> authorizerProviderMap;

    @NotNull
    private final ReadWriteLock readWriteLock;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @Inject
    public AuthorizersImpl(@NotNull final HiveMQExtensions hiveMQExtensions) {
        this.hiveMQExtensions = hiveMQExtensions;
        authorizerProviderMap = new TreeMap<>(new ExtensionPriorityComparator(hiveMQExtensions));
        readWriteLock = new ReentrantReadWriteLock();
    }

    @Override
    public void addAuthorizerProvider(@NotNull final AuthorizerProvider authorizerProvider) {

        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {
            final ClassLoader pluginClassloader = authorizerProvider.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {
                authorizerProviderMap.put(plugin.getId(), authorizerProvider);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    @NotNull
    public Map<@NotNull String, @NotNull AuthorizerProvider> getAuthorizerProviderMap() {

        final Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            return ImmutableMap.copyOf(authorizerProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean areAuthorizersAvailable() {
        final Lock lock = readWriteLock.readLock();
        try {
            lock.lock();
            return !authorizerProviderMap.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}
