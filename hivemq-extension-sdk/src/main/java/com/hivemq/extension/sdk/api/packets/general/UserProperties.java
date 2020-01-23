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
package com.hivemq.extension.sdk.api.packets.general;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The user properties of an MQTT packet.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface UserProperties {

    /**
     * @param name The name of the user property to get.
     * @return An {@link Optional} that contains the first user property with the specified name.
     * @since 4.0.0
     */
    @NotNull Optional<String> getFirst(@NotNull String name);

    /**
     * @param name The name of the user properties to get.
     * @return The values user property with the specified name.
     * @since 4.0.0
     */
    @Immutable
    @NotNull List<@NotNull String> getAllForName(@NotNull String name);

    /**
     * @return A list of all {@link UserProperty}s.
     * @since 4.0.0
     */
    @Immutable
    @NotNull List<@NotNull UserProperty> asList();

    /**
     * @return <code>true</code> if no user properties are present, else <code>false</code>.
     * @since 4.0.0
     */
    boolean isEmpty();
}
