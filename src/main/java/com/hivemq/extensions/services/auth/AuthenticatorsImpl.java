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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.hivemq.common.annotations.GuardedBy;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
@VisibleForTesting
public class AuthenticatorsImpl implements Authenticators {

    private static final Logger log = LoggerFactory.getLogger(AuthenticatorsImpl.class);

    private final @NotNull ReadWriteLock authenticatorsLock = new ReentrantReadWriteLock();
    @GuardedBy("authenticatorsLock")
    private final @NotNull TreeMap<String, WrappedAuthenticatorProvider> authenticatorPluginMap;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    @Inject
    public AuthenticatorsImpl(final @NotNull HiveMQExtensions hiveMQExtensions) {
        this.hiveMQExtensions = hiveMQExtensions;
        authenticatorPluginMap = new TreeMap<>(new ExtensionPriorityComparator(hiveMQExtensions));
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
            final ClassLoader extensionClassLoader = provider.getClassLoader();
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

    @Override
    public void checkAuthenticationSafetyAndLifeness() {

        // Only check for lifeness if safety is given
        if (InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.get()) {
            // Check lifeness
            if (getAuthenticatorProviderMap().isEmpty()) {
                log.warn("\n###############################################################################" +
                        "\n# No security extension present, MQTT clients can not connect to this broker. #" +
                        "\n###############################################################################");
            }
        }
    }
}
