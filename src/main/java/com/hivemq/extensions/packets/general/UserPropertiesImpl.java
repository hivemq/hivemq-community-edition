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
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.0.0
 */
@Immutable
public class UserPropertiesImpl implements UserProperties {

    private static final UserPropertiesImpl NO_USER_PROPERTIES = new UserPropertiesImpl(ImmutableList.of());

    public static @NotNull UserPropertiesImpl of(final @NotNull ImmutableList<MqttUserProperty> list) {
        return list.isEmpty() ? NO_USER_PROPERTIES : new UserPropertiesImpl(list);
    }

    private final @NotNull ImmutableList<MqttUserProperty> list;

    private UserPropertiesImpl(final @NotNull ImmutableList<MqttUserProperty> list) {
        this.list = list;
    }

    @Override
    public @NotNull Optional<String> getFirst(final @NotNull String name) {
        checkNotNull(name, "Name must never be null");
        return list.stream()
                .filter(userProperty -> userProperty.getName().equals(name))
                .findFirst()
                .map(UserProperty::getValue);
    }

    @Override
    public @NotNull ImmutableList<String> getAllForName(final @NotNull String name) {
        checkNotNull(name, "Name must never be null");
        return list.stream()
                .filter(userProperty -> userProperty.getName().equals(name))
                .map(UserProperty::getValue)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public @NotNull ImmutableList<UserProperty> asList() {
        return ImmutableList.copyOf(list);
    }

    public @NotNull ImmutableList<MqttUserProperty> asInternalList() {
        return list;
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserPropertiesImpl)) {
            return false;
        }
        final UserPropertiesImpl that = (UserPropertiesImpl) o;
        return list.equals(that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list);
    }
}
