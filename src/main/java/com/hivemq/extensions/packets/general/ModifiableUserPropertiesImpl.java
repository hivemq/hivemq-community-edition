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
package com.hivemq.extensions.packets.general;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Georg Held
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
@ThreadSafe
public class ModifiableUserPropertiesImpl implements ModifiableUserProperties {

    private final @NotNull ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private @NotNull List<MqttUserProperty> list;

    private final boolean validateUTF8;
    private boolean modified = false;

    public ModifiableUserPropertiesImpl(
            final @NotNull ImmutableList<MqttUserProperty> list, final boolean validateUTF8) {

        this.list = list;
        this.validateUTF8 = validateUTF8;
    }

    @Override
    public @NotNull Optional<String> getFirst(final @NotNull String name) {
        checkNotNull(name, "Name must never be null");

        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            return list.stream()
                    .filter(userProperty -> userProperty.getName().equals(name))
                    .findFirst()
                    .map(UserProperty::getValue);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull List<String> getAllForName(final @NotNull String name) {
        checkNotNull(name, "Name must never be null");

        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            return list.stream()
                    .filter(userProperty -> userProperty.getName().equals(name))
                    .map(UserProperty::getValue)
                    .collect(ImmutableList.toImmutableList());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull ImmutableList<UserProperty> asList() {
        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            return ImmutableList.copyOf(list);
        } finally {
            lock.unlock();
        }
    }

    public @NotNull ImmutableList<MqttUserProperty> asInternalList() {
        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            return ImmutableList.copyOf(list);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            return list.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addUserProperty(final @NotNull UserProperty userProperty) {
        checkNotNull(userProperty, "User property must never be null");
        if (!(userProperty instanceof MqttUserProperty)) {
            throw new DoNotImplementException(UserProperty.class.getSimpleName());
        }

        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            modify().add(((MqttUserProperty) userProperty));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addUserProperty(final @NotNull String name, final @NotNull String value) {
        PluginBuilderUtil.checkUserProperty(name, value, validateUTF8);

        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            modify().add(new MqttUserProperty(name, value));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeUserProperty(final @NotNull String name, final @NotNull String value) {
        PluginBuilderUtil.checkUserProperty(name, value, validateUTF8);

        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            modify().remove(new MqttUserProperty(name, value));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull ImmutableList<UserProperty> removeName(final @NotNull String name) {
        PluginBuilderUtil.checkUserPropertyName(name, validateUTF8);

        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            final ImmutableList.Builder<UserProperty> removed = ImmutableList.builder();
            modify().removeIf(userProperty -> {
                if (userProperty.getName().equals(name)) {
                    removed.add(userProperty);
                    return true;
                }
                return false;
            });
            return removed.build();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            modified = true;
            list = new LinkedList<>();
        } finally {
            lock.unlock();
        }
    }

    public @NotNull UserPropertiesImpl copy() {
        return UserPropertiesImpl.of(asInternalList());
    }

    private @NotNull LinkedList<MqttUserProperty> modify() {
        if (list instanceof LinkedList) {
            return (LinkedList<MqttUserProperty>) list;
        }
        modified = true;
        final LinkedList<MqttUserProperty> modifiableList = new LinkedList<>(list);
        list = modifiableList;
        return modifiableList;
    }

    public boolean isModified() {
        return modified;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModifiableUserPropertiesImpl)) {
            return false;
        }
        final ModifiableUserPropertiesImpl that = (ModifiableUserPropertiesImpl) o;
        return list.equals(that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list);
    }
}
