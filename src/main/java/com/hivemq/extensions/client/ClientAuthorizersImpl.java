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
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientAuthorizersImpl implements ClientAuthorizers {

    private final @NotNull ReadWriteLock authorizerLock;

    @GuardedBy("authorizerLock")
    private final @NotNull Map<String, SubscriptionAuthorizer> subscriptionAuthorizerMap;
    private final @NotNull Map<String, PublishAuthorizer> publishAuthorizerMap;

    public ClientAuthorizersImpl(final @NotNull ExtensionPriorityComparator extensionPriorityComparator) {
        this.subscriptionAuthorizerMap = new TreeMap<>(extensionPriorityComparator);
        this.publishAuthorizerMap = new TreeMap<>(extensionPriorityComparator);
        this.authorizerLock = new ReentrantReadWriteLock();
    }

    @Override
    public void put(@NotNull final String pluginId, @NotNull final Authorizer authorizer) {
        final Lock lock = authorizerLock.writeLock();
        lock.lock();
        try {
            if (authorizer instanceof SubscriptionAuthorizer) {
                subscriptionAuthorizerMap.put(pluginId, (SubscriptionAuthorizer) authorizer);
            }
            if (authorizer instanceof PublishAuthorizer) {
                publishAuthorizerMap.put(pluginId, (PublishAuthorizer) authorizer);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeAllForPlugin(final @NotNull IsolatedExtensionClassloader pluginClassLoader) {
        final Lock lock = authorizerLock.writeLock();
        lock.lock();
        try {
            final List<String> keysToRemove = new ArrayList<>();
            for (final Map.Entry<String, SubscriptionAuthorizer> entry : subscriptionAuthorizerMap.entrySet()) {
                if (entry.getValue().getClass().getClassLoader().equals(pluginClassLoader)) {
                    keysToRemove.add(entry.getKey());
                }
            }
            for (final String key : keysToRemove) {
                subscriptionAuthorizerMap.remove(key);
            }

            keysToRemove.clear();
            for (final Map.Entry<String, PublishAuthorizer> entry : publishAuthorizerMap.entrySet()) {
                if (entry.getValue().getClass().getClassLoader().equals(pluginClassLoader)) {
                    keysToRemove.add(entry.getKey());
                }
            }
            for (final String key : keysToRemove) {
                publishAuthorizerMap.remove(key);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull Map<String, SubscriptionAuthorizer> getSubscriptionAuthorizersMap() {
        final Lock lock = authorizerLock.readLock();
        lock.lock();
        try {
            return ImmutableMap.copyOf(subscriptionAuthorizerMap);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull Map<String, PublishAuthorizer> getPublishAuthorizersMap() {
        final Lock lock = authorizerLock.readLock();
        lock.lock();
        try {
            return ImmutableMap.copyOf(publishAuthorizerMap);
        } finally {
            lock.unlock();
        }
    }

}
