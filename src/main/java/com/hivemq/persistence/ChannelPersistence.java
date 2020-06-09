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
package com.hivemq.persistence;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.Set;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
public interface ChannelPersistence {

    /**
     * Receive a {@link Channel} from the channel persistence, for a specific client id.
     *
     * @param clientId The client identifier.
     * @return The Channel of the client or <null> if not found.
     */
    @Nullable
    Channel get(@NotNull String clientId);

    /**
     * Store a {@link Channel} in the channel persistence, for a specific client id.
     *
     * @param clientId The client identifier.
     * @param value    The Channel of the client.
     */
    void persist(@NotNull String clientId, @NotNull Channel value);

    /**
     * Remove a {@link Channel} from the channel persistence, for a specific client id.
     *
     * @param clientId The client identifier.
     * @return The Channel of the client or <null> if not found.
     */
    @Nullable
    Channel remove(@NotNull String clientId);

    /**
     * @return the amount of stored channels.
     */
    long size();

    /**
     * Receive all channels with their corresponding client identifier as a set of map entries.
     *
     * @return all channels currently stored.
     */
    @NotNull
    Set<Map.Entry<String, Channel>> entries();

}
