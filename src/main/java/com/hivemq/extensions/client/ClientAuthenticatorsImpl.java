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
package com.hivemq.extensions.client;

import com.google.common.collect.ImmutableMap;
import com.hivemq.common.annotations.GuardedBy;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Florian Limpöck
 */
public class ClientAuthenticatorsImpl implements ClientAuthenticators {

    private final @NotNull ReadWriteLock authenticatorLock;

    @GuardedBy("authenticatorLock")
    private final @NotNull Map<String, EnhancedAuthenticator> enhancedAuthenticatorMap;

    public ClientAuthenticatorsImpl(final @NotNull ExtensionPriorityComparator extensionPriorityComparator) {
        this.enhancedAuthenticatorMap = new TreeMap<>(extensionPriorityComparator);
        this.authenticatorLock = new ReentrantReadWriteLock();
    }

    @Override
    public void put(@NotNull final String pluginId, @NotNull final EnhancedAuthenticator authenticator) {
        final Lock lock = authenticatorLock.writeLock();
        lock.lock();
        try {
            enhancedAuthenticatorMap.put(pluginId, authenticator);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeForExtension(final @NotNull IsolatedExtensionClassloader pluginClassLoader) {
        final Lock lock = authenticatorLock.writeLock();
        lock.lock();
        try {
            enhancedAuthenticatorMap.entrySet().removeIf(next -> next.getValue().getClass().getClassLoader().equals(pluginClassLoader));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull Map<String, EnhancedAuthenticator> getAuthenticatorMap() {
        final Lock lock = authenticatorLock.readLock();
        lock.lock();
        try {
            return ImmutableMap.copyOf(enhancedAuthenticatorMap);
        } finally {
            lock.unlock();
        }
    }
}
