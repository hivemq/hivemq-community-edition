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

package com.hivemq.extensions.packets.general;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Georg Held
 * @author Florian Limpöck
 */
public class ModifiableUserPropertiesImpl implements InternalUserProperties, ModifiableUserProperties {

    @NotNull
    private static final Function<Map.Entry<String, Set<String>>, Stream<? extends UserProperty>>
            ENTRY_STREAM_FUNCTION = e -> {
        final Stream.Builder<UserProperty> builder = Stream.builder();
        e.getValue().forEach(l -> builder.add(new MqttUserProperty(e.getKey(), l)));
        return builder.build();
    };

    @NotNull
    @VisibleForTesting
    InternalUserProperties legacy;

    private final boolean validateUTF8;

    @NotNull
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Nullable
    private Map<String, Set<String>> current;

    private boolean modified;

    public ModifiableUserPropertiesImpl(final boolean validateUTF8) {
        this(null, validateUTF8);
    }

    public ModifiableUserPropertiesImpl(@Nullable final InternalUserProperties legacy, final boolean validateUTF8) {
        if (legacy == null) {
            this.legacy = EmptyUserPropertiesImpl.INSTANCE;
        } else {
            // It is necessary to copy here, because the legacy user properties can be modifiable
            this.legacy = new UserPropertiesImpl(Mqtt5UserProperties.of(legacy.asImmutableList()));
        }
        this.validateUTF8 = validateUTF8;
        this.modified = false;
    }

    @Override
    public void addUserProperty(@NotNull final UserProperty userProperty) {

        checkNotNull(userProperty, "User property must never be null");
        if (!(userProperty instanceof UserPropertyImpl) && !(userProperty instanceof MqttUserProperty)) {
            throw new DoNotImplementException(UserProperty.class.getSimpleName());
        }

        final Lock lock = readWriteLock.writeLock();
        lock.lock();

        try {
            this.modified = true;
            addUserProperty(userProperty.getName(), userProperty.getValue());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addUserProperty(@NotNull final String name, @NotNull final String value) {
        PluginBuilderUtil.checkUserProperty(name, value, validateUTF8);
        final Lock lock = readWriteLock.writeLock();
        lock.lock();

        try {
            this.modified = true;
            createCurrent(name);

            checkNotNull(current, "Current must not be null");
            current.get(name).add(value);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeUserProperty(@NotNull final String name, @NotNull final String value) {
        PluginBuilderUtil.checkUserProperty(name, value, validateUTF8);

        final Lock lock = readWriteLock.writeLock();
        lock.lock();

        try {

            this.modified = true;
            createCurrent(name);

            checkNotNull(current, "Current must not be null");

            final Set<String> values = current.get(name);
            values.remove(value);

        } finally {
            lock.unlock();
        }
    }

    @NotNull
    @Override
    public List<UserProperty> removeName(@NotNull final String name) {
        PluginBuilderUtil.checkUserPropertyName(name, validateUTF8);

        final Lock lock = readWriteLock.writeLock();
        lock.lock();

        try {

            this.modified = true;
            createCurrent(name);

            checkNotNull(current, "Current must not be null");

            final Set<String> remove = current.remove(name);

            return remove.stream().map(s -> new MqttUserProperty(name, s)).collect(ImmutableList.toImmutableList());

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
            current = null;
            legacy = EmptyUserPropertiesImpl.INSTANCE;
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    @Override
    public Optional<String> getFirst(@NotNull final String name) {
        checkNotNull(name, "Name must never be null");

        final Lock lock = readWriteLock.readLock();
        lock.lock();

        try {
            if (current == null) {
                if (modified) {
                    return Optional.empty();
                }
                return legacy.getFirst(name);
            }

            return Optional.ofNullable(current.get(name)).flatMap(s -> s.stream().findAny());
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    @Override
    public List<String> getAllForName(@NotNull final String name) {
        checkNotNull(name, "Name must never be null");

        final Lock lock = readWriteLock.readLock();
        lock.lock();

        try {
            if (current == null) {
                if (modified) {
                    return Collections.unmodifiableList(Collections.emptyList());
                }
                return legacy.getAllForName(name);
            }

            final Set<String> values = current.get(name);
            return values == null ? ImmutableList.of() : ImmutableList.copyOf(values);
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    @Override
    public List<UserProperty> asList() {

        final Lock lock = readWriteLock.readLock();
        lock.lock();

        try {
            if (current == null) {
                if (modified) {
                    return Collections.unmodifiableList(Collections.emptyList());
                }
                return legacy.asList();
            }
            return current.entrySet().stream().flatMap(ENTRY_STREAM_FUNCTION).collect(ImmutableList.toImmutableList());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {

        final Lock lock = readWriteLock.readLock();
        lock.lock();

        try {
            if (modified) {
                return current == null || current.isEmpty();
            }
            return legacy.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    public InternalUserProperties consolidate() {

        final Lock lock = readWriteLock.readLock();
        lock.lock();

        try {
            if (modified) {
                if (current == null) {
                    return EmptyUserPropertiesImpl.INSTANCE;
                }
                return this;
            }
            return legacy;
        } finally {
            lock.unlock();
        }

    }

    @Override
    public @NotNull ImmutableList<MqttUserProperty> asImmutableList() {
        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            return asList().stream().map(MqttUserProperty::of).collect(ImmutableList.toImmutableList());
        } finally {
            lock.unlock();
        }
    }

    private void createCurrent(@NotNull final String name) {

        if (current == null) {
            current = new HashMap<>();
            for (final UserProperty userProperty : legacy.asList()) {
                addUserProperty(userProperty);
            }
            legacy = EmptyUserPropertiesImpl.INSTANCE;
        }

        if (!current.containsKey(name)) {
            current.put(name, new HashSet<>());
        }

        modified = true;
    }

    public boolean isModified() {
        return modified;
    }
}
