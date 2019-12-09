/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.services.auth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.common.annotations.GuardedBy;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.PluginPriorityComparator;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.handler.PluginAuthenticatorService;
import com.hivemq.persistence.ChannelPersistence;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Georg Held
 */
@Singleton
@VisibleForTesting
public class AuthenticatorsImpl implements Authenticators {

    @NotNull
    private final ReadWriteLock authenticatorsLock = new ReentrantReadWriteLock();

    @NotNull
    @GuardedBy("authenticatorsLock")
    private final TreeMap<String, WrappedAuthenticatorProvider> authenticatorPluginMap;
    @NotNull
    private final PluginAuthenticatorService pluginAuthenticatorService;
    private final ChannelPersistence channelPersistence;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    private final @NotNull AtomicInteger enhancedAuthenticatorCount;
    private final @NotNull AtomicInteger simpleAuthenticatorCount;

    @Inject
    public AuthenticatorsImpl(final @NotNull HiveMQExtensions hiveMQExtensions,
                              final @NotNull PluginAuthenticatorService pluginAuthenticatorService,
                              final @NotNull ChannelPersistence channelPersistence) {
        this.hiveMQExtensions = hiveMQExtensions;
        this.authenticatorPluginMap = new TreeMap<>(new PluginPriorityComparator(hiveMQExtensions));
        this.pluginAuthenticatorService = pluginAuthenticatorService;
        this.channelPersistence = channelPersistence;
        this.enhancedAuthenticatorCount = new AtomicInteger();
        this.simpleAuthenticatorCount = new AtomicInteger();
    }

    @Override
    @NotNull
    public Map<@NotNull String, @NotNull WrappedAuthenticatorProvider> getAuthenticatorProviderMap() {

        final Lock readLock = authenticatorsLock.readLock();
        readLock.lock();
        try {
            return ImmutableMap.copyOf(authenticatorPluginMap);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void registerAuthenticatorProvider(@NotNull final WrappedAuthenticatorProvider provider) {

        final Lock writeLock = authenticatorsLock.writeLock();

        writeLock.lock();

        try {

            final IsolatedPluginClassloader pluginClassloader = provider.getClassLoader();
            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {
                if(provider.isEnhanced()){
                    enhancedAuthenticatorCount.incrementAndGet();
                } else {
                    simpleAuthenticatorCount.incrementAndGet();
                }
                authenticatorPluginMap.put(plugin.getId(), provider);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean areAuthenticatorsAvailable() {
        final Lock lock = authenticatorsLock.readLock();
        try {
            lock.lock();
            return !authenticatorPluginMap.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEnhancedAvailable() {
        final Lock lock = authenticatorsLock.readLock();
        try {
            lock.lock();
            return enhancedAuthenticatorCount.get() > 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getEnhancedAuthenticatorCount(){
        return enhancedAuthenticatorCount.get();
    }

    @Override
    public int getSimpleAuthenticatorCount(){
        return simpleAuthenticatorCount.get();
    }
}
