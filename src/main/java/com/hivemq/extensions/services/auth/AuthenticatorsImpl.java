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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Georg Held
 * @author Silvio Giebl
 */
@Singleton
@VisibleForTesting
public class AuthenticatorsImpl implements Authenticators {

    private static final Logger log = LoggerFactory.getLogger(AuthenticatorsImpl.class);

    private final @NotNull ReadWriteLock authenticatorsLock = new ReentrantReadWriteLock();
    @GuardedBy("authenticatorsLock")
    private final @NotNull TreeMap<String, WrappedAuthenticatorProvider> authenticatorPluginMap;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull PluginAuthenticatorService pluginAuthenticatorService;
    private final @NotNull ChannelPersistence channelPersistence;

    @Inject
    public AuthenticatorsImpl(
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull PluginAuthenticatorService pluginAuthenticatorService,
            final @NotNull ChannelPersistence channelPersistence) {

        this.authenticatorPluginMap = new TreeMap<>(new PluginPriorityComparator(hiveMQExtensions));
        this.hiveMQExtensions = hiveMQExtensions;
        this.pluginAuthenticatorService = pluginAuthenticatorService;
        this.channelPersistence = channelPersistence;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull WrappedAuthenticatorProvider> getAuthenticatorProviderMap() {

        final Lock readLock = authenticatorsLock.readLock();
        readLock.lock();
        try {
            return ImmutableMap.copyOf(authenticatorPluginMap);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void registerAuthenticatorProvider(final @NotNull WrappedAuthenticatorProvider provider) {

        final Lock writeLock = authenticatorsLock.writeLock();
        writeLock.lock();
        try {
            final IsolatedPluginClassloader extensionClassLoader = provider.getClassLoader();
            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(extensionClassLoader);

            if (extension != null) {
                authenticatorPluginMap.put(extension.getId(), provider);
                if (provider.isEnhanced()) {
                    log.debug("Enhanced authenticator added by extension '{}'.", extension.getId());
                } else {
                    log.debug("Simple authenticator added by extension '{}'.", extension.getId());
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
}
